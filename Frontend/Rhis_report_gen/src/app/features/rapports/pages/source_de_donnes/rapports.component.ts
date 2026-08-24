import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { finalize } from 'rxjs';

import { ReportRelatedCardComponent } from '../../components/report-related-card/report-related-card.component';
import { Dataset } from '../../models/dataset.model';
import { TableRelation } from '../../models/table-relation.model';
import { DatasetService } from '../../services/dataset.service';
import {ReportStepsComponent} from '../../../../shared/report-steps/report-steps.component';

interface ReportStep {
  readonly number: number;
  readonly label: string;
  readonly active: boolean;
}

interface RelatedDatasetView {
  readonly datasetId: number;
  readonly sourceName: string;
  readonly title: string;
  readonly description: string;
  readonly icon: string;
}

interface RelatedDatasetAccumulator extends Dataset {
  readonly mappings: Set<string>;
}

interface DatasetAccordionView {
  readonly dataset: Dataset;
  readonly icon: string;
  readonly relatedDatasets: readonly RelatedDatasetView[];
}

const DATASET_ICONS: Readonly<Record<string, string>> = {
  rhis_absence_conge: 'event_busy',
  rhis_contrat: 'contract',
  rhis_detail_evenement: 'event_note',
  rhis_employee: 'badge',
  rhis_pointage: 'schedule',
  rhis_restaurant: 'restaurant',
  rhis_shift: 'calendar_month',
};

@Component({
  selector: 'app-rapports',
  standalone: true,
  imports: [AccordionModule, ButtonModule, ProgressSpinnerModule, ReportRelatedCardComponent,  ReportStepsComponent],
  templateUrl: './rapports.component.html',
  styleUrl: './rapports.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RapportsComponent {
  private readonly datasetService = inject(DatasetService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  readonly steps: readonly ReportStep[] = [
    { number: 1, label: 'Source de données', active: true },
    { number: 2, label: 'Configuration', active: false },
    { number: 3, label: 'Export', active: false },
  ];
  readonly reportWizardSteps = [
    { value: 1, label: 'Source de données' },
    { value: 2, label: 'Configuration' },
    { value: 3, label: 'Export' },
  ] as const;

  readonly datasets = signal<readonly Dataset[]>([]);
  readonly relations = signal<readonly TableRelation[]>([]);
  readonly selectedDatasetId = signal<number | null>(null);
  readonly selectedRelatedDatasetIds = signal<ReadonlySet<number>>(new Set<number>());
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly datasetAccordions = computed<readonly DatasetAccordionView[]>(() =>
    this.buildDatasetAccordions(this.datasets(), this.relations()),
  );
  readonly selectedDatasetSummary = computed<readonly Dataset[]>(() => {
    const mainDatasetId = this.selectedDatasetId();
    if (mainDatasetId === null) {
      return [];
    }

    const mainDataset = this.datasets().find((dataset) => dataset.id === mainDatasetId);
    if (!mainDataset) {
      return [];
    }

    const relatedIds = this.selectedRelatedDatasetIds();
    const relatedDatasets = this.datasets()
      .filter((dataset) => dataset.id !== mainDatasetId && relatedIds.has(dataset.id))
      .sort((left, right) =>
        left.displayName.localeCompare(right.displayName, 'fr', { sensitivity: 'base' }),
      );

    return [mainDataset, ...relatedDatasets];
  });

  constructor() {
    this.loadReportSources();
  }

  loadReportSources(): void {
    if (this.isLoading()) {
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.datasetService
      .getReportSources()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: ({ datasets, relations }) => this.setReportSources(datasets, relations),
        error: () => {
          this.setReportSources([], []);
          this.errorMessage.set(
            'Impossible de charger les sources de données et leurs relations. Réessayez dans quelques instants.',
          );
        },
      });
  }

  openDataset(datasetId: number): void {
    if (this.selectedDatasetId() === datasetId) {
      return;
    }

    this.setSelectedDataset(datasetId);
  }

  closeDataset(datasetId: number): void {
    if (this.selectedDatasetId() !== datasetId) {
      return;
    }

    this.setSelectedDataset(null);
  }

  toggleRelatedDataset(datasetId: number, selected: boolean): void {
    this.selectedRelatedDatasetIds.update((currentIds) => {
      const nextIds = new Set(currentIds);

      if (selected) {
        nextIds.add(datasetId);
      } else {
        nextIds.delete(datasetId);
      }

      return nextIds;
    });
  }

  isRelatedDatasetSelected(datasetId: number): boolean {
    return this.selectedRelatedDatasetIds().has(datasetId);
  }

  continueToConfiguration(): void {
    const datasetId = this.selectedDatasetId();
    if (datasetId === null) {
      return;
    }

    const relatedDatasetIds = [...this.selectedRelatedDatasetIds()]
      .sort((left, right) => left - right)
      .join(',');

    void this.router.navigate(['/rapports/configuration', datasetId], {
      queryParams: relatedDatasetIds ? { relatedDatasetIds } : {},
    });
  }

  private setSelectedDataset(datasetId: number | null): void {
    this.selectedDatasetId.set(datasetId);
    this.selectedRelatedDatasetIds.set(new Set<number>());
  }

  private setReportSources(
    datasets: readonly Dataset[],
    relations: readonly TableRelation[],
  ): void {
    this.datasets.set(datasets);
    this.relations.set(relations);
    this.setSelectedDataset(null);
  }

  private buildDatasetAccordions(
    datasets: readonly Dataset[],
    relations: readonly TableRelation[],
  ): readonly DatasetAccordionView[] {
    const sortedDatasets = [...datasets].sort((left, right) =>
      left.displayName.localeCompare(right.displayName, 'fr', { sensitivity: 'base' }),
    );
    const mappingsByDataset = this.buildRelationMappings(sortedDatasets, relations);

    return sortedDatasets.map((dataset) => {
      const relatedMappings = mappingsByDataset.get(dataset.id);
      const relatedDatasets = [...(relatedMappings?.values() ?? [])]
        .map(
          (relatedDataset) =>
            ({
              datasetId: relatedDataset.id,
              sourceName: relatedDataset.sourceName,
              title: relatedDataset.displayName,
              description: [...relatedDataset.mappings].join(' · '),
              icon: this.datasetIcon(relatedDataset.sourceName),
            }) satisfies RelatedDatasetView,
        )
        .sort((left, right) =>
          left.title.localeCompare(right.title, 'fr', { sensitivity: 'base' }),
        );

      return {
        dataset,
        icon: this.datasetIcon(dataset.sourceName),
        relatedDatasets,
      } satisfies DatasetAccordionView;
    });
  }

  private buildRelationMappings(
    datasets: readonly Dataset[],
    relations: readonly TableRelation[],
  ): Map<number, Map<number, RelatedDatasetAccumulator>> {
    const mappingsByDataset = new Map<number, Map<number, RelatedDatasetAccumulator>>(
      datasets.map((dataset) => [dataset.id, new Map<number, RelatedDatasetAccumulator>()]),
    );

    for (const relation of relations) {
      if (relation.sourceTable === relation.targetTable) {
        continue;
      }

      const mapping = `${relation.sourceTable}.${relation.sourceColumn} → ${relation.targetTable}.${relation.targetColumn}`;

      this.addRelationMapping(
        mappingsByDataset.get(relation.sourceDatasetId),
        {
          id: relation.targetDatasetId,
          sourceName: relation.targetTable,
          displayName: relation.targetDisplayName,
        },
        mapping,
      );
    }

    return mappingsByDataset;
  }

  private addRelationMapping(
    relatedMappings: Map<number, RelatedDatasetAccumulator> | undefined,
    relatedDataset: Dataset,
    mapping: string,
  ): void {
    if (!relatedMappings) {
      return;
    }

    const accumulator = relatedMappings.get(relatedDataset.id) ?? {
      ...relatedDataset,
      mappings: new Set<string>(),
    };
    accumulator.mappings.add(mapping);
    relatedMappings.set(relatedDataset.id, accumulator);
  }

  private datasetIcon(sourceName: string): string {
    return DATASET_ICONS[sourceName] ?? 'dataset';
  }
}
