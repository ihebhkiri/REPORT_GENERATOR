import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { Dataset } from '../../models/dataset.model';
import { DatasetField } from '../../models/dataset-field.model';
import { TableRelation } from '../../models/table-relation.model';
import { DatasetService } from '../../services/dataset.service';
import { ReportPreviewService } from '../../services/report-preview.service';
import { ReportGenerationService } from '../../services/report-generation.service';
import { ReportDraftStorageService } from '../../services/report-draft-storage.service';
import { ConfigurationComponent } from './configuration.component';

describe('ConfigurationComponent', () => {
  let fixture: ComponentFixture<ConfigurationComponent>;
  let component: ConfigurationComponent;
  let navigateSpy: jasmine.Spy;

  const datasets: readonly Dataset[] = [
    { id: 1, displayName: 'Employés', sourceName: 'rhis_employee' },
    { id: 2, displayName: 'Contrats', sourceName: 'rhis_contrat' },
  ];
  const relations: readonly TableRelation[] = [
    {
      sourceDatasetId: 1,
      sourceTable: 'rhis_employee',
      sourceDisplayName: 'Employés',
      sourceColumn: 'contrat_fk_id',
      targetDatasetId: 2,
      targetTable: 'rhis_contrat',
      targetDisplayName: 'Contrats',
      targetColumn: 'cont_pk_id',
    },
    {
      sourceDatasetId: 1,
      sourceTable: 'rhis_employee',
      sourceDisplayName: 'Employés',
      sourceColumn: 'detail_fk_id',
      targetDatasetId: 3,
      targetTable: 'rhis_detail_evenement',
      targetDisplayName: 'Détail événement',
      targetColumn: 'detail_pk_id',
    },
  ];
  const fieldsByDataset: Readonly<Record<number, readonly DatasetField[]>> = {
    1: [field(11, 'ID', 'emp_pk_id', 'INTEGER')],
    2: [field(21, 'ID', 'cont_pk_id', 'INTEGER')],
    3: [field(31, 'Date événement', 'date_event', 'DATE')],
  };
  const routeStub = {
    snapshot: {
      paramMap: convertToParamMap({ datasetId: '1' }),
      queryParamMap: convertToParamMap({ relatedDatasetIds: '2,3,2' }),
    },
  };
  const datasetService = {
    getReportSources: jasmine.createSpy().and.returnValue(of({ datasets, relations })),
    getDatasetFields: jasmine
      .createSpy()
      .and.callFake((datasetId: number) => of(fieldsByDataset[datasetId] ?? [])),
  };
  const reportPreviewService = {
    preview: jasmine.createSpy(),
  };
  const reportGenerationService = {
    startReportGeneration: jasmine.createSpy().and.returnValue(of({ generationId: 'generation-id' })),
  };
  const reportDraftStorage = {
    save: jasmine.createSpy(),
    load: jasmine.createSpy().and.returnValue(null),
  };
  beforeEach(async () => {
    routeStub.snapshot = {
      paramMap: convertToParamMap({ datasetId: '1' }),
      queryParamMap: convertToParamMap({ relatedDatasetIds: '2,3,2' }),
    };
    datasetService.getReportSources.calls.reset();
    datasetService.getReportSources.and.returnValue(of({ datasets, relations }));
    datasetService.getDatasetFields.calls.reset();
    datasetService.getDatasetFields.and.callFake(
      (datasetId: number) => of(fieldsByDataset[datasetId] ?? []),
    );
    reportPreviewService.preview.calls.reset();
    reportPreviewService.preview.and.returnValue(
      of({ columns: [], rows: [], hasMore: false, returnedRowCount: 0 }),
    );
    reportGenerationService.startReportGeneration.calls.reset();
    reportGenerationService.startReportGeneration.and.returnValue(of({ generationId: 'generation-id' }));
    reportDraftStorage.save.calls.reset();
    reportDraftStorage.load.calls.reset();
    reportDraftStorage.load.and.returnValue(null);
    await TestBed.configureTestingModule({
      imports: [ConfigurationComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: DatasetService, useValue: datasetService },
        { provide: ReportPreviewService, useValue: reportPreviewService },
        { provide: ReportGenerationService, useValue: reportGenerationService },
        { provide: ReportDraftStorageService, useValue: reportDraftStorage },
      ],
    }).compileComponents();

    navigateSpy = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
  });

  function createComponent(): void {
    fixture = TestBed.createComponent(ConfigurationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('loads the main and deduplicated directly related datasets', () => {
    createComponent();

    expect(component.selectedDatasets().map((dataset) => dataset.id)).toEqual([1, 2, 3]);
    expect(datasetService.getDatasetFields.calls.allArgs()).toEqual([[1], [2], [3]]);
    expect(component.fieldGroups().length).toBe(3);
  });

  it('keeps the final field selection received from the column selector', () => {
    createComponent();

    const employeeId = component.fieldGroups()[0].fields[0];
    const contractId = component.fieldGroups()[1].fields[0];
    component.updateSelectedFields([employeeId, contractId]);

    expect(component.selectedFields().map((field) => field.key)).toEqual(['1:11', '2:21']);
    expect(component.selectedCount()).toBe(2);
    expect(component.filterFieldGroups().map((group) => group.fields.map((field) => field.key)))
      .toEqual([['1:11'], ['2:21']]);
  });

  it('enables generation only when columns are selected and filters are valid', () => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];

    expect(component.canGenerate()).toBeFalse();

    component.updateSelectedFields([employeeId]);
    expect(component.canGenerate()).toBeTrue();

    component.updateFilters({ filters: [], count: 1, valid: false });
    expect(component.canGenerate()).toBeFalse();
  });

  it('creates one generation and navigates with its id', () => {
    createComponent();
    component.updateSelectedFields([component.fieldGroups()[0].fields[0]]);

    component.continueToExport();

    expect(reportGenerationService.startReportGeneration).toHaveBeenCalledTimes(1);
    expect(reportDraftStorage.save).toHaveBeenCalledTimes(1);
    expect(navigateSpy).toHaveBeenCalledOnceWith(['/rapports/export', 'generation-id']);
  });

  it('builds the ordered preview request from columns, filters and sorts', () => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];
    const contractId = component.fieldGroups()[1].fields[0];
    component.updateSelectedFields([contractId, employeeId]);
    component.updateFilters({
      filters: [{ fieldId: employeeId.id, operator: 'EQUALS', values: ['42'] }],
      count: 1,
      valid: true,
    });
    component.updateSorts([
      { fieldId: contractId.id, direction: 'DESC' },
      { fieldId: employeeId.id, direction: 'ASC' },
    ]);

    component.openPreview();

    expect(reportPreviewService.preview).toHaveBeenCalledOnceWith({
      rootDatasetId: 1,
      selectedFieldIds: [21, 11],
      filters: [{ fieldId: 11, operator: 'EQUALS', values: ['42'] }],
      sorts: [
        { fieldId: 21, direction: 'DESC' },
        { fieldId: 11, direction: 'ASC' },
      ],
    });
    expect(component.previewVisible()).toBeTrue();
    expect(component.previewResult()?.returnedRowCount).toBe(0);
  });

  it('never calls preview automatically when columns, filters or sorts change', () => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];

    component.updateSelectedFields([employeeId]);
    component.updateFilters({
      filters: [{ fieldId: employeeId.id, operator: 'EQUALS', values: ['42'] }],
      count: 1,
      valid: true,
    });
    component.updateSorts([{ fieldId: employeeId.id, direction: 'ASC' }]);
    component.updateFilters({ filters: [], count: 0, valid: true });

    expect(reportPreviewService.preview).not.toHaveBeenCalled();
  });

  it('keeps the last valid filters and preview while an invalid draft is corrected', () => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];
    const validFilter = { fieldId: employeeId.id, operator: 'EQUALS' as const, values: ['42'] };
    reportPreviewService.preview.and.returnValue(
      of({
        columns: [
          { key: 'c0', fieldId: employeeId.id, displayName: 'ID', dataType: 'INTEGER' },
        ],
        rows: [{ c0: 42 }],
        hasMore: false,
        returnedRowCount: 1,
      }),
    );

    component.updateSelectedFields([employeeId]);
    component.updateFilters({ filters: [validFilter], count: 1, valid: true });
    component.openPreview();

    expect(component.previewResult()?.rows).toEqual([{ c0: 42 }]);
    expect(component.previewStale()).toBeFalse();

    component.updateFilters({ filters: [], count: 1, valid: false });

    expect(component.filters()).toEqual([validFilter]);
    expect(component.filtersValid()).toBeFalse();
    expect(component.canPreview()).toBeFalse();
    expect(component.previewResult()?.rows).toEqual([{ c0: 42 }]);
    expect(component.previewStale()).toBeTrue();
    expect(reportPreviewService.preview).toHaveBeenCalledTimes(1);
  });

  it('keeps the previous preview when refreshing it fails with an API problem', () => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];
    reportPreviewService.preview.and.returnValue(
      of({
        columns: [
          { key: 'c0', fieldId: employeeId.id, displayName: 'ID', dataType: 'INTEGER' },
        ],
        rows: [{ c0: 42 }],
        hasMore: false,
        returnedRowCount: 1,
      }),
    );
    component.updateSelectedFields([employeeId]);
    component.openPreview();

    reportPreviewService.preview.and.returnValue(
      throwError(() => new HttpErrorResponse({
        status: 400,
        error: { detail: 'Le filtre est invalide.' },
      })),
    );
    component.openPreview();

    expect(component.previewResult()?.rows).toEqual([{ c0: 42 }]);
    expect(component.previewError()).toBe('Le filtre est invalide.');
    expect(component.previewStale()).toBeTrue();
  });

  it('removes filters and sorts whose selected column is removed', () => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];
    const contractId = component.fieldGroups()[1].fields[0];
    component.updateSelectedFields([employeeId, contractId]);
    component.updateSorts([
      { fieldId: employeeId.id, direction: 'ASC' },
      { fieldId: contractId.id, direction: 'DESC' },
    ]);
    component.updateFilters({
      filters: [{ fieldId: contractId.id, operator: 'EQUALS', values: ['9'] }],
      count: 1,
      valid: true,
    });

    component.updateSelectedFields([employeeId]);

    expect(component.sorts()).toEqual([{ fieldId: employeeId.id, direction: 'ASC' }]);
    expect(component.filters()).toEqual([]);
    expect(component.filterFieldGroups().flatMap((group) => group.fields)).toEqual([
      employeeId,
    ]);
    expect(reportPreviewService.preview).not.toHaveBeenCalled();
  });

  it('removes the page summary and renders one full-width action bar', () => {
    createComponent();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('#summary-title')).toBeNull();
    expect(element.querySelectorAll('[aria-label="Actions du rapport"]').length).toBe(1);
  });

  it('excludes unsupported fields from configuration', () => {
    routeStub.snapshot = {
      paramMap: convertToParamMap({ datasetId: '1' }),
      queryParamMap: convertToParamMap({}),
    };
    datasetService.getDatasetFields.and.returnValue(
      of([
        field(11, 'ID', 'emp_pk_id', 'INTEGER'),
        {
          ...field(99, 'Objet interne', 'internal_object', 'UNSUPPORTED'),
          supported: false,
          supportedOperators: [],
        },
      ]),
    );

    createComponent();

    expect(component.fieldGroups()[0].fields.map((configuredField) => configuredField.id))
      .toEqual([11]);
  });

  it('rejects a related dataset that is not directly connected to the main dataset', () => {
    routeStub.snapshot = {
      paramMap: convertToParamMap({ datasetId: '1' }),
      queryParamMap: convertToParamMap({ relatedDatasetIds: '99' }),
    };

    createComponent();

    expect(component.errorMessage()).toContain('n’est plus directement reliée');
    expect(datasetService.getDatasetFields).not.toHaveBeenCalled();
  });

  it('keeps the current configuration when a reload has an invalid related dataset', () => {
    createComponent();
    const selectedField = component.fieldGroups()[0].fields[0];
    component.updateSelectedFields([selectedField]);
    routeStub.snapshot = {
      paramMap: convertToParamMap({ datasetId: '1' }),
      queryParamMap: convertToParamMap({ relatedDatasetIds: '99' }),
    };

    component.loadConfiguration();

    expect(component.errorMessage()).toContain('n’est plus directement reliée');
    expect(component.selectedDatasets().map((dataset) => dataset.id)).toEqual([1, 2, 3]);
    expect(component.selectedFields()).toEqual([selectedField]);
  });

  it('loads a related dataset when the main dataset is the relation source', () => {
    routeStub.snapshot = {
      paramMap: convertToParamMap({ datasetId: '1' }),
      queryParamMap: convertToParamMap({ relatedDatasetIds: '4' }),
    };
    datasetService.getReportSources.and.returnValue(
      of({
        datasets,
        relations: [
        {
          sourceDatasetId: 1,
          sourceTable: 'rhis_employee',
          sourceDisplayName: 'Employés',
          sourceColumn: 'emp_pk_id',
          targetDatasetId: 4,
          targetTable: 'rhis_absence',
          targetDisplayName: 'Absences',
          targetColumn: 'employee_id',
        },
        ],
      }),
    );

    createComponent();

    expect(component.selectedDatasets().map((dataset) => dataset.id)).toEqual([1, 4]);
    expect(datasetService.getDatasetFields.calls.allArgs()).toEqual([[1], [4]]);
  });

  it('rejects a related dataset connected only by an incoming relation', () => {
    routeStub.snapshot = {
      paramMap: convertToParamMap({ datasetId: '1' }),
      queryParamMap: convertToParamMap({ relatedDatasetIds: '2' }),
    };
    datasetService.getReportSources.and.returnValue(
      of({
        datasets,
        relations: [
        {
          sourceDatasetId: 2,
          sourceTable: 'rhis_contrat',
          sourceDisplayName: 'Contrats',
          sourceColumn: 'emp_fk_id',
          targetDatasetId: 1,
          targetTable: 'rhis_employee',
          targetDisplayName: 'Employés',
          targetColumn: 'emp_pk_id',
        },
        ],
      }),
    );

    createComponent();

    expect(component.errorMessage()).toContain('n’est plus directement reliée');
    expect(datasetService.getDatasetFields).not.toHaveBeenCalled();
  });

  it('rejects a missing main dataset without loading fields', () => {
    routeStub.snapshot = {
      paramMap: convertToParamMap({ datasetId: '99' }),
      queryParamMap: convertToParamMap({}),
    };

    createComponent();

    expect(component.errorMessage()).toContain('source principale est introuvable');
    expect(datasetService.getDatasetFields).not.toHaveBeenCalled();
  });

  it('rejects malformed related dataset ids before making HTTP calls', () => {
    routeStub.snapshot = {
      paramMap: convertToParamMap({ datasetId: '1' }),
      queryParamMap: convertToParamMap({ relatedDatasetIds: '2,abc' }),
    };

    createComponent();

    expect(component.errorMessage()).toContain('URL est invalide');
    expect(datasetService.getDatasetFields).not.toHaveBeenCalled();
  });

  it('rejects a malformed main dataset id before making HTTP calls', () => {
    routeStub.snapshot = {
      paramMap: convertToParamMap({ datasetId: 'invalid' }),
      queryParamMap: convertToParamMap({}),
    };

    createComponent();

    expect(component.errorMessage()).toContain('URL est invalide');
    expect(datasetService.getReportSources).not.toHaveBeenCalled();
    expect(datasetService.getDatasetFields).not.toHaveBeenCalled();
  });

  it('keeps loading active and ignores duplicate loads until report sources complete', () => {
    createComponent();
    const reportSourcesRequest = new Subject<{
      readonly datasets: readonly Dataset[];
      readonly relations: readonly TableRelation[];
    }>();
    datasetService.getReportSources.and.returnValue(reportSourcesRequest);

    component.loadConfiguration();

    expect(component.isLoading()).toBeTrue();
    expect(datasetService.getReportSources).toHaveBeenCalledTimes(2);

    component.loadConfiguration();

    expect(datasetService.getReportSources).toHaveBeenCalledTimes(2);

    reportSourcesRequest.next({ datasets, relations });
    reportSourcesRequest.complete();

    expect(component.isLoading()).toBeFalse();
    expect(component.selectedDatasets().map((dataset) => dataset.id)).toEqual([1, 2, 3]);
  });

  it('clears configuration after an HTTP error and succeeds on retry', () => {
    createComponent();
    component.updateSelectedFields([component.fieldGroups()[0].fields[0]]);
    datasetService.getReportSources.and.returnValue(
      throwError(() => new Error('Datasets unavailable')),
    );

    component.loadConfiguration();

    expect(component.selectedDatasets()).toEqual([]);
    expect(component.fieldGroups()).toEqual([]);
    expect(component.selectedFields()).toEqual([]);
    expect(component.errorMessage()).toContain('Impossible de charger');
    expect(component.isLoading()).toBeFalse();

    datasetService.getReportSources.and.returnValue(of({ datasets, relations }));

    component.loadConfiguration();

    expect(component.selectedDatasets().map((dataset) => dataset.id)).toEqual([1, 2, 3]);
    expect(component.fieldGroups().length).toBe(3);
    expect(component.errorMessage()).toBeNull();
    expect(component.isLoading()).toBeFalse();
  });

  it('resets the report definition and preview after a successful reload', () => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];
    component.updateSelectedFields([employeeId]);
    component.updateFilters({
      filters: [{ fieldId: employeeId.id, operator: 'EQUALS', values: ['42'] }],
      count: 1,
      valid: true,
    });
    component.updateSorts([{ fieldId: employeeId.id, direction: 'ASC' }]);
    component.openPreview();

    component.loadConfiguration();

    expect(component.selectedFields()).toEqual([]);
    expect(component.filters()).toEqual([]);
    expect(component.filtersValid()).toBeTrue();
    expect(component.sorts()).toEqual([]);
    expect(component.previewResult()).toBeNull();
    expect(component.previewError()).toBeNull();
    expect(component.previewStale()).toBeFalse();
  });

  function field(
    id: number,
    displayName: string,
    sourceName: string,
    dataType: DatasetField['dataType'],
  ): DatasetField {
    return {
      id,
      displayName,
      sourceName,
      dataType,
      nullable: false,
      supported: true,
      supportedOperators: ['EQUALS', 'NOT_EQUALS', 'IS_NULL', 'IS_NOT_NULL'],
    };
  }
});
