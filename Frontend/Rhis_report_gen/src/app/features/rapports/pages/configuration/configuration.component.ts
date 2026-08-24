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
import {finalize} from 'rxjs';

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
import {PreviewDialogComponent} from './components/preview-dialog/preview-dialog.component';
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

/**
 * Gère la définition locale du rapport et le cycle de vie de l’aperçu pour cette page.
 * La résolution des jeux de données et le chargement des champs restent confiés à
 * `ReportConfigurationLoader`, tandis que ce composant conserve l’état de référence des éditeurs
 * enfants et déclenche explicitement les demandes d’aperçu.
 */
@Component({
  selector: 'app-configuration',
  standalone: true,
  imports: [
    ButtonModule,
    ColumnSelectorComponent,
    FilterEditorComponent,
    PreviewDialogComponent,
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
  readonly previewVisible = signal(false);
  readonly previewLoading = signal(false);
  readonly previewResult = signal<ReportPreviewResponse | null>(null);
  readonly previewError = signal<string | null>(null);
  readonly previewStale = signal(false);
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
  readonly canPreview = computed(
    () => this.selectedFields().length > 0
      && this.filtersValid()
      && !this.previewLoading()
      && !this.isGenerating(),
  );
  readonly canGenerate = computed(
    () => this.selectedFields().length > 0 && this.filtersValid() && !this.isGenerating(),
  );

  constructor() {
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

    this.reportConfigurationLoader
      .load(mainDatasetId, relatedDatasetIds)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
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
   * puis signale que l’aperçu existant ne correspond plus à la définition courante.
   */
  updateSelectedFields(fields: readonly ReportField[]): void {
    this.selectedFields.set(fields);
    const selectedIds = new Set(fields.map((field) => field.id));
    this.filters.update((filters) =>
      filters.filter((filter) => selectedIds.has(filter.fieldId)),
    );
    this.sorts.update((sorts) => sorts.filter((sort) => selectedIds.has(sort.fieldId)));
    this.markPreviewAsPrevious();
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
    this.markPreviewAsPrevious();
  }

  updateSorts(sorts: readonly ReportSortRequest[]): void {
    this.sorts.set(sorts);
    this.markPreviewAsPrevious();
  }

  openPreview(): void {
    if (!this.canPreview()) {
      return;
    }

    this.previewVisible.set(true);
    this.loadPreview();
  }

  retryPreview(): void {
    if (this.previewLoading()) {
      return;
    }
    this.loadPreview();
  }

  setPreviewVisible(visible: boolean): void {
    this.previewVisible.set(visible);
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

    this.reportGenerationService
      .startReportGeneration(definition, globalThis.crypto.randomUUID())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isGenerating.set(false)),
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
  }

  /**
   * Exécute une demande explicite d’aperçu. Un résultat précédent reste affichable et marqué comme
   * ancien pendant le rafraîchissement ou après une erreur, puis est remplacé uniquement au succès.
   */
  private loadPreview(): void {
    const rootDataset = this.selectedDatasets().find((dataset) => dataset.main);
    if (!rootDataset || !this.canPreview()) {
      return;
    }

    const request = this.createPreviewRequest(rootDataset);

    this.previewLoading.set(true);
    this.previewError.set(null);
    this.previewStale.set(this.previewResult() !== null);

    this.reportPreviewService
      .preview(request)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.previewLoading.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.previewResult.set(response);
          this.previewStale.set(false);
        },
        error: (error: HttpErrorResponse) => this.previewError.set(this.previewErrorMessage(error)),
      });
  }

  private createPreviewRequest(rootDataset: SelectedDataset): ReportPreviewRequest {
    return {
      rootDatasetId: rootDataset.id,
      selectedFieldIds: this.selectedFields().map((field) => field.id),
      filters: this.filters(),
      sorts: this.sorts(),
    };
  }

  private markPreviewAsPrevious(): void {
    if (this.previewResult() !== null) {
      this.previewStale.set(true);
    }
    this.previewError.set(null);
  }

  private clearPreview(): void {
    this.previewResult.set(null);
    this.previewError.set(null);
    this.previewStale.set(false);
  }

  /**
   * Privilégie le détail métier renvoyé par l’API, puis applique les messages dédiés à l’expiration
   * de session et à l’indisponibilité réseau avant le message générique.
   */
  private previewErrorMessage(error: HttpErrorResponse): string {
    const problem = error.error as ApiProblem | null;
    if (problem && typeof problem === 'object' && typeof problem.detail === 'string') {
      return problem.detail;
    }

    if (error.status === 401) {
      return 'Votre session a expiré. Reconnectez-vous avant de demander un aperçu.';
    }
    if (error.status === 0) {
      return 'Le serveur est momentanément inaccessible. Vérifiez votre connexion puis réessayez.';
    }
    return 'Impossible de charger l’aperçu du rapport.';
  }
}
