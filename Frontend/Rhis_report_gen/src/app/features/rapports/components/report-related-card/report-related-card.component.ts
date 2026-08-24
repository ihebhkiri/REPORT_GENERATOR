import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CheckboxModule } from 'primeng/checkbox';

@Component({
  selector: 'app-report-related-card',
  standalone: true,
  imports: [CheckboxModule, FormsModule],
  templateUrl: './report-related-card.component.html',
  styleUrl: './report-related-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportRelatedCardComponent {
  readonly inputId = input.required<string>();
  readonly title = input.required<string>();
  readonly icon = input.required<string>();
  readonly selected = input(false);
  readonly selectedChange = output<boolean>();
}
