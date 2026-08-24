import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';

import {environment} from '../../../../environments/environment';
import {
  DatasetExposureConfiguration,
  UpdateDatasetExposureRequest,
} from './dataset-exposure.model';

@Injectable({providedIn: 'root'})
export class DatasetExposureService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/admin/dataset-exposure`;

  getConfiguration(): Observable<DatasetExposureConfiguration> {
    return this.http.get<DatasetExposureConfiguration>(this.url, {withCredentials: true});
  }

  updateConfiguration(
    request: UpdateDatasetExposureRequest,
  ): Observable<DatasetExposureConfiguration> {
    return this.http.put<DatasetExposureConfiguration>(this.url, request, {
      withCredentials: true,
    });
  }
}
