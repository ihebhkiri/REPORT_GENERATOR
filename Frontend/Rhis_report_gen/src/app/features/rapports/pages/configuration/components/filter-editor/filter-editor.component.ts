import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { Select, SelectModule } from 'primeng/select';

import {
  DatasetFieldType,
  FilterOperator,
} from '../../../../models/dataset-field.model';
import { ReportFilterRequest } from '../../../../models/report-preview.model';
import { DatasetFieldGroup, ReportField } from '../../configuration.models';

type FilterRowForm = FormGroup<{
  fieldId: FormControl<number | null>;
  operator: FormControl<FilterOperator | null>;
  value1: FormControl<string>;
  value2: FormControl<string>;
}>;

interface FilterFieldOption {
  readonly value: number;
  readonly label: string;
  readonly datasetLabel: string;
  readonly field: ReportField;
}

interface FilterFieldOptionGroup {
  readonly label: string;
  readonly items: FilterFieldOption[];
}

interface OperatorOption {
  readonly value: FilterOperator;
  readonly label: string;
}

export interface FilterEditorState {
  readonly filters: readonly ReportFilterRequest[];
  readonly count: number;
  readonly valid: boolean;
}

const OPERATOR_LABELS: Readonly<Record<FilterOperator, string>> = {
  EQUALS: 'est égal à',
  NOT_EQUALS: 'est différent de',
  IS_NULL: 'est vide',
  IS_NOT_NULL: "n'est pas vide",
  CONTAINS: 'contient',
  GREATER_THAN: 'est supérieur à',
  GREATER_THAN_OR_EQUAL: 'est supérieur ou égal à',
  LESS_THAN: 'est inférieur à',
  LESS_THAN_OR_EQUAL: 'est inférieur ou égal à',
  BETWEEN: 'est compris entre',
};

const INTEGER_PATTERN = /^-?\d+$/;
const VISIBLE_OPERATORS = new Set<FilterOperator>([
  'EQUALS',
  'CONTAINS',
  'GREATER_THAN',
  'GREATER_THAN_OR_EQUAL',
  'LESS_THAN',
  'LESS_THAN_OR_EQUAL',
  'BETWEEN',
]);

const DECIMAL_PATTERN = /^-?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?$/;
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const OFFSET_DATE_TIME_PATTERN =
  /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d{1,9})?)?(?:Z|[+-]\d{2}:\d{2})$/;

@Component({
  selector: 'app-filter-editor',
  standalone: true,
  imports: [ButtonModule, DatePickerModule, ReactiveFormsModule, SelectModule],
  templateUrl: './filter-editor.component.html',
  styleUrl: './filter-editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilterEditorComponent {
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private lastResetToken = -1;

  readonly fieldGroups = input.required<readonly DatasetFieldGroup[]>();
  readonly initialFilters = input<readonly ReportFilterRequest[]>([]);
  readonly resetToken = input(0);
  readonly configurationChange = output<FilterEditorState>();

  readonly form = new FormGroup({
    rows: new FormArray<FilterRowForm>([]),
  });
  readonly pendingFieldId = new FormControl<number | null>(null);
  readonly addingFilter = signal(false);
  readonly fieldPicker = viewChild<Select>('fieldPicker');
  readonly datePickerInputStyle: Readonly<Record<string, string>> = {
    width: '100%',
    minWidth: '0',
    height: '2.75rem',
    border: '0',
    borderRadius: '0',
    background: 'transparent',
    boxShadow: 'none',
    padding: '0 0.75rem',
    fontSize: '0.875rem',
    fontVariantNumeric: 'tabular-nums',
  };

  readonly filterFieldGroups = computed<FilterFieldOptionGroup[]>(() =>
    this.fieldGroups()
      .map((group) => ({
        label: group.dataset.displayName,
        items: group.fields
          .filter((field) => this.visibleOperators(field).length > 0)
          .map((field) => ({
            value: field.id,
            label: field.displayName,
            datasetLabel: group.dataset.displayName,
            field,
          })),
      }))
      .filter((group) => group.items.length > 0),
  );

  readonly filterFields = computed<readonly FilterFieldOption[]>(() =>
    this.filterFieldGroups().flatMap((group) => group.items),
  );

  readonly canAddFilter = computed(() =>
    this.filterFields().length > 0 && !this.addingFilter(),
  );

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.emitConfiguration());

    effect(() => {
      const allowedIds = new Set(this.filterFields().map((option) => option.value));
      let changed = false;

      for (let index = this.rows.length - 1; index >= 0; index--) {
        const fieldId = this.rows.at(index).controls.fieldId.value;
        if (fieldId === null || !allowedIds.has(fieldId)) {
          this.rows.removeAt(index, { emitEvent: false });
          changed = true;
        }
      }

      if (changed) {
        this.emitConfiguration();
      }
    });

    effect(() => {
      const resetToken = this.resetToken();
      if (resetToken === this.lastResetToken) {
        return;
      }

      this.lastResetToken = resetToken;
      this.addingFilter.set(false);
      this.pendingFieldId.setValue(null, { emitEvent: false });
      this.rows.clear({ emitEvent: false });
      for (const filter of this.initialFilters()) {
        const field = this.fieldFor(filter.fieldId);
        if (!field) {
          continue;
        }
        const row = this.createRow(field);
        row.controls.operator.setValue(filter.operator, { emitEvent: false });
        row.controls.value1.setValue(filter.values[0] ?? '', { emitEvent: false });
        row.controls.value2.setValue(filter.values[1] ?? '', { emitEvent: false });
        this.configureValueControls(row);
        this.rows.push(row, { emitEvent: false });
      }
      this.emitConfiguration();
    });
  }

  get rows(): FormArray<FilterRowForm> {
    return this.form.controls.rows;
  }

  startAddingFilter(): void {
    if (!this.canAddFilter()) {
      return;
    }

    this.pendingFieldId.setValue(null, { emitEvent: false });
    this.addingFilter.set(true);
    this.changeDetectorRef.detectChanges();
    this.fieldPicker()?.focus();
    this.fieldPicker()?.show(true);
  }

  cancelAddingFilter(): void {
    if (!this.addingFilter()) {
      return;
    }

    this.addingFilter.set(false);
    this.pendingFieldId.setValue(null, { emitEvent: false });
    this.focusElement('add-filter-button');
  }

  confirmPendingField(): void {
    const field = this.fieldFor(this.pendingFieldId.value);
    if (!field) {
      return;
    }

    const row = this.createRow(field);
    const index = this.rows.length;
    this.rows.push(row, { emitEvent: false });
    this.addingFilter.set(false);
    this.pendingFieldId.setValue(null, { emitEvent: false });
    this.configureValueControls(row);
    this.emitConfiguration();
    this.changeDetectorRef.detectChanges();
    this.focusElement(`filter-value-1-${index}`);
  }

  removeFilter(index: number): void {
    this.rows.removeAt(index);
    this.changeDetectorRef.detectChanges();

    if (this.rows.length === 0) {
      this.focusElement('add-filter-button');
      return;
    }

    const nextIndex = Math.min(index, this.rows.length - 1);
    this.focusElement(`filter-field-${nextIndex}`);
  }

  onFieldChange(index: number): void {
    const row = this.rows.at(index);
    const field = this.fieldFor(row.controls.fieldId.value);

    row.controls.operator.setValue(
      field ? this.suggestedOperator(field) : null,
      { emitEvent: false },
    );
    row.controls.value1.setValue('', { emitEvent: false });
    row.controls.value2.setValue('', { emitEvent: false });
    this.configureValueControls(row);
    row.markAsUntouched();
    this.emitConfiguration();
    this.changeDetectorRef.detectChanges();
    this.focusElement(`filter-value-1-${index}`);
  }

  onOperatorChange(index: number): void {
    const row = this.rows.at(index);
    row.controls.value1.setValue('', { emitEvent: false });
    row.controls.value2.setValue('', { emitEvent: false });
    this.configureValueControls(row);
    row.controls.value1.markAsUntouched();
    row.controls.value2.markAsUntouched();
    this.emitConfiguration();
    this.changeDetectorRef.detectChanges();

    if (this.valueCount(row) > 0) {
      this.focusElement(`filter-value-1-${index}`);
    }
  }

  operatorOptions(row: FilterRowForm): OperatorOption[] {
    const field = this.fieldFor(row.controls.fieldId.value);
    return (field ? this.visibleOperators(field) : []).map((operator) => ({
      value: operator,
      label: OPERATOR_LABELS[operator],
    }));
  }

  usesDatePicker(row: FilterRowForm): boolean {
    return this.fieldFor(row.controls.fieldId.value)?.dataType === 'DATE';
  }

  usesTimePicker(row: FilterRowForm): boolean {
    return this.fieldFor(row.controls.fieldId.value)?.dataType === 'TIME';
  }

  valueCount(row: FilterRowForm): 0 | 1 | 2 {
    const operator = row.controls.operator.value;
    if (operator === 'IS_NULL' || operator === 'IS_NOT_NULL') {
      return 0;
    }
    return operator === 'BETWEEN' ? 2 : 1;
  }

  inputType(row: FilterRowForm): string {
    return this.htmlInputType(this.fieldFor(row.controls.fieldId.value)?.dataType);
  }

  inputMode(row: FilterRowForm): 'decimal' | 'numeric' | 'text' | null {
    const type = this.fieldFor(row.controls.fieldId.value)?.dataType;
    if (type === 'INTEGER') {
      return 'numeric';
    }
    if (type === 'DECIMAL') {
      return 'decimal';
    }
    if (type === 'OFFSET_DATE_TIME' || type === 'UUID') {
      return 'text';
    }
    return null;
  }

  inputStep(row: FilterRowForm): string | null {
    const type = this.fieldFor(row.controls.fieldId.value)?.dataType;
    if (type === 'INTEGER') {
      return '1';
    }
    if (type === 'DECIMAL') {
      return 'any';
    }
    if (type === 'TIME' || type === 'DATE_TIME') {
      return '1';
    }
    return null;
  }

  isBoolean(row: FilterRowForm): boolean {
    return this.fieldFor(row.controls.fieldId.value)?.dataType === 'BOOLEAN';
  }

  valuePlaceholder(row: FilterRowForm): string {
    const type = this.fieldFor(row.controls.fieldId.value)?.dataType;
    if (type === 'OFFSET_DATE_TIME') {
      return '2026-08-10T12:30:00+02:00';
    }
    if (type === 'UUID') {
      return '123e4567-e89b-12d3-a456-426614174000';
    }
    return 'Saisir une valeur';
  }

  shouldShowError(control: AbstractControl): boolean {
    return control.invalid && control.touched;
  }

  valueErrorMessage(row: FilterRowForm, secondValue = false): string {
    const type = this.fieldFor(row.controls.fieldId.value)?.dataType;
    const control = secondValue ? row.controls.value2 : row.controls.value1;

    if (control.hasError('required')) {
      const typedMessage = this.requiredValueMessage(type);
      return secondValue
        ? `Seconde valeur : ${typedMessage.charAt(0).toLowerCase()}${typedMessage.slice(1)}`
        : typedMessage;
    }

    switch (type) {
      case 'INTEGER':
        return 'Saisissez un nombre entier valide.';
      case 'DECIMAL':
        return 'Saisissez un nombre décimal valide.';
      case 'DATE':
        return 'Saisissez une date valide.';
      case 'TIME':
        return 'Saisissez une heure valide.';
      case 'DATE_TIME':
        return 'Saisissez une date et une heure valides.';
      case 'OFFSET_DATE_TIME':
        return 'Saisissez une date et une heure ISO avec un fuseau, par exemple +02:00.';
      case 'UUID':
        return 'Saisissez un identifiant UUID valide.';
      default:
        return 'Saisissez une valeur valide.';
    }
  }

  private createRow(field: ReportField): FilterRowForm {
    return new FormGroup({
      fieldId: new FormControl<number | null>(field.id, Validators.required),
      operator: new FormControl<FilterOperator | null>(
        this.suggestedOperator(field),
        Validators.required,
      ),
      value1: new FormControl('', { nonNullable: true }),
      value2: new FormControl('', { nonNullable: true }),
    });
  }

  private requiredValueMessage(type: DatasetFieldType | undefined): string {
    switch (type) {
      case 'INTEGER':
        return 'Saisissez un nombre entier valide.';
      case 'DECIMAL':
        return 'Saisissez un nombre décimal valide.';
      case 'DATE':
        return 'Saisissez une date valide.';
      case 'TIME':
        return 'Saisissez une heure valide.';
      case 'DATE_TIME':
        return 'Saisissez une date et une heure valides.';
      case 'OFFSET_DATE_TIME':
        return 'Saisissez une date et une heure ISO avec un fuseau.';
      case 'UUID':
        return 'Saisissez un identifiant UUID valide.';
      default:
        return 'Saisissez une valeur.';
    }
  }

  private suggestedOperator(field: ReportField): FilterOperator | null {
    const operators = this.visibleOperators(field);
    if (field.dataType === 'TEXT' && operators.includes('CONTAINS')) {
      return 'CONTAINS';
    }
    if (operators.includes('EQUALS')) {
      return 'EQUALS';
    }
    return operators[0] ?? null;
  }

  private visibleOperators(field: ReportField): readonly FilterOperator[] {
    return field.supportedOperators.filter((operator) => VISIBLE_OPERATORS.has(operator));
  }

  private configureValueControls(row: FilterRowForm): void {
    const field = this.fieldFor(row.controls.fieldId.value);
    const count = this.valueCount(row);

    row.controls.value1.setValidators(this.valueValidators(field?.dataType, count > 0));
    row.controls.value2.setValidators(this.valueValidators(field?.dataType, count === 2));

    row.controls.value1.updateValueAndValidity({ emitEvent: false });
    row.controls.value2.updateValueAndValidity({ emitEvent: false });
  }

  private valueValidators(
    type: DatasetFieldType | undefined,
    required: boolean,
  ): ValidatorFn[] {
    if (!required) {
      return [];
    }

    const validators: ValidatorFn[] = [Validators.required];
    switch (type) {
      case 'TEXT':
        validators.push(Validators.pattern(/\S/));
        break;
      case 'INTEGER':
        validators.push(Validators.pattern(INTEGER_PATTERN));
        break;
      case 'DECIMAL':
        validators.push(Validators.pattern(DECIMAL_PATTERN));
        break;
      case 'OFFSET_DATE_TIME':
        validators.push(Validators.pattern(OFFSET_DATE_TIME_PATTERN));
        break;
      case 'UUID':
        validators.push(Validators.pattern(UUID_PATTERN));
        break;
    }
    return validators;
  }

  private htmlInputType(type: DatasetFieldType | undefined): string {
    switch (type) {
      case 'INTEGER':
      case 'DECIMAL':
        return 'number';
      case 'DATE':
        return 'date';
      case 'TIME':
        return 'time';
      case 'DATE_TIME':
        return 'datetime-local';
      default:
        return 'text';
    }
  }

  private fieldFor(fieldId: number | null): ReportField | undefined {
    return this.filterFields().find((option) => option.value === fieldId)?.field;
  }

  private emitConfiguration(): void {
    const filters = this.rows.controls.flatMap((row) => {
      if (row.invalid) {
        return [];
      }

      const fieldId = row.controls.fieldId.value;
      const operator = row.controls.operator.value;
      if (fieldId === null || operator === null) {
        return [];
      }

      const count = this.valueCount(row);
      const values = count === 0
        ? []
        : count === 1
          ? [row.controls.value1.value]
          : [row.controls.value1.value, row.controls.value2.value];

      return [{ fieldId, operator, values } satisfies ReportFilterRequest];
    });

    this.configurationChange.emit({
      filters,
      count: this.rows.length,
      valid: this.form.valid && filters.length === this.rows.length,
    });
  }

  private focusElement(id: string): void {
    queueMicrotask(() => {
      this.host.nativeElement.querySelector<HTMLElement>(`#${id}`)?.focus();
    });
  }
}
