import { ReportPreviewRequest } from './report-preview.model';

export type ReportGenerationStatus = 'PENDING' | 'RUNNING' | 'READY' | 'FAILED' | 'EXPIRED';
export type ReportGenerationPhase = 'VALIDATING' | 'COUNTING' | 'READING_ROWS' | 'FINALIZING';
export type ReportExportStatus = 'PENDING' | 'RUNNING' | 'READY' | 'FAILED';
export type ReportExportFormat = 'PDF' | 'XLSX';

export interface UnavailableReportElement {
  readonly kind: 'DATASET' | 'FIELD' | 'RELATION';
  readonly id: number;
  readonly displayName: string;
  readonly reason: string;
}

export interface ReportGeneration {
  readonly generationId: string;
  readonly status: ReportGenerationStatus;
  readonly phase: ReportGenerationPhase | null;
  readonly progress: number;
  readonly processedRowCount: number | null;
  readonly totalRowCount: number | null;
  readonly createdAt: string;
  readonly expiresAt: string;
  readonly errorCode: string | null;
  readonly unavailableElements?: readonly UnavailableReportElement[];
}

export interface ReportExport {
  readonly exportId: string;
  readonly generationId: string;
  readonly format: ReportExportFormat;
  readonly status: ReportExportStatus;
  readonly progress: number;
  readonly errorCode: string | null;
  readonly unavailableElements?: readonly UnavailableReportElement[];
}

export interface ReportDraft {
  readonly version: 1;
  readonly definition: ReportPreviewRequest;
  readonly relatedDatasetIds: readonly number[];
}
