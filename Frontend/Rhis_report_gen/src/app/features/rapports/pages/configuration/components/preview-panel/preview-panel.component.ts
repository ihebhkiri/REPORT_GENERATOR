import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';

import { ReportPreviewCell, ReportPreviewResponse } from '../../../../models/report-preview.model';

export type PreviewStatus =
  | 'À jour'
  | 'Mise à jour…'
  | 'Aperçu précédent'
  | 'Configuration incomplète'
  | 'Erreur'
  | 'En attente';

@Component({
  selector: 'app-preview-panel',
  standalone: true,
  imports: [ButtonModule, ProgressSpinnerModule, TableModule],
  templateUrl: './preview-panel.component.html',
  styleUrl: './preview-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PreviewPanelComponent {
  readonly loading = input(false);
  readonly response = input<ReportPreviewResponse | null>(null);
  readonly error = input<string | null>(null);
  readonly stale = input(false);
  readonly status = input<PreviewStatus>('En attente');
  readonly incompleteMessage = input<string | null>(null);
  readonly collapsed = input(false);
  readonly retryDisabled = input(false);
  readonly tableRows = computed(() => [...(this.response()?.rows ?? [])]);

  readonly retry = output<void>();

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
