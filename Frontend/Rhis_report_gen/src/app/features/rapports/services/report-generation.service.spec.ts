import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../../environments/environment';
import { ReportGenerationService } from './report-generation.service';

describe('ReportGenerationService', () => {
  let service: ReportGenerationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ReportGenerationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('creates a generation once with credentials and its idempotency key', () => {
    const definition = { rootDatasetId: 1, selectedFieldIds: [11], filters: [], sorts: [] };
    service.startReportGeneration(definition, 'request-key').subscribe();

    const request = http.expectOne(`${environment.apiBaseUrl}/report-generations`);
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBeTrue();
    expect(request.request.headers.get('Idempotency-Key')).toBe('request-key');
    expect(request.request.body).toEqual(definition);
    request.flush({});
  });

  it('uses separate generation and export resources', () => {
    service.getReportGeneration('generation-id').subscribe();
    http.expectOne(`${environment.apiBaseUrl}/report-generations/generation-id`).flush({});

    service.startReportExport('generation-id', 'PDF').subscribe();
    const create = http.expectOne(`${environment.apiBaseUrl}/report-generations/generation-id/exports`);
    expect(create.request.body).toEqual({ format: 'PDF' });
    create.flush({});

    service.getReportExport('export-id').subscribe();
    http.expectOne(`${environment.apiBaseUrl}/report-exports/export-id`).flush({});

    service.downloadExportFile('export-id').subscribe();
    const download = http.expectOne(`${environment.apiBaseUrl}/report-exports/export-id/file`);
    expect(download.request.responseType).toBe('blob');
    download.flush(new Blob());

    service.deleteGeneration('generation-id').subscribe();
    const deletion = http.expectOne(`${environment.apiBaseUrl}/report-generations/generation-id`);
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
  });
});
