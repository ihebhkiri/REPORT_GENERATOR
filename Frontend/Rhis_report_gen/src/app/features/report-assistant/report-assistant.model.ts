import {ReportExportFormat} from '../rapports/models/report-generation.model';

export type BotReportStatus = 'READY' | 'NEEDS_CLARIFICATION' | 'FAILED';

export interface BotReportRequest {
  readonly message: string;
  readonly format: ReportExportFormat;
  readonly clarificationQuestion?: string | null;
  readonly clarificationAnswer?: string | null;
}

export interface BotReportResponse {
  readonly status: BotReportStatus;
  readonly question: string | null;
  readonly generationId: string | null;
  readonly format: ReportExportFormat | null;
  readonly planSummary: string | null;
  readonly errors: readonly string[];
}
