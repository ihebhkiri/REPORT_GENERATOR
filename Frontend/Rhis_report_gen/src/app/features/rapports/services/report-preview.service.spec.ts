import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { ReportPreviewRequest, ReportPreviewResponse } from '../models/report-preview.model';
import { ReportPreviewService } from './report-preview.service';

describe('ReportPreviewService', () => {
  let service: ReportPreviewService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ReportPreviewService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReportPreviewService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('posts the ordered preview definition with credentials', () => {
    const request: ReportPreviewRequest = {
      rootDatasetId: 1,
      selectedFieldIds: [12, 11],
      filters: [{ fieldId: 13, operator: 'EQUALS', values: ['true'] }],
      sorts: [
        { fieldId: 12, direction: 'DESC' },
        { fieldId: 11, direction: 'ASC' },
      ],
    };
    const response: ReportPreviewResponse = {
      columns: [],
      rows: [],
      hasMore: false,
      returnedRowCount: 0,
    };

    service.preview(request).subscribe((actual) => expect(actual).toEqual(response));

    const httpRequest = httpTesting.expectOne(`${environment.apiBaseUrl}/reports/preview`);
    expect(httpRequest.request.method).toBe('POST');
    expect(httpRequest.request.withCredentials).toBeTrue();
    expect(httpRequest.request.body).toEqual(request);
    httpRequest.flush(response);
  });
});
