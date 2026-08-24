import {HttpErrorResponse} from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormsModule} from '@angular/forms';
import {AccordionModule} from 'primeng/accordion';
import {ButtonModule} from 'primeng/button';
import {CheckboxModule} from 'primeng/checkbox';
import {InputTextModule} from 'primeng/inputtext';
import {MessageModule} from 'primeng/message';
import {SelectModule} from 'primeng/select';
import {SkeletonModule} from 'primeng/skeleton';

import {
  DatasetExposure,
  DatasetExposureMode,
  DatasetExposureUpdate,
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
    AccordionModule,
    ButtonModule,
    CheckboxModule,
    FormsModule,
    InputTextModule,
    MessageModule,
    SelectModule,
    SkeletonModule,
  ],
  templateUrl: './dataset-exposure.component.html',
  styleUrl: './dataset-exposure.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DatasetExposureComponent {
  private readonly exposureService = inject(DatasetExposureService);
  private readonly destroyRef = inject(DestroyRef);

  readonly modeOptions: ExposureModeOption[] = [
    {label: 'Non exposée', value: 'NONE'},
    {label: 'Principale uniquement', value: 'MAIN_ONLY'},
    {label: 'Relation uniquement', value: 'RELATED_ONLY'},
    {label: 'Principale et relation', value: 'MAIN_AND_RELATED'},
  ];

  readonly baseline = signal<readonly DatasetExposure[]>([]);
  readonly draft = signal<readonly DatasetExposure[]>([]);
  readonly searchTerm = signal('');
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly saveError = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly changes = computed<readonly DatasetExposureUpdate[]>(() => this.buildChanges());
  readonly changeCount = computed(() => this.changes().length);
  readonly dirty = computed(() => this.changeCount() > 0);
  readonly filteredDatasets = computed(() => {
    const search = this.searchTerm().trim().toLocaleLowerCase('fr');
    if (!search) {
      return this.draft();
    }
    return this.draft()
      .map((dataset) => {
        if (dataset.displayName.toLocaleLowerCase('fr').includes(search)) {
          return dataset;
        }
        return {
          ...dataset,
          fields: dataset.fields.filter((field) =>
            field.displayName.toLocaleLowerCase('fr').includes(search),
          ),
        };
      })
      .filter((dataset) => dataset.fields.length > 0);
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
          this.loadError.set(this.errorMessage(error, 'La configuration ne peut pas être chargée.'));
          this.loading.set(false);
        },
      });
  }

  updateSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
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
    this.clearSaveFeedback();
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
    this.clearSaveFeedback();
  }

  visibleFieldCount(dataset: DatasetExposure): number {
    return dataset.fields.filter((field) => field.active && field.visible).length;
  }

  fieldsDisabled(dataset: DatasetExposure): boolean {
    return !dataset.active || this.exposureMode(dataset) === 'NONE';
  }

  save(): void {
    const datasets = this.changes();
    if (datasets.length === 0 || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.saveError.set(null);
    this.successMessage.set(null);
    this.exposureService
      .updateConfiguration({datasets})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (configuration) => {
          this.setConfiguration(configuration.datasets);
          this.successMessage.set('Configuration enregistrée.');
          this.saving.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.saveError.set(this.errorMessage(error, 'Les modifications ne peuvent pas être enregistrées.'));
          this.saving.set(false);
        },
      });
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
        .filter((field) => field.active && baselineFields.get(field.id)?.visible !== field.visible)
        .map((field) => ({id: field.id, visible: field.visible}));
      const modeChanged =
        baseline.displayMain !== dataset.displayMain ||
        baseline.displayRelated !== dataset.displayRelated;
      if (!modeChanged && fields.length === 0) {
        return [];
      }
      return [
        {
          id: dataset.id,
          displayMain: dataset.displayMain,
          displayRelated: dataset.displayRelated,
          fields,
        },
      ];
    });
  }

  private setConfiguration(datasets: readonly DatasetExposure[]): void {
    const snapshot = datasets.map((dataset) => ({
      ...dataset,
      fields: dataset.fields.map((field) => ({...field})),
    }));
    this.baseline.set(snapshot);
    this.draft.set(snapshot.map((dataset) => ({...dataset, fields: dataset.fields.map((field) => ({...field}))})));
  }

  private clearSaveFeedback(): void {
    this.saveError.set(null);
    this.successMessage.set(null);
  }

  private errorMessage(error: HttpErrorResponse, fallback: string): string {
    const detail = error.error?.detail;
    return typeof detail === 'string' && detail.trim() ? detail : fallback;
  }
}
