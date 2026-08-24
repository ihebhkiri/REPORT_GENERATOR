import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  OnChanges,
  output,
  SimpleChanges,
  signal,
} from '@angular/core';
import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';

import { DatasetFieldGroup, ReportField } from '../../configuration.models';

@Component({
  selector: 'app-column-selector',
  standalone: true,
  imports: [AccordionModule, ButtonModule, CdkDrag, CdkDragHandle, CdkDropList],
  templateUrl: './column-selector.component.html',
  styleUrl: './column-selector.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ColumnSelectorComponent implements OnChanges {
  readonly fieldGroups = input.required<readonly DatasetFieldGroup[]>();
  readonly selectedFields = input.required<readonly ReportField[]>();
  readonly selectedFieldsChange = output<readonly ReportField[]>();

  readonly searchTerm = signal('');
  readonly expandedFieldGroupIds = signal<number[]>([]);

  readonly normalizedSearch = computed(() =>
    this.searchTerm().trim().toLocaleLowerCase('fr'),
  );

  readonly availableFieldGroups = computed<readonly DatasetFieldGroup[]>(() => {
    const selectedKeys = new Set(this.selectedFields().map((field) => field.key));
    const search = this.normalizedSearch();

    return this.fieldGroups().map((group) => {
      const datasetMatches =
        !search ||
        group.dataset.displayName.toLocaleLowerCase('fr').includes(search);
      const fields = group.fields.filter(
        (field) =>
          !selectedKeys.has(field.key) &&
          (datasetMatches || field.displayName.toLocaleLowerCase('fr').includes(search)),
      );

      return { ...group, fields };
    });
  });

  readonly selectedCount = computed(() => this.selectedFields().length);
  readonly totalFieldCount = computed(() =>
    this.fieldGroups().reduce((total, group) => total + group.fields.length, 0),
  );
  readonly hasUnselectedFields = computed(
    () => this.totalFieldCount() > this.selectedFields().length,
  );

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['fieldGroups']) {
      this.expandedFieldGroupIds.set(this.fieldGroups().map((group) => group.dataset.id));
    }
  }

  updateSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  addField(field: ReportField): void {
    if (this.selectedFields().some((selectedField) => selectedField.key === field.key)) {
      return;
    }

    this.selectedFieldsChange.emit([...this.selectedFields(), field]);
  }

  removeField(field: ReportField): void {
    this.selectedFieldsChange.emit(
      this.selectedFields().filter((current) => current.key !== field.key),
    );
  }

  dropSelectedField(event: CdkDragDrop<readonly ReportField[]>): void {
    if (event.previousIndex === event.currentIndex) {
      return;
    }

    const reorderedFields = [...this.selectedFields()];
    moveItemInArray(reorderedFields, event.previousIndex, event.currentIndex);
    this.selectedFieldsChange.emit(reorderedFields);
  }

  moveField(field: ReportField, offset: -1 | 1): void {
    const reorderedFields = [...this.selectedFields()];
    const currentIndex = reorderedFields.findIndex((current) => current.key === field.key);
    const targetIndex = currentIndex + offset;

    if (currentIndex < 0 || targetIndex < 0 || targetIndex >= reorderedFields.length) {
      return;
    }

    moveItemInArray(reorderedFields, currentIndex, targetIndex);
    this.selectedFieldsChange.emit(reorderedFields);
  }
}
