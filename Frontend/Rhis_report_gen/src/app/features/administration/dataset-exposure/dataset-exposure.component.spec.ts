import {HttpErrorResponse} from '@angular/common/http';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {MessageService} from 'primeng/api';
import {of, Subject, throwError} from 'rxjs';

import {DatasetExposureComponent} from './dataset-exposure.component';
import {DatasetExposureConfiguration, DatasetExposureMode} from './dataset-exposure.model';
import {DatasetExposureService} from './dataset-exposure.service';

describe('DatasetExposureComponent', () => {
  let fixture: ComponentFixture<DatasetExposureComponent>;
  let component: DatasetExposureComponent;
  let service: jasmine.SpyObj<DatasetExposureService>;
  let messageService: jasmine.SpyObj<MessageService>;

  const configuration: DatasetExposureConfiguration = {
    datasets: [
      {
        id: 1,
        displayName: 'Employés',
        active: true,
        displayMain: true,
        displayRelated: false,
        visibleFieldCount: 1,
        fields: [
          {id: 11, displayName: 'Nom', active: true, visible: true},
          {id: 12, displayName: 'Matricule historique', active: false, visible: true},
          {id: 13, displayName: 'Adresse e-mail', active: true, visible: false},
        ],
      },
      {
        id: 2,
        displayName: 'Départements',
        active: true,
        displayMain: false,
        displayRelated: true,
        visibleFieldCount: 1,
        fields: [
          {id: 21, displayName: 'Libellé', active: true, visible: true},
          {id: 22, displayName: 'Code', active: true, visible: false},
        ],
      },
      {
        id: 3,
        displayName: 'Archives',
        active: false,
        displayMain: true,
        displayRelated: true,
        visibleFieldCount: 0,
        fields: [{id: 31, displayName: 'Ancien identifiant', active: false, visible: true}],
      },
    ],
  };

  beforeEach(async () => {
    service = jasmine.createSpyObj<DatasetExposureService>('DatasetExposureService', [
      'getConfiguration',
      'updateConfiguration',
    ]);
    messageService = jasmine.createSpyObj<MessageService>('MessageService', ['add']);
    service.getConfiguration.and.returnValue(of(configuration));
    service.updateConfiguration.and.returnValue(of(configuration));

    await TestBed.configureTestingModule({
      imports: [DatasetExposureComponent],
      providers: [
        provideNoopAnimations(),
        {provide: DatasetExposureService, useValue: service},
        {provide: MessageService, useValue: messageService},
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DatasetExposureComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('starts with the table list and no selected dataset', () => {
    expect(component.selectedDatasetId()).toBeNull();
    expect(component.selectedDataset()).toBeNull();
    expect(component.mobileDetailVisible()).toBeFalse();
    expect(component.filteredDatasets().length).toBe(3);
  });

  it('selects a dataset without saving or resetting another dataset draft', () => {
    component.updateMode(1, 'RELATED_ONLY');

    component.selectDataset(2);
    component.updateField(2, 22, true);
    component.selectDataset(1);

    expect(component.selectedDataset()?.id).toBe(1);
    expect(component.mobileDetailVisible()).toBeTrue();
    expect(component.exposureMode(component.draft()[0])).toBe('RELATED_ONLY');
    expect(component.draft()[1].fields.find((field) => field.id === 22)?.visible).toBeTrue();
    expect(component.dirtyDatasetIds()).toEqual(new Set([1, 2]));
    expect(service.updateConfiguration).not.toHaveBeenCalled();
  });

  it('searches tables and selected fields independently by display name', () => {
    component.tableSearchTerm.set('départ');
    expect(component.filteredDatasets().map((dataset) => dataset.id)).toEqual([2]);

    component.tableSearchTerm.set('libellé');
    expect(component.filteredDatasets()).toEqual([]);

    component.selectDataset(2);
    component.fieldSearchTerm.set('code');
    expect(component.filteredFields().map((field) => field.id)).toEqual([22]);
    expect(component.filteredDatasets()).toEqual([]);
  });

  it('supports all four exposure modes through the existing conversions', () => {
    const modes: readonly DatasetExposureMode[] = [
      'NONE',
      'MAIN_ONLY',
      'RELATED_ONLY',
      'MAIN_AND_RELATED',
    ];

    for (const mode of modes) {
      component.updateMode(1, mode);
      expect(component.exposureMode(component.draft()[0])).toBe(mode);
    }
  });

  it('preserves field visibility when the table becomes non exposed', () => {
    component.updateMode(1, 'NONE');

    expect(component.draft()[0].fields[0].visible).toBeTrue();
    expect(component.fieldsDisabled(component.draft()[0])).toBeTrue();
    expect(component.dirty()).toBeTrue();
  });

  it('does not update inactive datasets or inactive fields', () => {
    component.updateMode(3, 'NONE');
    component.updateField(1, 12, false);

    expect(component.exposureMode(component.draft()[2])).toBe('MAIN_AND_RELATED');
    expect(component.draft()[0].fields.find((field) => field.id === 12)?.visible).toBeTrue();
    expect(component.fieldsDisabled(component.draft()[2])).toBeTrue();
    expect(component.dirty()).toBeFalse();
  });

  it('resets the complete draft without an HTTP request', () => {
    const loadedConfiguration = structuredClone(configuration);
    service.getConfiguration.and.returnValue(of(loadedConfiguration));
    component.load();
    const originalDatasets = structuredClone(loadedConfiguration.datasets);
    const baseline = component.baseline();
    for (const dataset of baseline) {
      dataset.fields.forEach((field) => Object.freeze(field));
      Object.freeze(dataset.fields);
      Object.freeze(dataset);
    }
    Object.freeze(baseline);
    component.selectDataset(2);
    component.tableSearchTerm.set('départ');
    component.fieldSearchTerm.set('code');
    component.updateMode(1, 'NONE');
    component.updateField(2, 22, true);

    component.resetDraft();

    expect(component.draft()).toEqual(configuration.datasets);
    expect(component.dirty()).toBeFalse();
    expect(component.selectedDatasetId()).toBe(2);
    expect(component.tableSearchTerm()).toBe('départ');
    expect(component.fieldSearchTerm()).toBe('code');
    expect(service.updateConfiguration).not.toHaveBeenCalled();

    component.updateMode(1, 'RELATED_ONLY');
    component.updateField(2, 22, true);

    expect(component.baseline()).toEqual(originalDatasets);
    expect(loadedConfiguration.datasets).toEqual(originalDatasets);
    expect(component.changes()).toEqual([
      {id: 1, displayMain: false, displayRelated: true, fields: []},
      {id: 2, displayMain: false, displayRelated: true, fields: [{id: 22, visible: true}]},
    ]);
  });

  it('sends all deltas on save and installs the returned configuration as baseline', () => {
    const savedConfiguration: DatasetExposureConfiguration = {
      datasets: configuration.datasets.map((dataset) =>
        dataset.id === 1
          ? {...dataset, displayMain: false, displayRelated: true}
          : dataset.id === 2
            ? {
                ...dataset,
                fields: dataset.fields.map((field) =>
                  field.id === 22 ? {...field, visible: true} : field,
                ),
              }
            : dataset,
      ),
    };
    service.updateConfiguration.and.returnValue(of(savedConfiguration));
    component.updateMode(1, 'RELATED_ONLY');
    component.updateField(2, 22, true);

    component.save();

    expect(service.updateConfiguration).toHaveBeenCalledOnceWith({
      datasets: [
        {id: 1, displayMain: false, displayRelated: true, fields: []},
        {id: 2, displayMain: false, displayRelated: true, fields: [{id: 22, visible: true}]},
      ],
    });
    expect(component.baseline()).toEqual(savedConfiguration.datasets);
    expect(component.draft()).toEqual(savedConfiguration.datasets);
    expect(component.dirty()).toBeFalse();
    expect(messageService.add).toHaveBeenCalledOnceWith({
      severity: 'success',
      summary: 'Enregistrement réussi',
      detail: 'La configuration a été enregistrée.',
    });
  });

  it('keeps the draft and shows a functional toast when save fails', () => {
    const technicalDetail = 'org.hibernate.ConstraintViolationException: secret_table';
    const consoleSpy = spyOn(console, 'error');
    service.updateConfiguration.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 500,
            error: {detail: technicalDetail},
          }),
      ),
    );
    component.updateMode(1, 'NONE');

    component.save();

    expect(component.exposureMode(component.draft()[0])).toBe('NONE');
    expect(component.dirty()).toBeTrue();
    expect(messageService.add).toHaveBeenCalledOnceWith({
      severity: 'error',
      summary: 'Échec de l’enregistrement',
      detail: 'Vos modifications sont conservées. Réessayez.',
    });
    expect(JSON.stringify(messageService.add.calls.mostRecent().args[0])).not.toContain(
      technicalDetail,
    );
    expect(consoleSpy).toHaveBeenCalled();
  });

  it('keeps an initial load failure in the page and retries without a toast', () => {
    const consoleSpy = spyOn(console, 'error');
    service.getConfiguration.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 500,
            error: {detail: 'Stack trace interne'},
          }),
      ),
    );

    component.load();

    expect(component.loadError()).toBe('Impossible de charger la configuration des données.');
    expect(messageService.add).not.toHaveBeenCalled();
    expect(consoleSpy).toHaveBeenCalled();

    service.getConfiguration.and.returnValue(of(configuration));
    component.load();
    expect(component.loadError()).toBeNull();
  });

  it('renders the master-detail shell without an automatic selection', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('[data-testid="master-panel"]')).not.toBeNull();
    expect(element.querySelector('[data-testid="detail-panel"]')).not.toBeNull();
    expect(element.textContent).toContain('Sélectionnez une table');
    expect(element.querySelector('p-accordion')).toBeNull();
    expect(element.querySelector('p-toast')).toBeNull();
  });

  it('renders the selected table, its field states and associated labels', () => {
    component.selectDataset(1);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('#dataset-detail-title')?.textContent).toContain('Employés');
    expect(element.querySelector('label[for="table-search"]')).not.toBeNull();
    expect(element.querySelector('label[for="field-search"]')).not.toBeNull();
    expect(element.querySelector('label[for="mode-1"]')).not.toBeNull();
    expect(element.querySelector('label[for="field-11"]')?.textContent).toContain('Exposer Nom');
    expect(element.textContent).toContain('1 champ exposé sur 3');
    expect(element.textContent).toContain('Indisponible');
  });

  it('shows dirty markers and distinct search empty states', () => {
    component.updateMode(1, 'NONE');
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('[data-dataset-id="1"] .dirty-marker')).not.toBeNull();

    component.tableSearchTerm.set('introuvable');
    fixture.detectChanges();
    expect(element.textContent).toContain('Aucune table ne correspond à votre recherche.');

    component.tableSearchTerm.set('');
    component.selectDataset(2);
    component.fieldSearchTerm.set('introuvable');
    fixture.detectChanges();
    expect(element.textContent).toContain('Aucun champ ne correspond à votre recherche.');
  });

  it('disables both draft actions while saving', () => {
    const response = new Subject<DatasetExposureConfiguration>();
    service.updateConfiguration.and.returnValue(response);
    component.updateMode(1, 'NONE');

    component.save();
    fixture.detectChanges();

    const actionBar = fixture.nativeElement.querySelector('[data-testid="action-bar"]');
    const buttons = Array.from(actionBar.querySelectorAll('button')) as HTMLButtonElement[];
    expect(actionBar.getAttribute('aria-busy')).toBe('true');
    expect(buttons.every((button) => button.disabled)).toBeTrue();

    response.next(configuration);
    response.complete();
  });

  it('moves focus into and out of the mobile detail view', fakeAsync(() => {
    if (!window.matchMedia('(max-width: 63.999rem)').matches) {
      pending('The Karma viewport is not using the mobile breakpoint.');
    }
    const element = fixture.nativeElement as HTMLElement;
    const tableButton = element.querySelector('[data-dataset-id="2"]') as HTMLButtonElement;

    tableButton.click();
    fixture.detectChanges();
    tick();
    expect(document.activeElement).toBe(element.querySelector('#dataset-detail-title'));

    const backButton = element.querySelector(
      '[data-testid="back-to-tables"] button',
    ) as HTMLButtonElement;
    backButton.click();
    fixture.detectChanges();
    tick();
    expect(document.activeElement).toBe(element.querySelector('[data-dataset-id="2"]'));
  }));

  it('protects browser unload only while the draft is dirty', () => {
    const cleanEvent = beforeUnloadEvent();
    component.handleBeforeUnload(cleanEvent);
    expect(cleanEvent.preventDefault).not.toHaveBeenCalled();

    component.updateMode(1, 'NONE');
    const dirtyEvent = beforeUnloadEvent();
    component.handleBeforeUnload(dirtyEvent);

    expect(dirtyEvent.preventDefault).toHaveBeenCalled();
    expect(dirtyEvent.returnValue).toBe('');
  });

  it('only renders the action bar while values differ, including reverting each value', fakeAsync(() => {
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('[data-testid="action-bar"]')).toBeNull();
    component.updateMode(1, 'RELATED_ONLY');
    component.updateField(1, 13, true);
    fixture.detectChanges();
    expect(element.querySelector('[data-testid="action-bar"]')).not.toBeNull();
    component.updateMode(1, 'MAIN_ONLY');
    fixture.detectChanges();
    expect(component.dirty()).toBeTrue();
    component.updateField(1, 13, false);
    fixture.detectChanges();
    tick(250);
    expect(component.dirty()).toBeFalse();
    expect(element.querySelector('[data-testid="action-bar"]:not([aria-hidden="true"])')).toBeNull();
  }));

  it('clears the bar on cancel and success, using the saved response for later comparisons', fakeAsync(() => {
    const element = fixture.nativeElement as HTMLElement;
    component.updateField(1, 13, true);
    fixture.detectChanges();
    component.resetDraft();
    fixture.detectChanges();
    tick(250);
    expect(element.querySelector('[data-testid="action-bar"]:not([aria-hidden="true"])')).toBeNull();
    component.updateField(1, 13, true);
    service.updateConfiguration.and.returnValue(of({datasets: component.draft()}));
    fixture.detectChanges();
    component.save();
    fixture.detectChanges();
    tick(250);
    expect(component.dirty()).toBeFalse();
    expect(element.querySelector('[data-testid="action-bar"]:not([aria-hidden="true"])')).toBeNull();
    component.updateField(1, 13, false);
    expect(component.dirty()).toBeTrue();
    component.updateField(1, 13, true);
    expect(component.dirty()).toBeFalse();
  }));

  it('keeps a persistent functional error and draft until a successful retry', fakeAsync(() => {
    spyOn(console, 'error');
    service.updateConfiguration.and.returnValue(throwError(() => new HttpErrorResponse({status: 500})));
    component.updateField(1, 13, true);
    component.save();
    fixture.detectChanges();
    tick(10000);
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('[data-testid="action-bar"]')?.textContent).toContain('Vos modifications sont conservées');
    expect(component.dirty()).toBeTrue();
    service.updateConfiguration.and.returnValue(of({datasets: component.draft()}));
    component.save();
    fixture.detectChanges();
    tick(250);
    expect(component.dirty()).toBeFalse();
    expect(element.querySelector('[data-testid="action-bar"]:not([aria-hidden="true"])')).toBeNull();
  }));

  it('names the mode combobox from its visible associated label', () => {
    component.selectDataset(1);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('[role="combobox"]')?.getAttribute('aria-labelledby')).toBe('mode-label-1');
  });

  it('restores focus after an asynchronous save removes the action buttons', fakeAsync(() => {
    const response = new Subject<DatasetExposureConfiguration>();
    service.updateConfiguration.and.returnValue(response);
    component.selectDataset(1);
    component.updateField(1, 13, true);
    fixture.detectChanges();
    tick();
    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector('[data-testid="action-bar"] p-button:last-child button') as HTMLButtonElement;
    button.focus();
    button.click();
    fixture.detectChanges();
    tick();
    response.next({datasets: component.draft()});
    response.complete();
    fixture.detectChanges();
    tick(250);
    expect(document.activeElement).toBe(element.querySelector('#dataset-detail-title'));
    expect(component.dirty()).toBeFalse();
  }));

  it('does not bring back a previous save error after reverting and starting a new draft', () => {
    spyOn(console, 'error');
    service.updateConfiguration.and.returnValue(throwError(() => new HttpErrorResponse({status: 500})));
    component.updateField(1, 13, true);
    component.save();
    expect(component.saveError()).not.toBeNull();
    component.updateField(1, 13, false);
    expect(component.dirty()).toBeFalse();
    component.updateField(1, 13, true);
    expect(component.saveError()).toBeNull();
  });

  function beforeUnloadEvent(): BeforeUnloadEvent & {
    preventDefault: jasmine.Spy;
    returnValue: string;
  } {
    return {
      preventDefault: jasmine.createSpy('preventDefault'),
      returnValue: 'unchanged',
    } as unknown as BeforeUnloadEvent & {
      preventDefault: jasmine.Spy;
      returnValue: string;
    };
  }
});
