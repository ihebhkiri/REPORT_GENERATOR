import { DatasetFieldType, FilterOperator } from './dataset-field.model';

export type SortDirection = 'ASC' | 'DESC';
export type ReportPreviewCell = string | number | boolean | null;
export type ReportPreviewRow = Readonly<Record<string, ReportPreviewCell>>;

export interface ReportFilterRequest {
  readonly fieldId: number;
  readonly operator: FilterOperator;
  readonly values: readonly string[];
}

export interface ReportSortRequest {
  readonly fieldId: number;
  readonly direction: SortDirection;
}

export interface ReportPreviewRequest {
  readonly rootDatasetId: number;
  readonly selectedFieldIds: readonly number[];
  readonly filters: readonly ReportFilterRequest[];
  readonly sorts: readonly ReportSortRequest[];
}

export interface ReportPreviewColumn {
  readonly key: string;
  readonly fieldId: number;
  readonly displayName: string;
  readonly dataType: DatasetFieldType;
}

export interface ReportPreviewResponse {
  readonly columns: readonly ReportPreviewColumn[];
  readonly rows: readonly ReportPreviewRow[];
  readonly hasMore: boolean;
  readonly returnedRowCount: number;
}

export interface ApiProblem {
  readonly status?: number;
  readonly title?: string;
  readonly detail?: string;
}
