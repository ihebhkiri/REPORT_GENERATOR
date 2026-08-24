import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';

import {
  ReportSortRequest,
  SortDirection,
} from '../../../../models/report-preview.model';
import { ReportField } from '../../configuration.models';

interface SortFieldOption {
  readonly value: number;
  readonly label: string;
  readonly datasetLabel: string;
}

interface SortFieldOptionGroup {
  readonly label: string;
  readonly items: readonly SortFieldOption[];
}

interface SortDirectionOption {
  readonly label: string;
  readonly value: SortDirection;
}

const SORT_DIRECTION_OPTIONS: SortDirectionOption[] = [
  { label: 'Croissant', value: 'ASC' },
  { label: 'Décroissant', value: 'DESC' },
];

@Component({
  selector: 'app-sort-editor',
  standalone: true,
  imports: [ButtonModule, FormsModule, SelectModule],
  templateUrl: './sort-editor.component.html',
  styleUrl: './sort-editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SortEditorComponent {
  readonly selectedFields = input.required<readonly ReportField[]>();
  readonly sorts = input.required<readonly ReportSortRequest[]>();
  readonly sortsChange = output<readonly ReportSortRequest[]>();

  readonly canAddSort = computed(() => this.sorts().length < this.selectedFields().length);
  readonly directionOptions = SORT_DIRECTION_OPTIONS;

  addSort(): void {
    const usedIds = new Set(this.sorts().map((sort) => sort.fieldId));
    const field = this.selectedFields().find((candidate) => !usedIds.has(candidate.id));
    if (!field) {
      return;
    }

    this.sortsChange.emit([...this.sorts(), { fieldId: field.id, direction: 'ASC' }]);
  }

  removeSort(index: number): void {
    this.sortsChange.emit(this.sorts().filter((_, currentIndex) => currentIndex !== index));
  }

  updateField(index: number, fieldId: number | null): void {
    if (fieldId === null || !this.fieldOptions(index).some((field) => field.id === fieldId)) {
      return;
    }

    this.sortsChange.emit(
      this.sorts().map((sort, currentIndex) =>
        currentIndex === index ? { ...sort, fieldId } : sort,
      ),
    );
  }

  updateDirection(index: number, direction: SortDirection | null): void {
    if (direction !== 'ASC' && direction !== 'DESC') {
      return;
    }

    this.sortsChange.emit(
      this.sorts().map((sort, currentIndex) =>
        currentIndex === index ? { ...sort, direction } : sort,
      ),
    );
  }

  moveSort(index: number, offset: -1 | 1): void {
    const targetIndex = index + offset;
    if (targetIndex < 0 || targetIndex >= this.sorts().length) {
      return;
    }

    const reordered = [...this.sorts()];
    [reordered[index], reordered[targetIndex]] = [reordered[targetIndex], reordered[index]];
    this.sortsChange.emit(reordered);
  }

  fieldOptions(index: number): readonly ReportField[] {
    const currentFieldId = this.sorts()[index]?.fieldId;
    const usedIds = new Set(
      this.sorts()
        .filter((_, currentIndex) => currentIndex !== index)
        .map((sort) => sort.fieldId),
    );

    return this.selectedFields().filter(
      (field) => field.id === currentFieldId || !usedIds.has(field.id),
    );
  }

  fieldOptionGroups(index: number): SortFieldOptionGroup[] {
    const groups = new Map<number, { label: string; items: SortFieldOption[] }>();

    for (const field of this.fieldOptions(index)) {
      let group = groups.get(field.datasetId);
      if (!group) {
        group = { label: field.datasetDisplayName, items: [] };
        groups.set(field.datasetId, group);
      }

      group.items.push({
        value: field.id,
        label: field.displayName,
        datasetLabel: field.datasetDisplayName,
      });
    }

    return Array.from(groups.values());
  }
}
