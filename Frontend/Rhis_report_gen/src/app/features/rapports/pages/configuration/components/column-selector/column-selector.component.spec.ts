import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DatasetFieldGroup, ReportField } from '../../configuration.models';
import { ColumnSelectorComponent } from './column-selector.component';

describe('ColumnSelectorComponent', () => {
  let fixture: ComponentFixture<ColumnSelectorComponent>;
  let component: ColumnSelectorComponent;

  const employeeId: ReportField = {
    id: 11,
    key: '1:11',
    datasetId: 1,
    datasetDisplayName: 'Employés',
    displayName: 'ID',
    sourceName: 'emp_pk_id',
    dataType: 'INTEGER',
    nullable: false,
    supported: true,
    supportedOperators: ['EQUALS', 'NOT_EQUALS'],
  };
  const employeeName: ReportField = {
    id: 12,
    key: '1:12',
    datasetId: 1,
    datasetDisplayName: 'Employés',
    displayName: 'Nom',
    sourceName: 'nom',
    dataType: 'TEXT',
    nullable: false,
    supported: true,
    supportedOperators: ['EQUALS', 'CONTAINS'],
  };
  const contractId: ReportField = {
    id: 21,
    key: '2:21',
    datasetId: 2,
    datasetDisplayName: 'Contrats',
    displayName: 'ID',
    sourceName: 'cont_pk_id',
    dataType: 'INTEGER',
    nullable: false,
    supported: true,
    supportedOperators: ['EQUALS', 'NOT_EQUALS'],
  };
  const fieldGroups: readonly DatasetFieldGroup[] = [
    {
      dataset: {
        id: 1,
        displayName: 'Employés',
        sourceName: 'rhis_employee',
        main: true,
      },
      fields: [employeeId, employeeName],
    },
    {
      dataset: {
        id: 2,
        displayName: 'Contrats',
        sourceName: 'rhis_contrat',
        main: false,
      },
      fields: [contractId],
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ColumnSelectorComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(ColumnSelectorComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('fieldGroups', fieldGroups);
    fixture.componentRef.setInput('selectedFields', []);
    fixture.detectChanges();
  });

  it('filters fields by field and dataset names', () => {
    component.searchTerm.set('contrats');

    expect(component.availableFieldGroups()[0].fields).toEqual([]);
    expect(component.availableFieldGroups()[1].fields).toEqual([contractId]);

    component.searchTerm.set('nom');
    expect(component.availableFieldGroups()[0].fields).toEqual([employeeName]);
  });

  it('does not search or render hidden SQL source names', () => {
    component.searchTerm.set('rhis_contrat');
    fixture.detectChanges();

    expect(component.availableFieldGroups().flatMap((group) => group.fields)).toEqual([]);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('emp_pk_id');
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('cont_pk_id');
  });

  it('emits an immutable addition and prevents a duplicate already selected', () => {
    const emitSpy = spyOn(component.selectedFieldsChange, 'emit');

    component.addField(contractId);
    expect(emitSpy).toHaveBeenCalledOnceWith([contractId]);

    fixture.componentRef.setInput('selectedFields', [contractId]);
    fixture.detectChanges();
    component.addField(contractId);
    expect(emitSpy).toHaveBeenCalledTimes(1);
  });

  it('removes a field without mutating the input selection', () => {
    const initialSelection = [employeeId, contractId] as const;
    fixture.componentRef.setInput('selectedFields', initialSelection);
    fixture.detectChanges();
    const emitSpy = spyOn(component.selectedFieldsChange, 'emit');

    component.removeField(employeeId);

    expect(emitSpy).toHaveBeenCalledOnceWith([contractId]);
    expect(initialSelection).toEqual([employeeId, contractId]);
  });

  it('reorders fields with keyboard commands and drag-and-drop', () => {
    fixture.componentRef.setInput('selectedFields', [employeeId, employeeName, contractId]);
    fixture.detectChanges();
    const emitSpy = spyOn(component.selectedFieldsChange, 'emit');

    component.moveField(employeeName, -1);
    expect(emitSpy).toHaveBeenCalledWith([employeeName, employeeId, contractId]);

    component.dropSelectedField({ previousIndex: 2, currentIndex: 0 } as CdkDragDrop<
      readonly ReportField[]
    >);
    expect(emitSpy).toHaveBeenCalledWith([contractId, employeeId, employeeName]);
  });

  it('shows the empty selection state', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Ajoutez les champs à inclure dans votre rapport.');
  });
});
