import {HttpErrorResponse} from '@angular/common/http';
import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  HostListener,
  Injector,
  computed,
  inject,
  signal,
  viewChild,
  viewChildren,
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormsModule} from '@angular/forms';
import {MessageService} from 'primeng/api';
import {ButtonModule} from 'primeng/button';
import {CheckboxModule} from 'primeng/checkbox';
import {InputTextModule} from 'primeng/inputtext';
import {SelectModule} from 'primeng/select';
import {SkeletonModule} from 'primeng/skeleton';
import {TextareaModule} from 'primeng/textarea';

import {
  DatasetExposure,
  DatasetExposureMode,
  DatasetExposureUpdate,
  FieldExposure,
  fromExposureMode,
  toExposureMode,
} from './dataset-exposure.model';
import {DatasetExposureService} from './dataset-exposure.service';

interface ExposureModeOption {
  readonly label: string;
  readonly value: DatasetExposureMode;
}

@Component({
  selector: 'app-dataset-exposure',
  standalone: true,
  imports: [
    ButtonModule,
    CheckboxModule,
    FormsModule,
    InputTextModule,
    SelectModule,
    SkeletonModule,
    TextareaModule,
  ],
  templateUrl: './dataset-exposure.component.html',
  styleUrl: './dataset-exposure.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DatasetExposureComponent {
  private readonly exposureService = inject(DatasetExposureService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly hostElement = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly tablesHeading = viewChild<ElementRef<HTMLElement>>('tablesHeading');
  private readonly saveFailed = signal(false);
  private readonly detailHeading = viewChild<ElementRef<HTMLElement>>('detailHeading');
  private readonly tableOptions = viewChildren<ElementRef<HTMLButtonElement>>('tableOption');

  readonly modeOptions: ExposureModeOption[] = [
    {label: 'Non exposée', value: 'NONE'},
    {label: 'Principale uniquement', value: 'MAIN_ONLY'},
    {label: 'Relation uniquement', value: 'RELATED_ONLY'},
    {label: 'Principale et relation', value: 'MAIN_AND_RELATED'},
  ];

  readonly baseline = signal<readonly DatasetExposure[]>([]);
  readonly draft = signal<readonly DatasetExposure[]>([]);
  readonly selectedDatasetId = signal<number | null>(null);
  readonly tableSearchTerm = signal('');
  readonly fieldSearchTerm = signal('');
  readonly mobileDetailVisible = signal(false);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loadError = signal<string | null>(null);

  readonly changes = computed<readonly DatasetExposureUpdate[]>(() => this.buildChanges());
  readonly changeCount = computed(() => this.changes().length);
  readonly dirty = computed(() => this.changeCount() > 0);
  readonly metadataError = computed(() => {
    const entries = this.changes().flatMap((dataset) => [dataset, ...dataset.fields]);
    return entries.some((entry) => {
      const aliases = (entry.aliases ?? '').split(/\r?\n/).map((value) => value.trim()).filter(Boolean);
      return (entry.description?.length ?? 0) > 1000 || (entry.aliases?.length ?? 0) > 2000
        || new Set(aliases.map((value) => value.toLocaleLowerCase('fr'))).size > 20
        || aliases.some((value) => value.length > 100);
    }) ? 'Description : 1000 caractères maximum. Alias : 20 maximum, un par ligne, 100 caractères chacun et 2000 au total.' : null;
  });
  readonly saveError = computed(() => this.dirty() && this.saveFailed()
    ? 'Vos modifications sont conservées. Réessayez.' : null);
  readonly dirtyDatasetIds = computed<ReadonlySet<number>>(
    () => new Set(this.changes().map((dataset) => dataset.id)),
  );
  readonly selectedDataset = computed<DatasetExposure | null>(() =>
    this.draft().find((dataset) => dataset.id === this.selectedDatasetId()) ?? null,
  );
  readonly filteredDatasets = computed<readonly DatasetExposure[]>(() => {
    const search = this.normalizeSearch(this.tableSearchTerm());
    return search
      ? this.draft().filter((dataset) =>
          this.normalizeSearch(dataset.displayName).includes(search),
        )
      : this.draft();
  });
  readonly filteredFields = computed<readonly FieldExposure[]>(() => {
    const fields = this.selectedDataset()?.fields ?? [];
    const search = this.normalizeSearch(this.fieldSearchTerm());
    return search
      ? fields.filter((field) => this.normalizeSearch(field.displayName).includes(search))
      : fields;
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.exposureService
      .getConfiguration()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (configuration) => {
          this.setConfiguration(configuration.datasets);
          this.loading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          console.error('Dataset exposure configuration load failed.', error);
          this.loadError.set('Impossible de charger la configuration des données.');
          this.loading.set(false);
        },
      });
  }

  selectDataset(datasetId: number): void {
    if (!this.draft().some((dataset) => dataset.id === datasetId)) {
      return;
    }
    if (this.selectedDatasetId() !== datasetId) {
      this.fieldSearchTerm.set('');
    }
    this.selectedDatasetId.set(datasetId);
    this.mobileDetailVisible.set(true);
    this.focusDetailOnMobile();
  }

  showTableList(): void {
    this.mobileDetailVisible.set(false);
    if (!this.isMobileLayout()) {
      return;
    }
    afterNextRender(() => {
      const selectedId = String(this.selectedDatasetId());
      this.tableOptions()
        .find((option) => option.nativeElement.dataset['datasetId'] === selectedId)
        ?.nativeElement.focus();
    }, {injector: this.injector});
  }

  updateTableSearch(event: Event): void {
    this.tableSearchTerm.set((event.target as HTMLInputElement).value);
  }

  updateFieldSearch(event: Event): void {
    this.fieldSearchTerm.set((event.target as HTMLInputElement).value);
  }

  exposureMode(dataset: DatasetExposure): DatasetExposureMode {
    return toExposureMode(dataset.displayMain, dataset.displayRelated);
  }

  modeLabel(mode: DatasetExposureMode): string {
    return this.modeOptions.find((option) => option.value === mode)?.label ?? '';
  }

  updateMode(datasetId: number, mode: DatasetExposureMode): void {
    const exposure = fromExposureMode(mode);
    this.draft.update((datasets) =>
      datasets.map((dataset) =>
        dataset.id === datasetId && dataset.active ? {...dataset, ...exposure} : dataset,
      ),
    );
    if (!this.dirty()) this.saveFailed.set(false);
  }

  updateField(datasetId: number, fieldId: number, visible: boolean): void {
    this.draft.update((datasets) =>
      datasets.map((dataset) =>
        dataset.id === datasetId && dataset.active
          ? {
              ...dataset,
              fields: dataset.fields.map((field) =>
                field.id === fieldId && field.active ? {...field, visible} : field,
              ),
            }
          : dataset,
      ),
    );
    if (!this.dirty()) this.saveFailed.set(false);
  }

  visibleFieldCount(dataset: DatasetExposure): number {
    return dataset.fields.filter((field) => field.active && field.visible).length;
  }

  updateMetadata(datasetId: number, property: 'description' | 'aliases', value: string, fieldId?: number): void {
    if (this.saving()) return;
    this.draft.update((datasets) => datasets.map((dataset) => {
      if (dataset.id !== datasetId || !dataset.active) return dataset;
      if (fieldId === undefined) return {...dataset, [property]: value};
      return {...dataset, fields: dataset.fields.map((field) =>
        field.id === fieldId && field.active ? {...field, [property]: value} : field)};
    }));
    if (!this.dirty()) this.saveFailed.set(false);
  }

  fieldsDisabled(dataset: DatasetExposure): boolean {
    return !dataset.active || this.exposureMode(dataset) === 'NONE';
  }

  resetDraft(): void {
    if (this.saving()) {
      return;
    }
    this.restoreActionFocus();
    this.saveFailed.set(false);
    this.draft.set(this.baseline());
  }

  save(): void {
    const datasets = this.changes();
    if (datasets.length === 0 || this.saving() || this.metadataError()) {
      return;
    }
    const restoreFocus = !!this.hostElement.nativeElement.ownerDocument.activeElement
      ?.closest('[data-testid="action-bar"]');
    this.saveFailed.set(false);
    this.saving.set(true);
    this.exposureService
      .updateConfiguration({datasets})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (configuration) => {
          this.restoreActionFocus(restoreFocus);
          this.setConfiguration(configuration.datasets);
          this.saving.set(false);
          this.messageService.add({
            severity: 'success',
            summary: 'Enregistrement réussi',
            detail: 'La configuration a été enregistrée.',
          });
        },
        error: (error: HttpErrorResponse) => {
          console.error('Dataset exposure configuration save failed.', error);
          this.saveFailed.set(true);
          this.saving.set(false);
          this.messageService.add({
            severity: 'error',
            summary: 'Échec de l’enregistrement',
            detail: 'Vos modifications sont conservées. Réessayez.',
          });
        },
      });
  }

  hasUnsavedChanges(): boolean {
    return this.dirty();
  }

  @HostListener('window:beforeunload', ['$event'])
  handleBeforeUnload(event: BeforeUnloadEvent): void {
    if (!this.dirty()) {
      return;
    }
    event.preventDefault();
    event.returnValue = '';
  }

  private buildChanges(): readonly DatasetExposureUpdate[] {
    const baselineById = new Map(this.baseline().map((dataset) => [dataset.id, dataset]));
    return this.draft().flatMap((dataset) => {
      const baseline = baselineById.get(dataset.id);
      if (!baseline || !dataset.active) {
        return [];
      }
      const baselineFields = new Map(baseline.fields.map((field) => [field.id, field]));
      const fields = dataset.fields
        .flatMap((field) => {
          const original = baselineFields.get(field.id);
          if (!field.active || !original) return [];
          const metadata = this.metadataChanges(original, field);
          return original.visible !== field.visible || Object.keys(metadata).length > 0
            ? [{id: field.id, visible: field.visible, ...metadata}] : [];
        });
      const metadata = this.metadataChanges(baseline, dataset);
      const modeChanged =
        baseline.displayMain !== dataset.displayMain ||
        baseline.displayRelated !== dataset.displayRelated;
      if (!modeChanged && fields.length === 0 && Object.keys(metadata).length === 0) {
        return [];
      }
      return [
        {
          id: dataset.id,
          displayMain: dataset.displayMain,
          displayRelated: dataset.displayRelated,
          fields,
          ...metadata,
        },
      ];
    });
  }

  private setConfiguration(datasets: readonly DatasetExposure[]): void {
    this.baseline.set(datasets);
    this.draft.set(datasets);

    const selectedDatasetId = this.selectedDatasetId();
    if (
      selectedDatasetId !== null &&
      !datasets.some((dataset) => dataset.id === selectedDatasetId)
    ) {
      this.selectedDatasetId.set(null);
      this.mobileDetailVisible.set(false);
      this.fieldSearchTerm.set('');
    }
  }

  private metadataChanges(
    baseline: Pick<DatasetExposure, 'description' | 'aliases'>,
    draft: Pick<DatasetExposure, 'description' | 'aliases'>,
  ): {description?: string; aliases?: string} {
    const changes: {description?: string; aliases?: string} = {};
    if ((baseline.description ?? '') !== (draft.description ?? '')) changes.description = draft.description ?? '';
    if ((baseline.aliases ?? '') !== (draft.aliases ?? '')) changes.aliases = draft.aliases ?? '';
    return changes;
  }

  private normalizeSearch(value: string): string {
    return value.trim().toLocaleLowerCase('fr');
  }

  private focusDetailOnMobile(): void {
    if (!this.isMobileLayout()) {
      return;
    }
    afterNextRender(() => this.detailHeading()?.nativeElement.focus(), {injector: this.injector});
  }

  private restoreActionFocus(wasActionFocused = false): void {
    const activeElement = this.hostElement.nativeElement.ownerDocument.activeElement;
    if (!activeElement?.closest('[data-testid="action-bar"]') &&
        !(wasActionFocused && activeElement === this.hostElement.nativeElement.ownerDocument.body)) {
      return;
    }
    afterNextRender(() => {
      if (this.isMobileLayout() && !this.mobileDetailVisible()) {
        const selected = this.tableOptions().find(
          (option) => option.nativeElement.dataset['datasetId'] === String(this.selectedDatasetId()),
        );
        (selected ?? this.tablesHeading())?.nativeElement.focus();
      } else {
        (this.detailHeading() ?? this.tablesHeading())?.nativeElement.focus();
      }
    }, {injector: this.injector});
  }

  private isMobileLayout(): boolean {
    return window.matchMedia('(max-width: 63.999rem)').matches;
  }
}
