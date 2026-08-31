import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';

import {environment} from '../../../environments/environment';
import {BotReportRequest, BotReportResponse} from './report-assistant.model';

@Injectable({providedIn: 'root'})
export class BotReportService {
  private readonly http = inject(HttpClient);

  createReport(request: BotReportRequest): Observable<BotReportResponse> {
    return this.http.post<BotReportResponse>(`${environment.apiBaseUrl}/bot/reports`, request, {
      headers: {'Idempotency-Key': crypto.randomUUID()},
      withCredentials: true,
    });
  }
}
