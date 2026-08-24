import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';

import {environment} from '../../../../environments/environment';
import {DatasetExposureService} from './dataset-exposure.service';

describe('DatasetExposureService', () => {
  let service: DatasetExposureService;
  let httpTesting: HttpTestingController;
  const url = `${environment.apiBaseUrl}/admin/dataset-exposure`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DatasetExposureService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DatasetExposureService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('loads the configuration with credentials', () => {
    service.getConfiguration().subscribe();
    const request = httpTesting.expectOne(url);
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBeTrue();
    request.flush({datasets: []});
  });

  it('saves the configuration in one PUT', () => {
    const body = {
      datasets: [{id: 1, displayMain: false, displayRelated: true, fields: []}],
    };
    service.updateConfiguration(body).subscribe();
    const request = httpTesting.expectOne(url);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(body);
    expect(request.request.withCredentials).toBeTrue();
    request.flush({datasets: []});
  });
});
