import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { Dataset } from '../../models/dataset.model';
import { TableRelation } from '../../models/table-relation.model';
import { DatasetService } from '../../services/dataset.service';
import { RapportsComponent } from './rapports.component';

describe('RapportsComponent', () => {
  let fixture: ComponentFixture<RapportsComponent>;
  let component: RapportsComponent;

  const datasets: readonly Dataset[] = [
    { id: 1, displayName: 'Employés', sourceName: 'rhis_employee' },
    { id: 2, displayName: 'Contrats', sourceName: 'rhis_contrat' },
    { id: 3, displayName: 'Pointages', sourceName: 'rhis_pointage' },
    { id: 4, displayName: 'Restaurants', sourceName: 'rhis_restaurant' },
  ];

  const relations: readonly TableRelation[] = [
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
    {
      sourceDatasetId: 2,
      sourceTable: 'rhis_contrat',
      sourceDisplayName: 'Contrats',
      sourceColumn: 'responsable_fk_id',
      targetDatasetId: 1,
      targetTable: 'rhis_employee',
      targetDisplayName: 'Employés',
      targetColumn: 'emp_pk_id',
    },
    {
      sourceDatasetId: 3,
      sourceTable: 'rhis_pointage',
      sourceDisplayName: 'Pointages',
      sourceColumn: 'id_employee',
      targetDatasetId: 1,
      targetTable: 'rhis_employee',
      targetDisplayName: 'Employés',
      targetColumn: 'emp_pk_id',
    },
    {
      sourceDatasetId: 1,
      sourceTable: 'rhis_employee',
      sourceDisplayName: 'Employés',
      sourceColumn: 'manager_fk_id',
      targetDatasetId: 1,
      targetTable: 'rhis_employee',
      targetDisplayName: 'Employés',
      targetColumn: 'emp_pk_id',
    },
    {
      sourceDatasetId: 5,
      sourceTable: 'rhis_detail_evenement',
      sourceDisplayName: 'Détail événement',
      sourceColumn: 'id_restaurant',
      targetDatasetId: 4,
      targetTable: 'rhis_restaurant',
      targetDisplayName: 'Restaurants',
      targetColumn: 'restau_pk_id',
    },
    {
      sourceDatasetId: 6,
      sourceTable: 'inactive_source',
      sourceDisplayName: 'Source inactive',
      sourceColumn: 'target_id',
      targetDatasetId: 7,
      targetTable: 'inactive_target',
      targetDisplayName: 'Cible inactive',
      targetColumn: 'id',
    },
  ];

  const datasetService = {
    getReportSources: jasmine.createSpy().and.returnValue(of({ datasets, relations })),
  };
  const router = {
    navigate: jasmine.createSpy().and.resolveTo(true),
  };

  beforeEach(async () => {
    datasetService.getReportSources.calls.reset();
    datasetService.getReportSources.and.returnValue(of({ datasets, relations }));
    router.navigate.calls.reset();

    await TestBed.configureTestingModule({
      imports: [RapportsComponent],
      providers: [
        provideNoopAnimations(),
        { provide: DatasetService, useValue: datasetService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RapportsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('creates one accordion for each active dataset', () => {
    expect(component.datasetAccordions().length).toBe(datasets.length);
    expect(fixture.nativeElement.querySelectorAll('.dataset-accordion').length).toBe(
      datasets.length,
    );
  });

  it('groups only outgoing relations while ignoring incoming and self relations', () => {
    const contractAccordion = component
      .datasetAccordions()
      .find((item) => item.dataset.sourceName === 'rhis_contrat');
    const employeeAccordion = component
      .datasetAccordions()
      .find((item) => item.dataset.sourceName === 'rhis_employee');

    expect(contractAccordion?.relatedDatasets.map((item) => item.title)).toEqual(['Employés']);
    expect(contractAccordion?.relatedDatasets[0].description).toContain('emp_fk_id');
    expect(contractAccordion?.relatedDatasets[0].description).toContain('responsable_fk_id');
    expect(employeeAccordion?.relatedDatasets).toEqual([]);
  });

  it('does not expose an incoming relation from a dataset absent from the catalog', () => {
    const detailAccordion = component
      .datasetAccordions()
      .find((item) => item.dataset.sourceName === 'rhis_detail_evenement');
    const restaurantAccordion = component
      .datasetAccordions()
      .find((item) => item.dataset.sourceName === 'rhis_restaurant');

    expect(detailAccordion).toBeUndefined();
    expect(restaurantAccordion?.relatedDatasets).toEqual([]);
  });

  it('ignores a relation when neither endpoint is a main dataset', () => {
    expect(
      component.datasetAccordions().some((accordion) =>
        accordion.relatedDatasets.some(
          (relatedDataset) => relatedDataset.sourceName.startsWith('inactive_'),
        ),
      ),
    ).toBeFalse();
  });

  it('opens only one accordion and clears related selections when it changes', () => {
    component.openDataset(1);
    component.toggleRelatedDataset(2, true);
    const previousSelection = component.selectedRelatedDatasetIds();
    expect(component.selectedDatasetId()).toBe(1);
    expect(previousSelection.has(2)).toBeTrue();

    component.openDataset(3);
    expect(component.selectedDatasetId()).toBe(3);
    expect(component.selectedRelatedDatasetIds().size).toBe(0);
    expect(component.selectedRelatedDatasetIds()).not.toBe(previousSelection);

    component.closeDataset(3);
    expect(component.selectedDatasetId()).toBeNull();
  });

  it('keeps related selections when reopening the already selected dataset', () => {
    component.openDataset(1);
    component.toggleRelatedDataset(2, true);

    component.openDataset(1);

    expect(component.selectedDatasetId()).toBe(1);
    expect(component.selectedRelatedDatasetIds()).toEqual(new Set([2]));
  });

  it('summarizes the main table first and selected related tables by business name', () => {
    component.openDataset(2);
    component.toggleRelatedDataset(1, true);
    fixture.detectChanges();

    expect(component.selectedDatasetSummary().map((dataset) => dataset.displayName)).toEqual([
      'Contrats',
      'Employés',
    ]);

    const chips = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('.selected-datasets-summary__chip'),
    ).map((chip) => chip.textContent?.replace(/\s+/g, ' ').trim());
    expect(chips).toEqual(['Contrats', 'Employés']);
  });

  it('shows a non-interactive empty summary when no main table is selected', () => {
    const summary = (fixture.nativeElement as HTMLElement).querySelector(
      '.selected-datasets-summary',
    );

    expect(component.selectedDatasetSummary()).toEqual([]);
    expect(summary?.textContent).toContain('Aucune table sélectionnée.');
    expect(summary?.querySelector('button, input, select')).toBeNull();
  });

  it('keeps aria-expanded synchronized with the selected table', () => {
    component.openDataset(2);
    fixture.detectChanges();

    const selectedAccordion = fixture.nativeElement.querySelector(
      '.dataset-accordion--selected',
    ) as HTMLElement | null;
    const employeeHeader = selectedAccordion?.querySelector('p-accordion-header');

    expect(employeeHeader?.getAttribute('aria-expanded')).toBe('true');
    expect(selectedAccordion?.querySelectorAll('app-report-related-card').length).toBe(1);
  });

  it('keeps the closing accordion full-width while its content is animating', () => {
    component.openDataset(1);
    fixture.detectChanges();

    const accordion = fixture.nativeElement.querySelector(
      '.dataset-accordion--selected',
    ) as HTMLElement;
    const animatedContent = accordion.querySelector(
      '.p-accordioncontent-content',
    ) as HTMLElement;
    const expandedWidth = accordion.getBoundingClientRect().width;
    animatedContent.classList.add('ng-animating');

    component.closeDataset(1);
    fixture.detectChanges();

    expect(component.selectedDatasetId()).toBeNull();
    expect(accordion.classList).not.toContain('dataset-accordion--selected');
    expect(getComputedStyle(accordion).gridColumn).toBe('1 / -1');
    expect(accordion.getBoundingClientRect().width).toBe(expandedWidth);

    animatedContent.classList.remove('ng-animating');

    expect(getComputedStyle(accordion).gridColumn).toBe('auto');
    expect(accordion.getBoundingClientRect().width).toBeLessThan(expandedWidth);
  });

  it('navigates with sorted related dataset ids', () => {
    component.openDataset(1);
    component.toggleRelatedDataset(3, true);
    component.toggleRelatedDataset(2, true);

    component.continueToConfiguration();

    expect(router.navigate).toHaveBeenCalledOnceWith(['/rapports/configuration', 1], {
      queryParams: { relatedDatasetIds: '2,3' },
    });
  });

  it('does not navigate without a selected main dataset', () => {
    component.continueToConfiguration();

    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('does not list a target when the selected dataset is only the relation target', () => {
    const restaurantAccordion = component
      .datasetAccordions()
      .find((item) => item.dataset.id === 4);

    expect(restaurantAccordion?.relatedDatasets).toEqual([]);
  });

  it('keeps loading active and ignores duplicate loads until report sources complete', () => {
    const reportSourcesRequest = new Subject<{
      readonly datasets: readonly Dataset[];
      readonly relations: readonly TableRelation[];
    }>();
    datasetService.getReportSources.and.returnValue(reportSourcesRequest);

    component.loadReportSources();

    expect(component.isLoading()).toBeTrue();
    expect(datasetService.getReportSources).toHaveBeenCalledTimes(2);

    component.loadReportSources();

    expect(datasetService.getReportSources).toHaveBeenCalledTimes(2);

    reportSourcesRequest.next({ datasets, relations });
    reportSourcesRequest.complete();

    expect(component.isLoading()).toBeFalse();
    expect(component.datasets()).toEqual(datasets);
  });

  it('clears report sources after an HTTP error and succeeds on retry', () => {
    component.openDataset(1);
    component.toggleRelatedDataset(2, true);
    datasetService.getReportSources.and.returnValue(
      throwError(() => new Error('Datasets unavailable')),
    );

    component.loadReportSources();

    expect(component.datasets()).toEqual([]);
    expect(component.relations()).toEqual([]);
    expect(component.selectedDatasetId()).toBeNull();
    expect(component.selectedRelatedDatasetIds().size).toBe(0);
    expect(component.errorMessage()).toContain('Impossible de charger');
    expect(component.isLoading()).toBeFalse();

    datasetService.getReportSources.and.returnValue(of({ datasets, relations }));

    component.loadReportSources();

    expect(component.datasets()).toEqual(datasets);
    expect(component.relations()).toEqual(relations);
    expect(component.errorMessage()).toBeNull();
    expect(component.isLoading()).toBeFalse();
  });
});
