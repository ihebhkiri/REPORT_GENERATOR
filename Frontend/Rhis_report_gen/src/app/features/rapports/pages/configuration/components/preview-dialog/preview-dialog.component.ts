import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';

import { ReportPreviewCell, ReportPreviewResponse } from '../../../../models/report-preview.model';

@Component({
  selector: 'app-preview-dialog',
  standalone: true,
  imports: [ButtonModule, DialogModule, ProgressSpinnerModule, TableModule],
  templateUrl: './preview-dialog.component.html',
  styleUrl: './preview-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PreviewDialogComponent {
  readonly visible = input(false);
  readonly loading = input(false);
  readonly response = input<ReportPreviewResponse | null>(null);
  readonly error = input<string | null>(null);
  readonly stale = input(false);
  readonly tableRows = computed(() => [...(this.response()?.rows ?? [])]);

  readonly visibleChange = output<boolean>();
  readonly retry = output<void>();

  close(): void {
    this.visibleChange.emit(false);
  }

  formatValue(value: ReportPreviewCell | undefined): string {
    if (value === null || value === undefined) {
      return '—';
    }
    if (typeof value === 'boolean') {
      return value ? 'Oui' : 'Non';
    }
    return String(value);
  }
}
