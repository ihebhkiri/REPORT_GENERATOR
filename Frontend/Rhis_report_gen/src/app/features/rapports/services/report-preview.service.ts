import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  ReportPreviewRequest,
  ReportPreviewResponse,
} from '../models/report-preview.model';

@Injectable({
  providedIn: 'root',
})
export class ReportPreviewService {
  private readonly http = inject(HttpClient);
  private readonly previewUrl = `${environment.apiBaseUrl}/reports/preview`;

  preview(request: ReportPreviewRequest): Observable<ReportPreviewResponse> {
    return this.http.post<ReportPreviewResponse>(this.previewUrl, request, {
      withCredentials: true,
    });
  }
}
