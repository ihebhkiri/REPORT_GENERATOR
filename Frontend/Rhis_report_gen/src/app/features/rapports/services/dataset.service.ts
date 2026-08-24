import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { forkJoin, Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { DatasetField } from '../models/dataset-field.model';
import { Dataset } from '../models/dataset.model';
import { TableRelation } from '../models/table-relation.model';

@Injectable({
  providedIn: 'root',
})
export class DatasetService {
  private readonly http = inject(HttpClient);
  private readonly datasetsUrl = `${environment.apiBaseUrl}/datasets`;
  private readonly relationsUrl = `${this.datasetsUrl}/relations`;

  getDatasets(): Observable<readonly Dataset[]> {
    return this.http.get<readonly Dataset[]>(this.datasetsUrl, {
      withCredentials: true,
    });
  }

  getRelations(): Observable<readonly TableRelation[]> {
    return this.http.get<readonly TableRelation[]>(this.relationsUrl, {
      withCredentials: true,
    });
  }

  getReportSources(): Observable<{
    readonly datasets: readonly Dataset[];
    readonly relations: readonly TableRelation[];
  }> {
    return forkJoin({
      datasets: this.getDatasets(),
      relations: this.getRelations(),
    });
  }

  getDatasetFields(datasetId: number): Observable<readonly DatasetField[]> {
    return this.http.get<readonly DatasetField[]>(`${this.datasetsUrl}/${datasetId}/fields`, {
      withCredentials: true,
    });
  }
}
