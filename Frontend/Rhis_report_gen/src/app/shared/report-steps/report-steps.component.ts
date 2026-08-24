import {Component, input} from '@angular/core';
import {Step, StepList, Stepper} from "primeng/stepper";

@Component({
  selector: 'app-report-steps',
    imports: [
        Step,
        StepList,
        Stepper
    ],
  templateUrl: './report-steps.component.html',
  styleUrl: './report-steps.component.scss',
})
export class ReportStepsComponent {
  readonly reportWizardSteps = [
    { value: 1, label: 'Source de données' },
    { value: 2, label: 'Configuration' },
    { value: 3, label: 'Export' },
  ] as const;
  stepValue = input.required<number>() ;
  isStepCompleted(actualStepValue: number) {
    return actualStepValue < this.stepValue();
  }

}
