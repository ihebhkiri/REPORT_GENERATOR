import { inject, Injectable } from '@angular/core';
import { forkJoin, map, Observable, of, switchMap } from 'rxjs';

import { DatasetField } from '../../models/dataset-field.model';
import { Dataset } from '../../models/dataset.model';
import { TableRelation } from '../../models/table-relation.model';
import { DatasetService } from '../../services/dataset.service';
import {
  DatasetFieldGroup,
  ReportConfigurationLoadResult,
  ReportField,
  SelectedDataset,
} from './configuration.models';

type DatasetSelectionResult =
  | { readonly status: 'resolved'; readonly selectedDatasets: readonly SelectedDataset[] }
  | { readonly status: 'main-dataset-not-found' }
  | { readonly status: 'related-dataset-not-directly-related' };

/**
 * Résout les sources de données sélectionnées dans la route et charge les groupes de champs nécessaires
 * à la configuration du rapport.
 */
@Injectable({ providedIn: 'root' })
export class ReportConfigurationLoader {
  private readonly datasetService = inject(DatasetService);

  /**
   * Charge d’abord les métadonnées de sources, puis les champs seulement après une résolution
   * valide. Les échecs métier sont émis comme résultats, tandis que les erreurs HTTP sont propagées.
   */
  load(
    mainDatasetId: number,
    relatedDatasetIds: readonly number[],
  ): Observable<ReportConfigurationLoadResult> {
    return this.datasetService.getReportSources().pipe(
      switchMap(({ datasets, relations }) => {
        const selection = this.resolveSelectedDatasets(
          datasets,
          relations,
          mainDatasetId,
          relatedDatasetIds,
        );

        if (selection.status !== 'resolved') {
          return of(selection);
        }

        return this.loadFieldGroups(selection.selectedDatasets).pipe(
          map((fieldGroups) => ({
            status: 'loaded' as const,
            selectedDatasets: selection.selectedDatasets,
            fieldGroups,
          })),
        );
      }),
    );
  }

  /**
   * Charge les champs dans l’ordre des sources sélectionnés, retire les types non supportés et conserve
   * pour chaque groupe l’ordre de champs fourni par l’API.
   */
  private loadFieldGroups(
    selectedDatasets: readonly SelectedDataset[],
  ): Observable<readonly DatasetFieldGroup[]> {
    return forkJoin(
      selectedDatasets.map((dataset) =>
        this.datasetService.getDatasetFields(dataset.id).pipe(
          map((fields) => ({
            dataset,
            fields: fields
              .filter((field) => field.supported)
              .map((field) => this.toReportField(dataset, field)),
          })),
        ),
      ),
    );
  }

  /**
   * Accepte uniquement les relations directes sortantes et non réflexives du table principal. Les
   * métadonnées de relation définissent les tableaux liés ; le principal reste premier et les autres
   * sont triés par libellé selon la locale française.
   */

  private resolveSelectedDatasets(
    datasets: readonly Dataset[],
    relations: readonly TableRelation[],
    mainDatasetId: number,
    relatedDatasetIds: readonly number[],
  ): DatasetSelectionResult {
    const mainDataset = datasets.find((dataset) => dataset.id === mainDatasetId);
    if (!mainDataset) {
      return { status: 'main-dataset-not-found' };
    }

    const directlyRelatedDatasets = new Map<number, SelectedDataset>();
    for (const relation of relations) {
      const relatedDataset = this.toDirectlyRelatedDataset(relation, mainDatasetId);
      if (relatedDataset) {
        directlyRelatedDatasets.set(relatedDataset.id, relatedDataset);
      }
    }

    const relatedDatasets: SelectedDataset[] = [];
    for (const relatedDatasetId of relatedDatasetIds) {
      const relatedDataset = directlyRelatedDatasets.get(relatedDatasetId);
      if (!relatedDataset) {
        return { status: 'related-dataset-not-directly-related' };
      }
      relatedDatasets.push(relatedDataset);
    }

    relatedDatasets.sort((left, right) =>
      left.displayName.localeCompare(right.displayName, 'fr', { sensitivity: 'base' }),
    );

    return {
      status: 'resolved',
      selectedDatasets: [{ ...mainDataset, main: true }, ...relatedDatasets],
    };
  }

  private toDirectlyRelatedDataset(
    relation: TableRelation,
    mainDatasetId: number,
  ): SelectedDataset | null {
    if (relation.sourceDatasetId === mainDatasetId && relation.targetDatasetId !== mainDatasetId) {
      return {
        id: relation.targetDatasetId,
        displayName: relation.targetDisplayName,
        sourceName: relation.targetTable,
        main: false,
      };
    }

    return null;
  }

  /**
   * Enrichit un champ avec son contexte de dataset et une clé UI composite, sans modifier
   * l’identifiant de champ transmis au backend.
   */
  private toReportField(dataset: SelectedDataset, field: DatasetField): ReportField {
    return {
      ...field,
      key: `${dataset.id}:${field.id}`,
      datasetId: dataset.id,
      datasetDisplayName: dataset.displayName,
    };
  }
}
