import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ReportPreviewRequest } from '../models/report-preview.model';
import {
  ReportExport,
  ReportExportFormat,
  ReportGeneration,
} from '../models/report-generation.model';

@Injectable({ providedIn: 'root' })
export class ReportGenerationService {
  private readonly http = inject(HttpClient);
  private readonly generationsUrl = `${environment.apiBaseUrl}/report-generations`;
  private readonly exportsUrl = `${environment.apiBaseUrl}/report-exports`;

  startReportGeneration(
    definition: ReportPreviewRequest,
    idempotencyKey: string,
  ): Observable<ReportGeneration> {
    return this.http.post<ReportGeneration>(this.generationsUrl, definition, {
      headers: { 'Idempotency-Key': idempotencyKey },
      withCredentials: true,
    });
  }

  getReportGeneration(generationId: string): Observable<ReportGeneration> {
    return this.http.get<ReportGeneration>(`${this.generationsUrl}/${generationId}`, {
      withCredentials: true,
    });
  }

  deleteGeneration(generationId: string): Observable<void> {
    return this.http.delete<void>(`${this.generationsUrl}/${generationId}`, {
      withCredentials: true,
    });
  }

  startReportExport(
    generationId: string,
    format: ReportExportFormat,
  ): Observable<ReportExport> {
    return this.http.post<ReportExport>(
      `${this.generationsUrl}/${generationId}/exports`,
      { format },
      { withCredentials: true },
    );
  }

  getReportExport(exportId: string): Observable<ReportExport> {
    return this.http.get<ReportExport>(`${this.exportsUrl}/${exportId}`, {
      withCredentials: true,
    });
  }

  downloadExportFile(exportId: string): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.exportsUrl}/${exportId}/file`, {
      observe: 'response',
      responseType: 'blob',
      withCredentials: true,
    });
  }
}
