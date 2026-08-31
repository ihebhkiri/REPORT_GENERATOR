import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';

import {environment} from '../../../environments/environment';
import {BotReportService} from './bot-report.service';

describe('BotReportService', () => {
  let service: BotReportService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});
    service = TestBed.inject(BotReportService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('posts the bot request with credentials and an idempotency key', () => {
    service.createReport({message: 'Liste des employés', format: 'XLSX'}).subscribe();
    const request = http.expectOne(`${environment.apiBaseUrl}/bot/reports`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({message: 'Liste des employés', format: 'XLSX'});
    expect(request.request.withCredentials).toBeTrue();
    expect(request.request.headers.get('Idempotency-Key')).toBeTruthy();
    request.flush({status: 'READY', question: null, generationId: 'generation-1',
      format: 'XLSX', planSummary: 'Rapport employés', errors: []});
  });
});
