import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Select } from 'primeng/select';

import { ReportSortRequest, SortDirection } from '../../../../models/report-preview.model';
import { ReportField } from '../../configuration.models';
import { SortEditorComponent } from './sort-editor.component';

describe('SortEditorComponent', () => {
  let fixture: ComponentFixture<SortEditorComponent>;
  let component: SortEditorComponent;

  const employeeName = field(11, 'Nom', 1, 'Rhis Employee');
  const shiftDate = field(12, 'Date journée', 2, 'Rhis Shift');
  const employeeNumber = field(13, 'Matricule', 1, 'Rhis Employee');
  const fields = [employeeName, shiftDate, employeeNumber];
  const initialSorts: readonly ReportSortRequest[] = [
    { fieldId: 11, direction: 'ASC' },
    { fieldId: 12, direction: 'DESC' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SortEditorComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(SortEditorComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('selectedFields', fields);
    fixture.componentRef.setInput('sorts', initialSorts);
    fixture.detectChanges();
  });

  it('groups allowed fields by dataset while preserving selection order', () => {
    fixture.componentRef.setInput('sorts', [{ fieldId: 11, direction: 'ASC' }]);
    fixture.detectChanges();

    expect(component.fieldOptionGroups(0)).toEqual([
      {
        label: 'Rhis Employee',
        items: [
          { value: 11, label: 'Nom', datasetLabel: 'Rhis Employee' },
          { value: 13, label: 'Matricule', datasetLabel: 'Rhis Employee' },
        ],
      },
      {
        label: 'Rhis Shift',
        items: [
          { value: 12, label: 'Date journée', datasetLabel: 'Rhis Shift' },
        ],
      },
    ]);
  });

  it('shows only field names in closed selectors and uses the two friendly directions', async () => {
    await fixture.whenStable();
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    const fieldLabels = Array.from(
      host.querySelectorAll<HTMLElement>('.field-control .p-select-label'),
      (label) => label.textContent?.trim(),
    );

    expect(fieldLabels).toEqual(['Nom', 'Date journée']);
    expect(fieldLabels.join(' ')).not.toContain('Rhis');
    expect(component.directionOptions).toEqual([
      { label: 'Croissant', value: 'ASC' },
      { label: 'Décroissant', value: 'DESC' },
    ]);
  });

  it('adds the first unused selected field in ascending order', () => {
    const emitSpy = spyOn(component.sortsChange, 'emit');

    component.addSort();

    expect(emitSpy).toHaveBeenCalledOnceWith([
      ...initialSorts,
      { fieldId: 13, direction: 'ASC' },
    ]);
  });

  it('updates valid values and rejects duplicate or unsupported values', () => {
    const emitSpy = spyOn(component.sortsChange, 'emit');

    component.updateField(0, 13);
    expect(emitSpy).toHaveBeenCalledOnceWith([
      { fieldId: 13, direction: 'ASC' },
      { fieldId: 12, direction: 'DESC' },
    ]);

    emitSpy.calls.reset();
    component.updateField(0, 12);
    component.updateField(0, 999);
    expect(emitSpy).not.toHaveBeenCalled();

    component.updateDirection(0, 'DESC');
    expect(emitSpy).toHaveBeenCalledOnceWith([
      { fieldId: 11, direction: 'DESC' },
      { fieldId: 12, direction: 'DESC' },
    ]);

    emitSpy.calls.reset();
    component.updateDirection(0, 'INVALID' as SortDirection);
    expect(emitSpy).not.toHaveBeenCalled();
  });

  it('removes and reorders rules without mutating the emitted priority order', () => {
    const emitSpy = spyOn(component.sortsChange, 'emit');

    component.removeSort(0);
    expect(emitSpy).toHaveBeenCalledOnceWith([{ fieldId: 12, direction: 'DESC' }]);

    emitSpy.calls.reset();
    component.moveSort(1, -1);
    expect(emitSpy).toHaveBeenCalledOnceWith([
      { fieldId: 12, direction: 'DESC' },
      { fieldId: 11, direction: 'ASC' },
    ]);

    emitSpy.calls.reset();
    component.moveSort(0, -1);
    component.moveSort(1, 1);
    expect(emitSpy).not.toHaveBeenCalled();
  });

  it('renders grouped PrimeNG selectors, sentence connectors and boundary controls', () => {
    const host = fixture.nativeElement as HTMLElement;
    const rowSelects = fixture.debugElement
      .queryAll(By.directive(Select))
      .filter((select) => select.nativeElement.closest('.sort-condition-controls'))
      .map((select) => select.componentInstance as Select);

    expect(host.querySelector('.sort-context')?.textContent?.trim()).toBe(
      'Trier les résultats par',
    );
    expect(host.querySelector('.sort-conjunction')?.textContent?.trim()).toBe('PUIS');
    expect(rowSelects.length).toBe(4);
    expect(rowSelects.every((select) => select.appendTo() === 'body')).toBeTrue();
    expect(rowSelects[0].group).toBeTrue();

    expect(button(host, 'Monter le tri 1').disabled).toBeTrue();
    expect(button(host, 'Descendre le tri 1').disabled).toBeFalse();
    expect(button(host, 'Monter le tri 2').disabled).toBeFalse();
    expect(button(host, 'Descendre le tri 2').disabled).toBeTrue();
  });

  it('keeps all four segments on one line without horizontal overflow at 1280 and 360 px', () => {
    const host = fixture.nativeElement as HTMLElement;
    const controls = host.querySelector<HTMLElement>('.sort-condition-controls');
    const segments = controls?.querySelectorAll<HTMLElement>(
      ':scope > .field-control, :scope > .direction-control, :scope > .priority-control, :scope > .delete-control',
    );

    for (const width of ['1280px', '360px']) {
      host.style.width = width;
      fixture.detectChanges();

      expect(getComputedStyle(controls!).gridTemplateAreas).toContain(
        'field direction priority delete',
      );
      const segmentOffsets = new Set(
        Array.from(segments ?? [], (segment) => segment.offsetTop),
      );
      expect(segmentOffsets.size).toBe(1);
      expect(controls!.scrollWidth).toBeLessThanOrEqual(controls!.clientWidth);
    }
  });

  function button(host: HTMLElement, ariaLabel: string): HTMLButtonElement {
    const element = host.querySelector<HTMLButtonElement>(`button[aria-label="${ariaLabel}"]`);
    expect(element).withContext(`Bouton introuvable : ${ariaLabel}`).not.toBeNull();
    return element!;
  }

  function field(
    id: number,
    displayName: string,
    datasetId: number,
    datasetDisplayName: string,
  ): ReportField {
    return {
      id,
      displayName,
      sourceName: displayName.toLowerCase(),
      dataType: 'TEXT',
      nullable: false,
      supported: true,
      supportedOperators: ['EQUALS'],
      key: `${datasetId}:${id}`,
      datasetId,
      datasetDisplayName,
    };
  }
});
