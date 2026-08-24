import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {of} from 'rxjs';

import {DatasetExposureConfiguration} from './dataset-exposure.model';
import {DatasetExposureComponent} from './dataset-exposure.component';
import {DatasetExposureService} from './dataset-exposure.service';

describe('DatasetExposureComponent', () => {
  let fixture: ComponentFixture<DatasetExposureComponent>;
  let component: DatasetExposureComponent;
  let service: jasmine.SpyObj<DatasetExposureService>;

  const configuration: DatasetExposureConfiguration = {
    datasets: [
      {
        id: 1,
        displayName: 'Employés',
        active: true,
        displayMain: true,
        displayRelated: false,
        visibleFieldCount: 1,
        fields: [{id: 11, displayName: 'Nom', active: true, visible: true}],
      },
    ],
  };

  beforeEach(async () => {
    service = jasmine.createSpyObj<DatasetExposureService>('DatasetExposureService', [
      'getConfiguration',
      'updateConfiguration',
    ]);
    service.getConfiguration.and.returnValue(of(configuration));
    service.updateConfiguration.and.returnValue(of(configuration));

    await TestBed.configureTestingModule({
      imports: [DatasetExposureComponent],
      providers: [provideNoopAnimations(), {provide: DatasetExposureService, useValue: service}],
    }).compileComponents();

    fixture = TestBed.createComponent(DatasetExposureComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('preserves field visibility when the table becomes non exposed', () => {
    component.updateMode(1, 'NONE');

    expect(component.draft()[0].fields[0].visible).toBeTrue();
    expect(component.fieldsDisabled(component.draft()[0])).toBeTrue();
    expect(component.dirty()).toBeTrue();
  });

  it('searches only display names', () => {
    component.searchTerm.set('nom');
    expect(component.filteredDatasets()[0].fields[0].displayName).toBe('Nom');

    component.searchTerm.set('technical_name');
    expect(component.filteredDatasets()).toEqual([]);
  });

  it('sends one delta request on explicit save', () => {
    component.updateMode(1, 'RELATED_ONLY');
    expect(service.updateConfiguration).not.toHaveBeenCalled();

    component.save();

    expect(service.updateConfiguration).toHaveBeenCalledOnceWith({
      datasets: [
        {id: 1, displayMain: false, displayRelated: true, fields: []},
      ],
    });
  });
});
