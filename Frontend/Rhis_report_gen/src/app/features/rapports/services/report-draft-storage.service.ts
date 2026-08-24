import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';

import { ReportDraft, ReportExportFormat } from '../models/report-generation.model';

const DRAFT_KEY = 'rhis.report.draft.v1';
const EXPORTS_PREFIX = 'rhis.report.exports.v1.';

@Injectable({ providedIn: 'root' })
export class ReportDraftStorageService {
  private readonly document = inject(DOCUMENT);

  save(draft: ReportDraft): void {
    this.storage()?.setItem(DRAFT_KEY, JSON.stringify(draft));
  }

  load(): ReportDraft | null {
    const storage = this.storage();
    const raw = storage?.getItem(DRAFT_KEY);
    if (!storage || !raw) {
      return null;
    }
    try {
      const parsedDraft = JSON.parse(raw) as Partial<ReportDraft>;
      if (
        parsedDraft.version !== 1
        || !parsedDraft.definition
        || !Array.isArray(parsedDraft.relatedDatasetIds)
      ) {
        throw new Error('Invalid report draft');
      }
      return parsedDraft as ReportDraft;
    } catch {
      storage.removeItem(DRAFT_KEY);
      return null;
    }
  }

  saveExportId(generationId: string, format: ReportExportFormat, exportId: string): void {
    const savedExportIds = this.loadExportIds(generationId);
    this.storage()?.setItem(
      EXPORTS_PREFIX + generationId,
      JSON.stringify({ ...savedExportIds, [format]: exportId }),
    );
  }

  loadExportIds(generationId: string): Partial<Record<ReportExportFormat, string>> {
    const raw = this.storage()?.getItem(EXPORTS_PREFIX + generationId);
    if (!raw) {
      return {};
    }
    try {
      const parsedExportIds = JSON.parse(raw) as Partial<Record<ReportExportFormat, string>>;
      return typeof parsedExportIds === 'object' && parsedExportIds !== null
        ? parsedExportIds
        : {};
    } catch {
      this.clearExportIds(generationId);
      return {};
    }
  }

  clearExportIds(generationId: string): void {
    this.storage()?.removeItem(EXPORTS_PREFIX + generationId);
  }

  private storage(): Storage | null {
    return this.document.defaultView?.sessionStorage ?? null;
  }
}
