import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { DatasetField } from '../../models/dataset-field.model';
import { Dataset } from '../../models/dataset.model';
import { TableRelation } from '../../models/table-relation.model';
import { DatasetService } from '../../services/dataset.service';
import { ReportConfigurationLoadResult } from './configuration.models';
import { ReportConfigurationLoader } from './report-configuration-loader.service';

describe('ReportConfigurationLoader', () => {
  const datasets: readonly Dataset[] = [
    { id: 1, displayName: 'Employés', sourceName: 'rhis_employee' },
  ];
  const relations: readonly TableRelation[] = [
    relation(1, 3, 'rhis_detail_evenement', 'Détail événement'),
    relation(1, 2, 'rhis_contrat', 'Contrats'),
  ];
  const fieldsByDataset: Readonly<Record<number, readonly DatasetField[]>> = {
    1: [field(11, true), field(12, false)],
    2: [field(21, true)],
    3: [field(31, true)],
  };
  const datasetService = {
    getReportSources: jasmine.createSpy(),
    getDatasetFields: jasmine.createSpy(),
  };
  let loader: ReportConfigurationLoader;

  beforeEach(() => {
    datasetService.getReportSources.calls.reset();
    datasetService.getReportSources.and.returnValue(of({ datasets, relations }));
    datasetService.getDatasetFields.calls.reset();
    datasetService.getDatasetFields.and.callFake(
      (datasetId: number) => of(fieldsByDataset[datasetId] ?? []),
    );

    TestBed.configureTestingModule({
      providers: [
        ReportConfigurationLoader,
        { provide: DatasetService, useValue: datasetService },
      ],
    });
    loader = TestBed.inject(ReportConfigurationLoader);
  });

  it('loads the main dataset and outgoing related datasets in display order', () => {
    let result: ReportConfigurationLoadResult | undefined;

    loader.load(1, [3, 2]).subscribe((value) => (result = value));

    expect(result!).toEqual({
      status: 'loaded',
      selectedDatasets: [
        { ...datasets[0], main: true },
        { id: 2, displayName: 'Contrats', sourceName: 'rhis_contrat', main: false },
        {
          id: 3,
          displayName: 'Détail événement',
          sourceName: 'rhis_detail_evenement',
          main: false,
        },
      ],
      fieldGroups: [
        {
          dataset: { ...datasets[0], main: true },
          fields: [reportField(1, 'Employés', fieldsByDataset[1][0])],
        },
        {
          dataset: {
            id: 2,
            displayName: 'Contrats',
            sourceName: 'rhis_contrat',
            main: false,
          },
          fields: [reportField(2, 'Contrats', fieldsByDataset[2][0])],
        },
        {
          dataset: {
            id: 3,
            displayName: 'Détail événement',
            sourceName: 'rhis_detail_evenement',
            main: false,
          },
          fields: [reportField(3, 'Détail événement', fieldsByDataset[3][0])],
        },
      ],
    });
    expect(datasetService.getDatasetFields.calls.allArgs()).toEqual([[1], [2], [3]]);
  });

  it('reports a missing main dataset without loading fields', () => {
    let status: string | undefined;

    loader.load(99, []).subscribe((result) => (status = result.status));

    expect(status).toBe('main-dataset-not-found');
    expect(datasetService.getDatasetFields).not.toHaveBeenCalled();
  });

  it('rejects an incoming-only related dataset', () => {
    datasetService.getReportSources.and.returnValue(
      of({ datasets, relations: [relation(2, 1, 'rhis_employee', 'Employés')] }),
    );
    let status: string | undefined;

    loader.load(1, [2]).subscribe((result) => (status = result.status));

    expect(status).toBe('related-dataset-not-directly-related');
    expect(datasetService.getDatasetFields).not.toHaveBeenCalled();
  });

  function relation(
    sourceDatasetId: number,
    targetDatasetId: number,
    targetTable: string,
    targetDisplayName: string,
  ): TableRelation {
    return {
      sourceDatasetId,
      sourceTable: sourceDatasetId === 1 ? 'rhis_employee' : 'rhis_contrat',
      sourceDisplayName: sourceDatasetId === 1 ? 'Employés' : 'Contrats',
      sourceColumn: 'source_id',
      targetDatasetId,
      targetTable,
      targetDisplayName,
      targetColumn: 'target_id',
    };
  }

  function field(id: number, supported: boolean): DatasetField {
    return {
      id,
      displayName: `Champ ${id}`,
      sourceName: `field_${id}`,
      dataType: supported ? 'INTEGER' : 'UNSUPPORTED',
      nullable: false,
      supported,
      supportedOperators: supported ? ['EQUALS'] : [],
    };
  }

  function reportField(datasetId: number, datasetDisplayName: string, source: DatasetField) {
    return {
      ...source,
      key: `${datasetId}:${source.id}`,
      datasetId,
      datasetDisplayName,
    };
  }
});
