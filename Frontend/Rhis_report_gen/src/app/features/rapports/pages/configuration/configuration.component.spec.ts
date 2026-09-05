import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { Observable, of, Subject, throwError } from 'rxjs';

import { Dataset } from '../../models/dataset.model';
import { DatasetField } from '../../models/dataset-field.model';
import { TableRelation } from '../../models/table-relation.model';
import { DatasetService } from '../../services/dataset.service';
import { ReportPreviewService } from '../../services/report-preview.service';
import { ReportGenerationService } from '../../services/report-generation.service';
import { ReportDraftStorageService } from '../../services/report-draft-storage.service';
import { ConfigurationComponent } from './configuration.component';
import { FilterEditorComponent } from './components/filter-editor/filter-editor.component';

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

  it('automatically previews one restored valid draft after configuration loading', fakeAsync(() => {
    reportDraftStorage.load.and.returnValue({
      version: 1,
      relatedDatasetIds: [2, 3],
      definition: {
        rootDatasetId: 1,
        selectedFieldIds: [11],
        filters: [{ fieldId: 11, operator: 'EQUALS', values: ['42'] }],
        sorts: [{ fieldId: 11, direction: 'ASC' }],
      },
    });

    createComponent();
    tick(300);

    expect(reportPreviewService.preview).toHaveBeenCalledOnceWith({
      rootDatasetId: 1,
      selectedFieldIds: [11],
      filters: [{ fieldId: 11, operator: 'EQUALS', values: ['42'] }],
      sorts: [{ fieldId: 11, direction: 'ASC' }],
    });
  }));

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

  it('generates from the current definition while a newer preview is waiting', fakeAsync(() => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];
    component.updateSelectedFields([employeeId]);
    tick(300);
    component.updateFilters({
      filters: [{ fieldId: employeeId.id, operator: 'EQUALS', values: ['42'] }],
      count: 1,
      valid: true,
    });

    component.continueToExport();
    tick(300);

    expect(reportGenerationService.startReportGeneration).toHaveBeenCalledOnceWith(
      jasmine.objectContaining({
        selectedFieldIds: [11],
        filters: [{ fieldId: 11, operator: 'EQUALS', values: ['42'] }],
      }),
      jasmine.any(String),
    );
    expect(reportPreviewService.preview).toHaveBeenCalledTimes(1);
    expect(navigateSpy).toHaveBeenCalledOnceWith(['/rapports/export', 'generation-id']);
  }));

  it('resumes automatic preview after generation fails', fakeAsync(() => {
    createComponent();
    component.updateSelectedFields([component.fieldGroups()[0].fields[0]]);
    tick(300);
    reportGenerationService.startReportGeneration.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );

    component.continueToExport();
    tick(300);

    expect(component.generationError()).toContain('Impossible de démarrer');
    expect(reportPreviewService.preview).toHaveBeenCalledTimes(2);
    expect(navigateSpy).not.toHaveBeenCalled();
  }));

  it('builds the ordered preview request from columns, filters and sorts', fakeAsync(() => {
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

    tick(300);

    expect(reportPreviewService.preview).toHaveBeenCalledOnceWith({
      rootDatasetId: 1,
      selectedFieldIds: [21, 11],
      filters: [{ fieldId: 11, operator: 'EQUALS', values: ['42'] }],
      sorts: [
        { fieldId: 21, direction: 'DESC' },
        { fieldId: 11, direction: 'ASC' },
      ],
    });
    expect(component.previewResult()?.returnedRowCount).toBe(0);
  }));

  it('automatically previews only the last valid definition after the debounce', fakeAsync(() => {
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

    tick(299);
    expect(reportPreviewService.preview).not.toHaveBeenCalled();

    tick(1);
    expect(reportPreviewService.preview).toHaveBeenCalledOnceWith({
      rootDatasetId: 1,
      selectedFieldIds: [11],
      filters: [],
      sorts: [{ fieldId: 11, direction: 'ASC' }],
    });
  }));

  it('keeps the last valid filters and preview while an invalid draft is corrected', fakeAsync(() => {
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
    tick(300);

    expect(component.previewResult()?.rows).toEqual([{ c0: 42 }]);
    expect(component.previewStale()).toBeFalse();

    component.updateFilters({ filters: [], count: 1, valid: false });

    expect(component.filters()).toEqual([validFilter]);
    expect(component.filtersValid()).toBeFalse();
    expect(component.canRetryPreview()).toBeFalse();
    expect(component.previewResult()?.rows).toEqual([{ c0: 42 }]);
    expect(component.previewStale()).toBeTrue();
    expect(reportPreviewService.preview).toHaveBeenCalledTimes(1);
  }));

  it('keeps the previous preview when refreshing fails and hides the technical detail', fakeAsync(() => {
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
    tick(300);

    reportPreviewService.preview.and.returnValue(
      throwError(() => new HttpErrorResponse({
        status: 500,
        error: { detail: 'internal stack trace' },
      })),
    );
    component.updateSorts([{ fieldId: employeeId.id, direction: 'ASC' }]);
    tick(300);

    expect(component.previewResult()?.rows).toEqual([{ c0: 42 }]);
    expect(component.previewError()).toBe(
      'Impossible de charger l’aperçu du rapport. Réessayez dans quelques instants.',
    );
    expect(component.previewStale()).toBeTrue();
  }));

  it('cancels an obsolete request so its response cannot replace the latest preview', fakeAsync(() => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];
    const obsoleteResponse = new Subject<{
      columns: readonly [];
      rows: readonly { c0: number }[];
      hasMore: boolean;
      returnedRowCount: number;
    }>();
    let obsoleteRequestCancelled = false;
    const latestResponse = {
      columns: [],
      rows: [{ c0: 2 }],
      hasMore: false,
      returnedRowCount: 1,
    };
    reportPreviewService.preview.and.returnValues(
      new Observable((subscriber) => {
        const subscription = obsoleteResponse.subscribe(subscriber);
        return () => {
          obsoleteRequestCancelled = true;
          subscription.unsubscribe();
        };
      }),
      of(latestResponse),
    );

    component.updateSelectedFields([employeeId]);
    tick(300);
    component.updateSorts([{ fieldId: employeeId.id, direction: 'ASC' }]);

    expect(obsoleteRequestCancelled).toBeTrue();

    obsoleteResponse.next({ columns: [], rows: [{ c0: 1 }], hasMore: false, returnedRowCount: 1 });
    tick(300);

    expect(component.previewResult()).toEqual(latestResponse);
    expect(reportPreviewService.preview).toHaveBeenCalledTimes(2);
  }));

  it('cancels an in-flight request when the current filter becomes invalid', fakeAsync(() => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];
    const response = new Subject<{
      columns: readonly [];
      rows: readonly { c0: number }[];
      hasMore: boolean;
      returnedRowCount: number;
    }>();
    let cancelled = false;
    reportPreviewService.preview.and.returnValue(new Observable((subscriber) => {
      const subscription = response.subscribe(subscriber);
      return () => {
        cancelled = true;
        subscription.unsubscribe();
      };
    }));

    component.updateSelectedFields([employeeId]);
    tick(300);
    component.updateFilters({ filters: [], count: 1, valid: false });
    response.next({ columns: [], rows: [{ c0: 1 }], hasMore: false, returnedRowCount: 1 });

    expect(cancelled).toBeTrue();
    expect(component.previewLoading()).toBeFalse();
    expect(component.previewResult()).toBeNull();
    expect(reportPreviewService.preview).toHaveBeenCalledTimes(1);
  }));

  it('retries the last valid request immediately after an error', fakeAsync(() => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];
    reportPreviewService.preview.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 504 })),
    );
    component.updateSelectedFields([employeeId]);
    tick(300);

    expect(component.previewError()).toContain('trop de temps');

    reportPreviewService.preview.and.returnValue(
      of({ columns: [], rows: [], hasMore: false, returnedRowCount: 0 }),
    );
    component.retryPreview();

    expect(reportPreviewService.preview).toHaveBeenCalledTimes(2);
    expect(component.previewError()).toBeNull();
  }));

  it('does not request a preview without columns, with an invalid filter, or for an identical definition', fakeAsync(() => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];

    tick(300);
    component.updateSelectedFields([employeeId]);
    component.updateFilters({ filters: [], count: 1, valid: false });
    tick(300);
    expect(reportPreviewService.preview).not.toHaveBeenCalled();

    component.updateFilters({ filters: [], count: 0, valid: true });
    tick(300);
    component.updateSelectedFields([employeeId]);
    tick(300);

    expect(reportPreviewService.preview).toHaveBeenCalledTimes(1);
  }));

  it('allows the same valid definition again after an invalid intermediate state', fakeAsync(() => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];
    component.updateSelectedFields([employeeId]);
    tick(300);
    component.updateFilters({ filters: [], count: 1, valid: false });
    component.updateFilters({ filters: [], count: 0, valid: true });
    tick(300);

    expect(reportPreviewService.preview).toHaveBeenCalledTimes(2);
  }));

  it('removes filters and sorts whose selected column is removed', fakeAsync(() => {
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
    tick(300);
    expect(reportPreviewService.preview).toHaveBeenCalledOnceWith({
      rootDatasetId: 1,
      selectedFieldIds: [11],
      filters: [],
      sorts: [{ fieldId: 11, direction: 'ASC' }],
    });
  }));

  it('removes the page summary and renders one full-width action bar', () => {
    createComponent();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('#summary-title')).toBeNull();
    expect(element.querySelectorAll('[aria-label="Actions du rapport"]').length).toBe(1);
  });

  it('renders the report workflow in the existing page content', () => {
    createComponent();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('app-report-steps')).not.toBeNull();
    expect(element.querySelector('app-column-selector')).not.toBeNull();
    expect(element.querySelector('app-filter-editor')).not.toBeNull();
    expect(element.querySelector('app-sort-editor')).not.toBeNull();
    expect(element.querySelector('app-preview-panel')).not.toBeNull();
  });

  it('removes the manual preview action and dialog while mounting one preview panel', () => {
    createComponent();
    const element = fixture.nativeElement as HTMLElement;
    const actionLabels = Array.from(
      element.querySelectorAll<HTMLElement>('[aria-label="Actions du rapport"] button'),
    ).map((button) => button.textContent?.trim());

    expect(actionLabels).toEqual(['Suivant']);
    expect(element.querySelector('p-dialog')).toBeNull();
    expect(element.querySelectorAll('app-preview-panel').length).toBe(1);
  });

  it('starts expanded and collapses the preview through a native button without another request', () => {
    createComponent();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector<HTMLButtonElement>(
      'button[aria-controls="report-preview-body"]',
    );

    expect(component.previewCollapsed()).toBeFalse();
    expect(button).not.toBeNull();
    expect(button?.getAttribute('aria-expanded')).toBe('true');
    expect(reportPreviewService.preview).not.toHaveBeenCalled();

    button?.click();
    fixture.detectChanges();

    expect(component.previewCollapsed()).toBeTrue();
    expect(button?.getAttribute('aria-expanded')).toBe('false');
    expect(element.querySelector('#report-preview-body')?.classList)
      .not.toContain('preview-content--expanded');
    expect(reportPreviewService.preview).not.toHaveBeenCalled();
  });

  it('switches mobile tabs without resetting state or requesting another preview', fakeAsync(() => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];
    component.updateSelectedFields([employeeId]);
    tick(300);

    component.selectMobileTab('preview');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(component.selectedFields()).toEqual([employeeId]);
    expect(element.querySelector('#configuration-panel')?.classList).toContain('mobile-hidden');
    expect(element.querySelector('#preview-panel')?.classList).not.toContain('mobile-hidden');
    expect(element.querySelector('#preview-tab')?.getAttribute('aria-selected')).toBe('true');
    expect(reportPreviewService.preview).toHaveBeenCalledTimes(1);

    component.selectMobileTab('configuration');
    fixture.detectChanges();
    expect(component.selectedFields()).toEqual([employeeId]);
    expect(reportPreviewService.preview).toHaveBeenCalledTimes(1);
  }));

  it('keeps the same invalid FilterEditor row across tabs and breakpoint changes', () => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];
    component.updateSelectedFields([employeeId]);
    fixture.detectChanges();
    const editor = fixture.debugElement.query(By.directive(FilterEditorComponent))
      .componentInstance as FilterEditorComponent;
    editor.startAddingFilter();
    editor.pendingFieldId.setValue(employeeId.id);
    editor.confirmPendingField();
    editor.rows.at(0).controls.value1.markAsTouched();

    const row = editor.rows.at(0);
    expect(row.invalid).toBeTrue();
    expect(row.controls.value1.touched).toBeTrue();

    component.selectMobileTab('preview');
    component.isMobile.set(false);
    component.isMobile.set(true);
    component.selectMobileTab('configuration');
    fixture.detectChanges();

    const sameEditor = fixture.debugElement.query(By.directive(FilterEditorComponent))
      .componentInstance as FilterEditorComponent;
    expect(sameEditor).toBe(editor);
    expect(sameEditor.rows.at(0)).toBe(row);
    expect(row.invalid).toBeTrue();
    expect(row.controls.value1.touched).toBeTrue();
  });

  it('supports cyclic arrow navigation and Home/End in the mobile tab list', () => {
    createComponent();
    const element = fixture.nativeElement as HTMLElement;
    const configurationTab = element.querySelector<HTMLButtonElement>('#configuration-tab')!;
    const previewTab = element.querySelector<HTMLButtonElement>('#preview-tab')!;

    configurationTab.focus();
    const previewFocus = spyOn(previewTab, 'focus');
    const configurationFocus = spyOn(configurationTab, 'focus');
    configurationTab.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }));
    fixture.detectChanges();
    expect(component.mobileTab()).toBe('preview');
    expect(previewFocus).toHaveBeenCalled();

    previewTab.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }));
    fixture.detectChanges();
    expect(component.mobileTab()).toBe('configuration');
    expect(configurationFocus).toHaveBeenCalled();

    configurationTab.dispatchEvent(new KeyboardEvent('keydown', { key: 'End' }));
    expect(component.mobileTab()).toBe('preview');
    previewTab.dispatchEvent(new KeyboardEvent('keydown', { key: 'Home' }));
    expect(component.mobileTab()).toBe('configuration');
  });

  it('keeps automatic updates active while the desktop panel is collapsed', fakeAsync(() => {
    createComponent();
    component.togglePreview();
    component.updateSelectedFields([component.fieldGroups()[0].fields[0]]);
    tick(300);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(component.previewResult()?.returnedRowCount).toBe(0);
    expect(element.querySelector('app-preview-panel')?.classList).toContain('preview-collapsed');
    expect(element.querySelector('#report-preview-body')?.classList)
      .not.toContain('preview-content--expanded');
    expect(element.textContent).toContain('À jour');
  }));

  it('tracks the responsive breakpoint and removes its listener on destroy', () => {
    let changeListener: ((event: MediaQueryListEvent) => void) | undefined;
    const mediaQuery = {
      matches: false,
      media: '(max-width: 47.999rem)',
      onchange: null,
      addListener: jasmine.createSpy(),
      removeListener: jasmine.createSpy(),
      addEventListener: jasmine.createSpy().and.callFake(
        (_type: string, listener: (event: MediaQueryListEvent) => void) => {
          changeListener = listener;
        },
      ),
      removeEventListener: jasmine.createSpy(),
      dispatchEvent: jasmine.createSpy(),
    } as unknown as MediaQueryList;
    spyOn(globalThis, 'matchMedia').and.returnValue(mediaQuery);

    createComponent();
    expect(component.isMobile()).toBeFalse();
    expect((fixture.nativeElement as HTMLElement).querySelector('#configuration-panel')?.getAttribute('role'))
      .toBeNull();

    changeListener?.({ matches: true } as MediaQueryListEvent);
    fixture.detectChanges();
    expect(component.isMobile()).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).querySelector('#configuration-panel')?.getAttribute('role'))
      .toBe('tabpanel');

    fixture.destroy();
    expect(mediaQuery.removeEventListener).toHaveBeenCalledWith(
      'change',
      jasmine.any(Function),
    );
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

  it('resets the report definition and preview after a successful reload', fakeAsync(() => {
    createComponent();
    const employeeId = component.fieldGroups()[0].fields[0];
    component.updateSelectedFields([employeeId]);
    component.updateFilters({
      filters: [{ fieldId: employeeId.id, operator: 'EQUALS', values: ['42'] }],
      count: 1,
      valid: true,
    });
    component.updateSorts([{ fieldId: employeeId.id, direction: 'ASC' }]);
    tick(300);

    component.loadConfiguration();

    expect(component.selectedFields()).toEqual([]);
    expect(component.filters()).toEqual([]);
    expect(component.filtersValid()).toBeTrue();
    expect(component.sorts()).toEqual([]);
    expect(component.previewResult()).toBeNull();
    expect(component.previewError()).toBeNull();
    expect(component.previewStale()).toBeFalse();
  }));

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
