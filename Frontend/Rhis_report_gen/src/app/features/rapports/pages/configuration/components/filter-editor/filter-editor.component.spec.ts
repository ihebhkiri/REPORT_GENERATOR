import { ComponentFixture, fakeAsync, flushMicrotasks, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';

import { DatasetFieldGroup, ReportField } from '../../configuration.models';
import { FilterEditorComponent, FilterEditorState } from './filter-editor.component';

describe('FilterEditorComponent', () => {
  let fixture: ComponentFixture<FilterEditorComponent>;
  let component: FilterEditorComponent;

  const textField = field(11, 'Nom', 'nom', 'TEXT', [
    'EQUALS',
    'CONTAINS',
    'IS_NULL',
  ]);
  const dateField = field(12, 'Date', 'date_journee', 'DATE', [
    'EQUALS',
    'BETWEEN',
  ]);
  const integerField = field(13, 'Effectif', 'effectif', 'INTEGER', ['EQUALS']);
  const offsetDateTimeField = field(
    14,
    'Début planifié',
    'debut_planifie',
    'OFFSET_DATE_TIME',
    ['EQUALS'],
  );
  const uuidField = field(15, 'Identifiant', 'uuid', 'UUID', ['EQUALS']);
  const fallbackField = field(16, 'Ancienneté', 'anciennete', 'INTEGER', [
    'NOT_EQUALS',
    'GREATER_THAN',
  ]);
  const timeField = field(17, 'Heure de début', 'heure_debut', 'TIME', [
    'EQUALS',
    'BETWEEN',
  ]);
  const hiddenOperatorField = field(18, 'Interne', 'interne', 'TEXT', ['NOT_EQUALS']);

  const fieldGroups: readonly DatasetFieldGroup[] = [
    {
      dataset: { id: 1, displayName: 'Shift', sourceName: 'rhis_shift', main: true },
      fields: [
        textField,
        dateField,
        integerField,
        offsetDateTimeField,
        uuidField,
        fallbackField,
        timeField,
        hiddenOperatorField,
      ],
    },
    {
      dataset: { id: 2, displayName: 'Contrat', sourceName: 'rhis_contrat', main: false },
      fields: [field(21, 'Type', 'type_contrat', 'TEXT', ['EQUALS', 'CONTAINS'])],
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FilterEditorComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(FilterEditorComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('fieldGroups', fieldGroups);
    fixture.detectChanges();
  });

  it('shows a quiet empty state without creating a form or an error', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(component.rows.length).toBe(0);
    expect(element.querySelector('form')).toBeNull();
    expect(element.textContent).toContain('Aucun filtre appliqué');
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });

  it('opens the grouped searchable field picker without creating a draft row', () => {
    component.startAddingFilter();
    fixture.detectChanges();

    expect(component.addingFilter()).toBeTrue();
    expect(component.rows.length).toBe(0);
    expect(component.filterFieldGroups().map((group) => group.label)).toEqual([
      'Shift',
      'Contrat',
    ]);
    expect(fixture.nativeElement.querySelector('#new-filter-field')).not.toBeNull();
    expect(component.filterFields().some((option) => option.value === 18)).toBeFalse();
  });

  it('creates a guided sentence and suggests CONTAINS for a text field', fakeAsync(() => {
    selectField(11);
    flushMicrotasks();

    const element = fixture.nativeElement as HTMLElement;
    expect(component.rows.length).toBe(1);
    expect(component.rows.at(0).controls.operator.value).toBe('CONTAINS');
    expect(element.textContent).toContain('Afficher les résultats où');
    expect(document.activeElement?.id).toBe('filter-value-1-0');
  }));

  it('renders each filter as one compound expression in control order', () => {
    selectField(11);
    component.rows.at(0).controls.value1.setValue('CDI');
    selectField(21);
    component.rows.at(1).controls.value1.setValue('Temps plein');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const expressionList = element.querySelector('.filter-expression-list');
    const conditions = expressionList?.querySelectorAll('.filter-condition');
    const firstSegments = conditions?.item(0).querySelectorAll(
      '.field-control, .operator-control, .value-segment, .delete-control',
    );

    expect(expressionList).not.toBeNull();
    expect(conditions?.length).toBe(2);
    expect(expressionList?.querySelector('.filter-conjunction')?.textContent?.trim()).toBe('ET');
    expect(expressionList?.querySelector('.and-separator')).toBeNull();
    const segmentRoles = ['field-control', 'operator-control', 'value-segment', 'delete-control'];
    expect(
      Array.from(firstSegments ?? []).map((segment) =>
        segmentRoles.find((role) => segment.classList.contains(role)),
      ),
    ).toEqual([
      'field-control',
      'operator-control',
      'value-segment',
      'delete-control',
    ]);
    expect(
      conditions?.item(0).querySelector<HTMLInputElement>('.filter-value-input')?.name,
    ).toBe('filter-value-1-0');

    const rowSelects = fixture.debugElement
      .queryAll(By.directive(Select))
      .filter((select) => select.nativeElement.closest('.filter-condition-controls'))
      .map((select) => select.componentInstance as Select);

    expect(rowSelects.map((select) => select.appendTo())).toEqual([
      'body',
      'body',
      'body',
      'body',
    ]);
    expect(
      rowSelects.every((select) =>
        select.panelStyleClass?.includes('filter-expression-panel') &&
        select.panelStyleClass.includes('max-w-[calc(100vw-2rem)]') &&
        select.panelStyleClass.includes('sm:max-w-sm'),
      ),
    ).toBeTrue();
  });

  it('keeps the expression on one row above 24rem and reflows below it', () => {
    selectField(11);
    const host = fixture.nativeElement as HTMLElement;
    const controls = host.querySelector<HTMLElement>('.filter-condition-controls');
    const fieldLabel = host.querySelector<HTMLElement>('.field-control .p-select-label');

    host.style.width = '25rem';
    fieldLabel!.textContent = 'Un libellé de champ volontairement très long qui doit rester contraint';
    expect(getComputedStyle(controls!).gridTemplateAreas).toContain(
      'field operator value delete',
    );
    expect(getComputedStyle(fieldLabel!).textOverflow).toBe('ellipsis');
    expect(controls!.scrollWidth).toBeLessThanOrEqual(controls!.clientWidth);

    host.style.width = '23rem';
    expect(getComputedStyle(controls!).gridTemplateAreas).toContain(
      'field operator operator',
    );
    expect(getComputedStyle(controls!).gridTemplateAreas).toContain(
      'value value delete',
    );
  });

  it('only exposes approved operators and falls back to the first visible one', () => {
    selectField(11);
    expect(component.operatorOptions(component.rows.at(0)).map((option) => option.value))
      .toEqual(['EQUALS', 'CONTAINS']);

    component.removeFilter(0);
    selectField(12);
    expect(component.rows.at(0).controls.operator.value).toBe('EQUALS');

    component.rows.at(0).controls.fieldId.setValue(16, { emitEvent: false });
    component.onFieldChange(0);

    expect(component.rows.at(0).controls.operator.value).toBe('GREATER_THAN');
    expect(component.rows.at(0).controls.value1.value).toBe('');
    expect(component.rows.at(0).controls.value1.touched).toBeFalse();
  });

  it('uses PrimeNG date and time pickers while keeping string form values', () => {
    selectField(12);
    let row = component.rows.at(0);
    let pickers = fixture.debugElement.queryAll(By.directive(DatePicker));

    expect(pickers.length).toBe(1);
    expect((pickers[0].componentInstance as DatePicker).dataType).toBe('string');
    expect((pickers[0].componentInstance as DatePicker).dateFormat).toBe('yy-mm-dd');
    expect((pickers[0].componentInstance as DatePicker).appendTo()).toBe('body');

    row.controls.operator.setValue('BETWEEN', { emitEvent: false });
    component.onOperatorChange(0);
    fixture.detectChanges();
    pickers = fixture.debugElement.queryAll(By.directive(DatePicker));
    expect(pickers.length).toBe(2);

    component.removeFilter(0);
    selectField(17);
    row = component.rows.at(0);
    row.controls.value1.setValue('08:30:00');
    fixture.detectChanges();
    const timePicker = fixture.debugElement.query(By.directive(DatePicker))
      .componentInstance as DatePicker;

    expect(timePicker.timeOnly).toBeTrue();
    expect(timePicker.hourFormat).toBe('24');
    expect(timePicker.showSeconds).toBeTrue();
    expect(row.controls.value1.value).toBe('08:30:00');
  });

  it('serializes zero, one and two values with the required arity', () => {
    const emitted: FilterEditorState[] = [];
    component.configurationChange.subscribe((state) => emitted.push(state));
    selectField(11);
    const row = component.rows.at(0);

    row.controls.operator.setValue('IS_NULL', { emitEvent: false });
    component.onOperatorChange(0);
    expect(emitted.at(-1)).toEqual({
      filters: [{ fieldId: 11, operator: 'IS_NULL', values: [] }],
      count: 1,
      valid: true,
    });

    row.controls.fieldId.setValue(12, { emitEvent: false });
    component.onFieldChange(0);
    row.controls.operator.setValue('BETWEEN', { emitEvent: false });
    component.onOperatorChange(0);
    row.controls.value1.setValue('2026-08-01');
    row.controls.value2.setValue('2026-08-10');
    expect(emitted.at(-1)).toEqual({
      filters: [
        {
          fieldId: 12,
          operator: 'BETWEEN',
          values: ['2026-08-01', '2026-08-10'],
        },
      ],
      count: 1,
      valid: true,
    });

    row.controls.operator.setValue('EQUALS', { emitEvent: false });
    component.onOperatorChange(0);
    row.controls.value1.setValue('2026-08-10');
    expect(emitted.at(-1)?.filters[0].values).toEqual(['2026-08-10']);
  });

  it('does not serialize an incomplete draft or show an error before interaction', () => {
    const emitted: FilterEditorState[] = [];
    component.configurationChange.subscribe((state) => emitted.push(state));
    selectField(13);
    fixture.detectChanges();

    const row = component.rows.at(0);
    expect(emitted.at(-1)).toEqual({ filters: [], count: 1, valid: false });
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();

    row.controls.value1.markAsTouched();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Saisissez un nombre entier valide.');
  });

  it('provides concrete validation messages for invalid typed values', () => {
    selectField(14);
    const row = component.rows.at(0);
    row.controls.value1.setValue('2026-08-10T12:30');
    row.controls.value1.markAsTouched();
    fixture.detectChanges();

    expect(component.valueErrorMessage(row)).toContain('ISO avec un fuseau');
    expect(row.invalid).toBeTrue();

    row.controls.fieldId.setValue(15, { emitEvent: false });
    component.onFieldChange(0);
    row.controls.value1.setValue('not-a-uuid');
    row.controls.value1.markAsTouched();
    fixture.detectChanges();

    expect(component.valueErrorMessage(row)).toContain('UUID valide');
  });

  it('keeps repeated fields as AND filters', () => {
    selectField(11);
    component.rows.at(0).controls.value1.setValue('CDI');
    selectField(11);
    component.rows.at(1).controls.value1.setValue('Temps plein');
    fixture.detectChanges();

    expect(component.rows.length).toBe(2);
    expect(component.form.valid).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('ET');
  });

  it('restores focus to Add after deleting the final filter', fakeAsync(() => {
    selectField(11);
    component.removeFilter(0);
    fixture.detectChanges();
    flushMicrotasks();

    expect(component.rows.length).toBe(0);
    expect(document.activeElement?.id).toBe('add-filter-button');
  }));

  function selectField(fieldId: number): void {
    component.startAddingFilter();
    component.pendingFieldId.setValue(fieldId, { emitEvent: false });
    component.confirmPendingField();
    fixture.detectChanges();
  }

  function field(
    id: number,
    displayName: string,
    sourceName: string,
    dataType: ReportField['dataType'],
    supportedOperators: ReportField['supportedOperators'],
  ): ReportField {
    return {
      id,
      displayName,
      sourceName,
      dataType,
      nullable: true,
      supported: true,
      supportedOperators,
      key: `1:${id}`,
      datasetId: 1,
      datasetDisplayName: 'Shift',
    };
  }
});
