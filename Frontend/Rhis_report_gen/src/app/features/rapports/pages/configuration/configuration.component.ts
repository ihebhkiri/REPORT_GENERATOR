import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {ButtonModule} from 'primeng/button';
import {ProgressSpinnerModule} from 'primeng/progressspinner';
import {
  EMPTY,
  Subject,
  catchError,
  distinctUntilChanged,
  finalize,
  of,
  switchMap,
  timer,
} from 'rxjs';

import {ReportPreviewService} from '../../services/report-preview.service';
import {
  ApiProblem,
  ReportFilterRequest,
  ReportPreviewRequest,
  ReportPreviewResponse,
  ReportSortRequest,
} from '../../models/report-preview.model';
import {ColumnSelectorComponent} from './components/column-selector/column-selector.component';
import {
  FilterEditorComponent,
  FilterEditorState,
} from './components/filter-editor/filter-editor.component';
import {
  PreviewPanelComponent,
  PreviewStatus,
} from './components/preview-panel/preview-panel.component';
import {SortEditorComponent} from './components/sort-editor/sort-editor.component';
import {
  DatasetFieldGroup,
  ReportConfigurationLoadResult,
  ReportField,
  SelectedDataset,
} from './configuration.models';
import {ReportConfigurationLoader} from './report-configuration-loader.service';
import { ReportGenerationService } from '../../services/report-generation.service';
import { ReportDraftStorageService } from '../../services/report-draft-storage.service';
import {ReportStepsComponent} from '../../../../shared/report-steps/report-steps.component';

interface ReportStep {
  readonly number: number;
  readonly label: string;
  readonly state: 'completed' | 'active' | 'pending';
}

interface PreviewIntent {
  readonly key: string;
  readonly request: ReportPreviewRequest | null;
  readonly immediate: boolean;
}

type MobileTab = 'configuration' | 'preview';

const PREVIEW_DEBOUNCE_MS = 300;
const MOBILE_MEDIA_QUERY = '(max-width: 47.999rem)';

/**
 * Gère la définition locale du rapport et le cycle de vie de l’aperçu pour cette page.
 * La résolution des jeux de données et le chargement des champs restent confiés à
 * `ReportConfigurationLoader`, tandis que ce composant conserve l’état de référence des éditeurs
 * enfants et orchestre l’actualisation automatique de l’aperçu.
 */
@Component({
  selector: 'app-configuration',
  standalone: true,
  imports: [
    ButtonModule,
    ColumnSelectorComponent,
    FilterEditorComponent,
    PreviewPanelComponent,
    ProgressSpinnerModule,
    RouterLink,
    SortEditorComponent,
    ReportStepsComponent,
  ],
  templateUrl: './configuration.component.html',
  styleUrl: './configuration.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfigurationComponent {
  private readonly reportConfigurationLoader = inject(ReportConfigurationLoader);
  private readonly reportPreviewService = inject(ReportPreviewService);
  private readonly reportGenerationService = inject(ReportGenerationService);
  private readonly reportDraftStorage = inject(ReportDraftStorageService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly previewIntents = new Subject<PreviewIntent>();
  private readonly mobileMediaQuery = typeof globalThis.matchMedia === 'function'
    ? globalThis.matchMedia(MOBILE_MEDIA_QUERY)
    : null;
  private readonly handleMobileMediaChange = (event: MediaQueryListEvent): void => {
    this.isMobile.set(event.matches);
    if (event.matches) {
      queueMicrotask(() => this.moveFocusOutOfHiddenMobilePanel());
    }
  };
  private previewRequestRevision = 0;

  readonly steps: readonly ReportStep[] = [
    {number: 1, label: 'Source de données', state: 'completed'},
    {number: 2, label: 'Configuration', state: 'active'},
    {number: 3, label: 'Export', state: 'pending'},
  ];

  readonly selectedDatasets = signal<readonly SelectedDataset[]>([]);
  readonly fieldGroups = signal<readonly DatasetFieldGroup[]>([]);
  readonly selectedFields = signal<readonly ReportField[]>([]);
  readonly filters = signal<readonly ReportFilterRequest[]>([]);
  readonly filtersValid = signal(true);
  readonly sorts = signal<readonly ReportSortRequest[]>([]);
  readonly filterResetRevision = signal(0);
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly previewLoading = signal(false);
  readonly previewResult = signal<ReportPreviewResponse | null>(null);
  readonly previewError = signal<string | null>(null);
  readonly previewStale = signal(false);
  readonly previewCollapsed = signal(false);
  readonly isMobile = signal(this.mobileMediaQuery?.matches ?? false);
  readonly mobileTab = signal<MobileTab>('configuration');
  readonly isGenerating = signal(false);
  readonly generationError = signal<string | null>(null);
  readonly restoredMessage = signal<string | null>(
    typeof globalThis.history?.state?.['reportMessage'] === 'string'
      ? globalThis.history.state['reportMessage']
      : null,
  );

  readonly selectedCount = computed(() => this.selectedFields().length);
  readonly filterFieldGroups = computed<readonly DatasetFieldGroup[]>(() => {
    const selectedKeys = new Set(this.selectedFields().map((field) => field.key));
    return this.fieldGroups()
      .map((group) => ({
        ...group,
        fields: group.fields.filter((field) => selectedKeys.has(field.key)),
      }))
      .filter((group) => group.fields.length > 0);
  });
  readonly canRetryPreview = computed(
    () => this.selectedDatasets().some((dataset) => dataset.main)
      && this.selectedFields().length > 0
      && this.filtersValid()
      && !this.previewLoading()
      && !this.isLoading()
      && !this.errorMessage()
      && !this.isGenerating(),
  );
  readonly canGenerate = computed(
    () => this.selectedFields().length > 0 && this.filtersValid() && !this.isGenerating(),
  );
  readonly previewStatus = computed<PreviewStatus>(() => {
    if (this.selectedFields().length === 0 || !this.filtersValid()) {
      return 'Configuration incomplète';
    }
    if (this.previewLoading()) {
      return 'Mise à jour…';
    }
    if (this.previewError()) {
      return 'Erreur';
    }
    if (this.previewStale()) {
      return 'Aperçu précédent';
    }
    return this.previewResult() ? 'À jour' : 'En attente';
  });
  readonly previewIncompleteMessage = computed(() => {
    if (this.selectedFields().length === 0) {
      return 'Sélectionnez au moins une colonne.';
    }
    return this.filtersValid() ? null : 'Terminez ou corrigez les filtres.';
  });

  constructor() {
    this.mobileMediaQuery?.addEventListener('change', this.handleMobileMediaChange);
    this.destroyRef.onDestroy(() =>
      this.mobileMediaQuery?.removeEventListener('change', this.handleMobileMediaChange),
    );
    this.observePreviewIntents();
    this.loadConfiguration();
  }

  /**
   * Charge la configuration décrite par la route. Une URL invalide ou une erreur HTTP efface
   * l’état courant, tandis qu’un résultat de résolution invalide est traité comme une erreur métier.
   */
  loadConfiguration(): void {
    const mainDatasetId = this.parseMainDatasetId();
    const relatedDatasetIds = this.parseRelatedDatasetIds();

    if (mainDatasetId === null || relatedDatasetIds === null) {
      this.resetData();
      this.errorMessage.set('La sélection des sources de données dans l’URL est invalide.');
      return;
    }

    if (this.isLoading()) {
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.schedulePreview();

    this.reportConfigurationLoader
      .load(mainDatasetId, relatedDatasetIds)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isLoading.set(false);
          this.schedulePreview();
        }),
      )
      .subscribe({
        next: (result) => this.applyConfigurationLoadResult(result),
        error: () => {
          this.resetData();
          this.errorMessage.set(
            'Impossible de charger la configuration du rapport. Réessayez dans quelques instants.',
          );
        },
      });
  }

  /**
   * Remplace la sélection, retire les filtres et tris qui ne ciblent plus une colonne sélectionnée,
   * puis programme l’actualisation correspondant à la définition courante.
   */
  updateSelectedFields(fields: readonly ReportField[]): void {
    this.selectedFields.set(fields);
    const selectedIds = new Set(fields.map((field) => field.id));
    this.filters.update((filters) =>
      filters.filter((filter) => selectedIds.has(filter.fieldId)),
    );
    this.sorts.update((sorts) => sorts.filter((sort) => selectedIds.has(sort.fieldId)));
    this.schedulePreview();
  }

  /**
   * Met toujours à jour la validité de l’éditeur, mais ne remplace les filtres exécutables que
   * lorsque le brouillon est valide afin de conserver la dernière définition utilisable.
   */
  updateFilters(state: FilterEditorState): void {
    this.filtersValid.set(state.valid);
    if (state.valid) {
      this.filters.set(state.filters);
    }
    this.schedulePreview();
  }

  updateSorts(sorts: readonly ReportSortRequest[]): void {
    this.sorts.set(sorts);
    this.schedulePreview();
  }

  retryPreview(): void {
    if (this.previewLoading()) {
      return;
    }
    this.schedulePreview(true);
  }

  togglePreview(): void {
    this.previewCollapsed.update((collapsed) => !collapsed);
  }

  selectMobileTab(tab: MobileTab): void {
    this.mobileTab.set(tab);
  }

  handleMobileTabKeydown(event: KeyboardEvent, currentTab: MobileTab): void {
    const nextTab = event.key === 'Home'
      ? 'configuration'
      : event.key === 'End'
        ? 'preview'
        : event.key === 'ArrowRight'
          ? currentTab === 'configuration' ? 'preview' : 'configuration'
          : event.key === 'ArrowLeft'
            ? currentTab === 'preview' ? 'configuration' : 'preview'
            : null;
    if (!nextTab) {
      return;
    }

    event.preventDefault();
    this.selectMobileTab(nextTab);
    const tabList = (event.currentTarget as HTMLElement).closest('[role="tablist"]');
    tabList?.querySelector<HTMLElement>(`#${nextTab}-tab`)?.focus();
  }

  continueToExport(): void {
    if (!this.canGenerate()) {
      return;
    }

    const mainDataset = this.selectedDatasets().find((dataset) => dataset.main);
    if (!mainDataset) {
      return;
    }

    const relatedDatasetIds = this.selectedDatasets()
      .filter((dataset) => !dataset.main)
      .map((dataset) => dataset.id)
      .sort((left, right) => left - right);
    const definition = this.createPreviewRequest(mainDataset);
    this.reportDraftStorage.save({ version: 1, definition, relatedDatasetIds });
    this.isGenerating.set(true);
    this.generationError.set(null);
    this.schedulePreview();

    this.reportGenerationService
      .startReportGeneration(definition, globalThis.crypto.randomUUID())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isGenerating.set(false);
          if (this.generationError()) {
            this.schedulePreview();
          }
        }),
      )
      .subscribe({
        next: (generation) => {
          void this.router.navigate(['/rapports/export', generation.generationId]);
        },
        error: (error: HttpErrorResponse) => {
          this.generationError.set(this.generationErrorMessage(error));
        },
      });
  }

  private generationErrorMessage(error: HttpErrorResponse): string {
    const problem = error.error as ApiProblem | null;
    if (problem && typeof problem === 'object' && typeof problem.detail === 'string') {
      return problem.detail;
    }
    if (error.status === 0) {
      return 'Le serveur est momentanément inaccessible. La génération n’a pas été recréée.';
    }
    return 'Impossible de démarrer la génération du rapport.';
  }

  private restoreDraft(result: Extract<ReportConfigurationLoadResult, { status: 'loaded' }>): boolean {
    const draft = this.reportDraftStorage.load();
    const mainDataset = result.selectedDatasets.find((dataset) => dataset.main);
    const relatedDatasetIds = result.selectedDatasets
      .filter((dataset) => !dataset.main)
      .map((dataset) => dataset.id)
      .sort((left, right) => left - right);
    if (!draft || !mainDataset || draft.definition.rootDatasetId !== mainDataset.id
      || JSON.stringify(draft.relatedDatasetIds) !== JSON.stringify(relatedDatasetIds)) {
      return false;
    }

    const fieldsById = new Map(
      result.fieldGroups.flatMap((group) => group.fields.map((field) => [field.id, field] as const)),
    );
    const selectedFields = draft.definition.selectedFieldIds.map((id) => fieldsById.get(id));
    const referencedIds = [
      ...draft.definition.selectedFieldIds,
      ...draft.definition.filters.map((filter) => filter.fieldId),
      ...draft.definition.sorts.map((sort) => sort.fieldId),
    ];
    if (selectedFields.some((field) => field === undefined)
      || referencedIds.some((id) => !fieldsById.has(id))) {
      return false;
    }

    this.selectedFields.set(selectedFields as readonly ReportField[]);
    this.filters.set(draft.definition.filters);
    this.filtersValid.set(true);
    this.sorts.set(draft.definition.sorts);
    this.filterResetRevision.update((revision) => revision + 1);
    this.clearPreview();
    this.schedulePreview();
    return true;
  }

  private parseMainDatasetId(): number | null {
    const rawDatasetId = this.route.snapshot.paramMap.get('datasetId');
    const datasetId = Number(rawDatasetId);

    return rawDatasetId && Number.isInteger(datasetId) && datasetId > 0 ? datasetId : null;
  }

  /**
   * Normalise le paramètre de route en identifiants uniques triés. Son absence représente une
   * sélection vide, tandis qu’une valeur non entière ou non positive invalide toute la sélection.
   */
  private parseRelatedDatasetIds(): readonly number[] | null {
    const rawIds = this.route.snapshot.queryParamMap.get('relatedDatasetIds');
    if (!rawIds) {
      return [];
    }

    const ids = rawIds.split(',').map((value) => Number(value.trim()));
    if (ids.some((id) => !Number.isInteger(id) || id <= 0)) {
      return null;
    }

    return [...new Set(ids)].sort((left, right) => left - right);
  }

  /**
   * Remplace les données et réinitialise la définition uniquement après un chargement valide.
   * Un échec de résolution met à jour le message sans supprimer la configuration déjà affichée.
   */
  private applyConfigurationLoadResult(result: ReportConfigurationLoadResult): void {
    if (result.status === 'main-dataset-not-found') {
      this.errorMessage.set('Cette source principale est introuvable, inactive ou invisible.');
      return;
    }

    if (result.status === 'related-dataset-not-directly-related') {
      this.errorMessage.set(
        'Une table liée sélectionnée est inconnue ou n’est plus directement reliée à la source principale.',
      );
      return;
    }

    this.selectedDatasets.set(result.selectedDatasets);
    this.fieldGroups.set(result.fieldGroups);
    if (!this.restoreDraft(result)) {
      this.resetReportDefinition();
    }
  }

  private resetData(): void {
    this.selectedDatasets.set([]);
    this.fieldGroups.set([]);
    this.resetReportDefinition();
  }

  /**
   * Réinitialise la définition du rapport, ordonne le reset de l’éditeur de filtres par révision
   * et supprime tout état d’aperçu associé à la configuration précédente.
   */
  private resetReportDefinition(): void {
    this.selectedFields.set([]);
    this.filters.set([]);
    this.filtersValid.set(true);
    this.sorts.set([]);
    this.filterResetRevision.update((revision) => revision + 1);
    this.clearPreview();
    this.schedulePreview();
  }

  private observePreviewIntents(): void {
    this.previewIntents
      .pipe(
        distinctUntilChanged(
          (previous, current) => !current.immediate && previous.key === current.key,
        ),
        switchMap((intent) => {
          const revision = ++this.previewRequestRevision;
          if (!intent.request) {
            this.previewLoading.set(false);
            this.previewStale.set(this.previewResult() !== null);
            return EMPTY;
          }

          this.previewLoading.set(true);
          this.previewError.set(null);
          this.previewStale.set(this.previewResult() !== null);

          return (intent.immediate ? of(0) : timer(PREVIEW_DEBOUNCE_MS)).pipe(
            switchMap(() => this.reportPreviewService.preview(intent.request!)),
            catchError((error: HttpErrorResponse) => {
              this.previewError.set(this.previewErrorMessage(error));
              this.previewStale.set(this.previewResult() !== null);
              return EMPTY;
            }),
            finalize(() => {
              if (revision === this.previewRequestRevision) {
                this.previewLoading.set(false);
              }
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((response) => {
        this.previewResult.set(response);
        this.previewStale.set(false);
      });
  }

  private schedulePreview(immediate = false): void {
    const rootDataset = this.selectedDatasets().find((dataset) => dataset.main);
    const request = rootDataset
      && this.selectedFields().length > 0
      && this.filtersValid()
      && !this.isLoading()
      && !this.errorMessage()
      && !this.isGenerating()
      ? this.createPreviewRequest(rootDataset)
      : null;

    this.previewIntents.next({
      request,
      immediate,
      key: request ? this.previewRequestKey(request) : 'invalid',
    });
  }

  private moveFocusOutOfHiddenMobilePanel(): void {
    const activeElement = globalThis.document?.activeElement as HTMLElement | null;
    const activeTab = this.mobileTab();
    const focusIsHidden = activeTab === 'preview'
      ? Boolean(activeElement?.closest('#configuration-panel'))
      : Boolean(activeElement?.closest('#preview-panel'));
    if (focusIsHidden) {
      globalThis.document?.getElementById(`${activeTab}-tab`)?.focus();
    }
  }

  private createPreviewRequest(rootDataset: SelectedDataset): ReportPreviewRequest {
    return {
      rootDatasetId: rootDataset.id,
      selectedFieldIds: this.selectedFields().map((field) => field.id),
      filters: this.filters(),
      sorts: this.sorts(),
    };
  }

  private previewRequestKey(request: ReportPreviewRequest): string {
    return JSON.stringify([
      request.rootDatasetId,
      request.selectedFieldIds,
      request.filters.map(({fieldId, operator, values}) => [fieldId, operator, values]),
      request.sorts.map(({fieldId, direction}) => [fieldId, direction]),
    ]);
  }

  private clearPreview(): void {
    this.previewResult.set(null);
    this.previewError.set(null);
    this.previewStale.set(false);
  }

  /** Retourne uniquement des messages locaux sûrs pour l’affichage persistant. */
  private previewErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 401) {
      return 'Votre session a expiré. Reconnectez-vous avant de demander un aperçu.';
    }
    if (error.status === 403) {
      return 'Vous n’avez pas accès à cet aperçu.';
    }
    if (error.status === 400) {
      return 'La configuration du rapport n’a pas pu être validée. Vérifiez les colonnes, filtres et tris.';
    }
    if (error.status === 409) {
      return 'Certaines données de la configuration ne sont plus disponibles. Rechargez la configuration.';
    }
    if (error.status === 504) {
      return 'L’aperçu a pris trop de temps. Réessayez ou précisez les filtres.';
    }
    if (error.status === 0) {
      return 'Le serveur est momentanément inaccessible. Vérifiez votre connexion puis réessayez.';
    }
    return 'Impossible de charger l’aperçu du rapport. Réessayez dans quelques instants.';
  }
}
