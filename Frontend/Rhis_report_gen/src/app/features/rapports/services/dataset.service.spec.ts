import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { Dataset } from '../models/dataset.model';
import { DatasetField } from '../models/dataset-field.model';
import { TableRelation } from '../models/table-relation.model';
import { DatasetService } from './dataset.service';

describe('DatasetService', () => {
  let service: DatasetService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DatasetService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DatasetService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('loads datasets and relations together with credentials', () => {
    const datasets: readonly Dataset[] = [
      { id: 1, displayName: 'Employés', sourceName: 'rhis_employee' },
    ];
    const relations: readonly TableRelation[] = [];
    let result: {
      readonly datasets: readonly Dataset[];
      readonly relations: readonly TableRelation[];
    } | undefined;

    service.getReportSources().subscribe((sources) => (result = sources));

    const datasetsRequest = httpTesting.expectOne(`${environment.apiBaseUrl}/datasets`);
    const relationsRequest = httpTesting.expectOne(`${environment.apiBaseUrl}/datasets/relations`);
    expect(datasetsRequest.request.withCredentials).toBeTrue();
    expect(relationsRequest.request.withCredentials).toBeTrue();

    datasetsRequest.flush(datasets);
    expect(result).toBeUndefined();
    relationsRequest.flush(relations);

    expect(result).toEqual({ datasets, relations });
  });

  it('loads fields for the requested dataset with credentials', () => {
    const fields: readonly DatasetField[] = [];

    service.getDatasetFields(7).subscribe((result) => expect(result).toEqual(fields));

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/datasets/7/fields`);
    expect(request.request.withCredentials).toBeTrue();
    request.flush(fields);
  });
});
