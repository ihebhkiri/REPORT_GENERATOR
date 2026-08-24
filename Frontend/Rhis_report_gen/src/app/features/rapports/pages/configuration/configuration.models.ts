import { DatasetField } from '../../models/dataset-field.model';
import { Dataset } from '../../models/dataset.model';

export interface SelectedDataset extends Dataset {
  readonly main: boolean;
}

export interface ReportField extends DatasetField {
  readonly key: string;
  readonly datasetId: number;
  readonly datasetDisplayName: string;
}

export interface DatasetFieldGroup {
  readonly dataset: SelectedDataset;
  readonly fields: readonly ReportField[];
}

export type ReportConfigurationLoadResult =
  | {
      readonly status: 'loaded';
      readonly selectedDatasets: readonly SelectedDataset[];
      readonly fieldGroups: readonly DatasetFieldGroup[];
    }
  | { readonly status: 'main-dataset-not-found' }
  | { readonly status: 'related-dataset-not-directly-related' };
