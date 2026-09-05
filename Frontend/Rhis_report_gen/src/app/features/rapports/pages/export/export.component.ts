import { NgTemplateOutlet } from '@angular/common';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationStart, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { ProgressBarModule } from 'primeng/progressbar';
import { StepperModule } from 'primeng/stepper';
import { TagModule } from 'primeng/tag';
import { TimelineModule } from 'primeng/timeline';
import { Observable, filter, finalize, retry, switchMap, takeWhile, throwError, timer } from 'rxjs';

import {
  ReportExport,
  ReportExportFormat,
  ReportGeneration,
} from '../../models/report-generation.model';
import { ApiProblem } from '../../models/report-preview.model';
import { ReportDraftStorageService } from '../../services/report-draft-storage.service';
import { ReportGenerationService } from '../../services/report-generation.service';
import {ReportStepsComponent} from '../../../../shared/report-steps/report-steps.component';

type ExportWorkflowStepId = 'preparation' | 'formats' | 'download';
type ExportWorkflowState = 'completed' | 'active' | 'pending' | 'error';
type ExportTagSeverity = 'secondary' | 'info' | 'success' | 'danger';

interface ExportFormatDefinition {
  readonly format: ReportExportFormat;
  readonly title: string;
  readonly description: string;
  readonly imageSrc: string;
  readonly exportLabel: string;
  readonly downloadLabel: string;
  readonly retryLabel: string;
}

interface ExportWorkflowStep {
  readonly id: ExportWorkflowStepId;
  readonly number: number;
  readonly label: string;
}

const EXPORT_FORMATS: readonly ExportFormatDefinition[] = [
  {
    format: 'PDF',
    title: 'Document PDF',
    description: 'Document paginé',
    imageSrc: '/assets/pdf logo.avif',
    exportLabel: 'Exporter PDF',
    downloadLabel: 'Télécharger PDF',
    retryLabel: 'Réessayer PDF',
  },
  {
    format: 'XLSX',
    title: 'Classeur Excel',
    description: 'Fichier XLSX typé',
    imageSrc: '/assets/ms-excel.jpg',
    exportLabel: 'Exporter Excel',
    downloadLabel: 'Télécharger Excel',
    retryLabel: 'Réessayer Excel',
  },
];

const EXPORT_WORKFLOW_STEPS: ExportWorkflowStep[] = [
  { id: 'preparation', number: 1, label: 'Préparation' },
  { id: 'formats', number: 2, label: 'Formats' },
  { id: 'download', number: 3, label: 'Téléchargement' },
];

const EXPORT_POLLING_INTERVAL_MS = 2_000;
const TRANSIENT_POLLING_RETRY_DELAY_MS = 2_000;

@Component({
  selector: 'app-export',
  standalone: true,
  imports: [
    NgTemplateOutlet,
    ButtonModule,
    CardModule,
    MessageModule,
    ProgressBarModule,
    StepperModule,
    TagModule,
    TimelineModule,
    ReportStepsComponent,
  ],
  templateUrl: './export.component.html',
  styleUrl: './export.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExportComponent {
readonly steps = [
  { number: 1, label: 'Source de données', active: true },
  { number: 2, label: 'Configuration', active: false },
  { number: 3, label: 'Export', active: false },
];
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reportGenerationService = inject(ReportGenerationService);
  private readonly reportDraftStorage = inject(ReportDraftStorageService);

  readonly reportGeneration = signal<ReportGeneration | null>(null);
  readonly pdfExport = signal<ReportExport | null>(null);
  readonly xlsxExport = signal<ReportExport | null>(null);
  readonly selectedFormats = signal<ReadonlySet<ReportExportFormat>>(new Set());
  readonly creatingFormats = signal<ReadonlySet<ReportExportFormat>>(new Set());
  readonly formatErrors = signal<Partial<Record<ReportExportFormat, string>>>({});
  readonly errorMessage = signal<string | null>(null);
  readonly networkInterrupted = signal(false);
  readonly reportGenerationId = this.route.snapshot.paramMap.get('generationId') ?? '';
  readonly reportWizardSteps = [
    { value: 1, label: 'Source de données' },
    { value: 2, label: 'Configuration' },
    { value: 3, label: 'Export' },
  ] as const;
  readonly workflowSteps = EXPORT_WORKFLOW_STEPS;
  readonly formatDefinitions = EXPORT_FORMATS;
  readonly availableExportOptions = computed(() =>
    this.formatDefinitions.filter(({ format }) => !this.selectedFormats().has(format)),
  );
  readonly selectedExportOptions = computed(() =>
    this.formatDefinitions.filter(({ format }) => this.selectedFormats().has(format)),
  );
  readonly readyExportCount = computed(() =>
    Number(this.pdfExport()?.status === 'READY') + Number(this.xlsxExport()?.status === 'READY'),
  );
  readonly preparationSummary = computed(() => {
    const reportGeneration = this.reportGeneration();
    if (!reportGeneration) {
      return 'Chargement de la préparation…';
    }
    if (reportGeneration.status === 'READY') {
      const rowCount = reportGeneration.totalRowCount ?? reportGeneration.processedRowCount ?? 0;
      return `${this.rowCountLabel(rowCount)} prête${rowCount !== 1 ? 's' : ''}`;
    }
    if (reportGeneration.status === 'FAILED') {
      return 'Préparation interrompue';
    }
    if (reportGeneration.status === 'EXPIRED') {
      return 'Rapport expiré';
    }
    const processed = reportGeneration.processedRowCount ?? 0;
    return reportGeneration.totalRowCount === null
      ? `${this.rowCountLabel(processed)} traitée${processed !== 1 ? 's' : ''}`
      : `${processed} sur ${reportGeneration.totalRowCount} lignes`;
  });
  readonly formatsSummary = computed(() => {
    if (this.reportGeneration()?.status !== 'READY') {
      return 'Disponible après la préparation';
    }
    const availableCount = this.availableExportOptions().length;
    return availableCount === 0
      ? 'Tous les formats ont été sélectionnés'
      : availableCount === 1
        ? '1 format encore disponible'
        : 'Choisissez un ou deux formats';
  });
  readonly downloadSummary = computed(() => {
    const selectedCount = this.selectedExportOptions().length;
    if (selectedCount === 0) {
      return 'Aucun format sélectionné';
    }
    if (this.readyExportCount() === selectedCount) {
      return selectedCount === 1 ? '1 fichier prêt' : '2 fichiers prêts';
    }
    return selectedCount === 1 ? '1 fichier en préparation' : 'Fichiers en préparation';
  });
  private cleanupRequested = false;

  constructor() {
    if (!this.reportGenerationId) {
      void this.restoreConfiguration('La génération demandée est invalide.');
      return;
    }
    this.resumeExports();
    this.pollReportGeneration();
    this.router.events.pipe(
      filter((event): event is NavigationStart => event instanceof NavigationStart),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.cleanupOnNavigation());
  }

  createExport(format: ReportExportFormat): void {
    const existingExport = this.exportSignal(format)();
    if (
      this.isCreatingFormat(format) ||
      (this.isExportRunning(existingExport) && !this.formatError(format))
    ) {
      return;
    }
    this.selectFormat(format);
    this.setFormatError(format, null);
    this.setCreatingFormat(format, true);
    this.errorMessage.set(null);
    this.reportGenerationService.startReportExport(this.reportGenerationId, format)
      .pipe(
        finalize(() => this.setCreatingFormat(format, false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (reportExport) => {
          this.exportSignal(format).set(reportExport);
          this.reportDraftStorage.saveExportId(
            this.reportGenerationId,
            format,
            reportExport.exportId,
          );
          if (reportExport.status !== 'READY') {
            this.pollExport(reportExport.exportId, format);
          }
        },
        error: (error: HttpErrorResponse) => this.setFormatError(format, this.problemMessage(error)),
      });
  }

  exportOrDownloadFormat(format: ReportExportFormat): void {
    const reportExport = this.reportExportFor(format);
    if (reportExport?.status === 'READY') {
      this.downloadExportFile(reportExport);
      return;
    }
    this.createExport(format);
  }

  reportExportFor(format: ReportExportFormat): ReportExport | null {
    return this.exportSignal(format)();
  }

  isCreatingFormat(format: ReportExportFormat): boolean {
    return this.creatingFormats().has(format);
  }

  formatError(format: ReportExportFormat): string | null {
    const requestError = this.formatErrors()[format];
    if (requestError) {
      return requestError;
    }
    return this.reportExportFor(format)?.status === 'FAILED'
      ? `La génération ${format === 'PDF' ? 'PDF' : 'Excel'} a échoué.`
      : null;
  }

  formatActionLabel(definition: ExportFormatDefinition): string {
    if (this.formatError(definition.format)) {
      return definition.retryLabel;
    }
    return this.reportExportFor(definition.format)?.status === 'READY'
      ? definition.downloadLabel
      : definition.exportLabel;
  }

  isFormatActionDisabled(format: ReportExportFormat): boolean {
    if (this.formatError(format)) {
      return false;
    }
    return this.isCreatingFormat(format) || this.isExportRunning(this.reportExportFor(format));
  }

  formatProgressMode(format: ReportExportFormat): 'determinate' | 'indeterminate' {
    return this.isCreatingFormat(format) && !this.reportExportFor(format)
      ? 'indeterminate'
      : 'determinate';
  }

  formatStatusLabel(format: ReportExportFormat): string {
    if (this.formatError(format)) {
      return 'Échec';
    }
    if (this.isCreatingFormat(format) && !this.reportExportFor(format)) {
      return 'En attente';
    }
    return this.exportStatusLabel(this.reportExportFor(format));
  }

  formatStatusSeverity(format: ReportExportFormat): ExportTagSeverity {
    if (this.formatError(format)) {
      return 'danger';
    }
    switch (this.reportExportFor(format)?.status) {
      case 'READY':
        return 'success';
      case 'PENDING':
      case 'RUNNING':
        return 'info';
      default:
        return 'secondary';
    }
  }

  workflowState(stepId: ExportWorkflowStepId): ExportWorkflowState {
    const generationStatus = this.reportGeneration()?.status;
    if (stepId === 'preparation') {
      if (generationStatus === 'FAILED' || generationStatus === 'EXPIRED') {
        return 'error';
      }
      return generationStatus === 'READY' ? 'completed' : 'active';
    }
    if (stepId === 'formats') {
      if (generationStatus !== 'READY') {
        return 'pending';
      }
      return this.availableExportOptions().length === 0 ? 'completed' : 'active';
    }
    return this.selectedExportOptions().length > 0 ? 'active' : 'pending';
  }

  downloadExportFile(reportExport: ReportExport): void {
    if (reportExport.status !== 'READY') {
      return;
    }
    this.reportGenerationService.downloadExportFile(reportExport.exportId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.saveBlob(response, reportExport.format),
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.problemMessage(error)),
      });
  }

  generationStatusLabel(reportGeneration: ReportGeneration): string {
    if (reportGeneration.status === 'PENDING') {
      return 'Préparation en attente';
    }
    if (reportGeneration.status === 'READY') {
      return 'Préparation terminée';
    }
    if (reportGeneration.status === 'FAILED') {
      return 'Échec de la préparation';
    }
    if (reportGeneration.status === 'EXPIRED') {
      return 'Rapport expiré';
    }
    switch (reportGeneration.phase) {
      case 'VALIDATING':
        return 'Validation du rapport';
      case 'COUNTING':
        return 'Comptage des lignes';
      case 'READING_ROWS':
        return 'Traitement des lignes';
      case 'FINALIZING':
        return 'Finalisation du rapport';
      default:
        return 'Préparation du rapport';
    }
  }

  exportStatusLabel(reportExport: ReportExport | null): string {
    switch (reportExport?.status) {
      case 'PENDING':
        return 'En attente';
      case 'RUNNING':
        return 'Génération en cours';
      case 'READY':
        return 'Prêt';
      case 'FAILED':
        return 'Échec de la génération';
      default:
        return 'À générer';
    }
  }

  isExportRunning(reportExport: ReportExport | null): boolean {
    return reportExport?.status === 'PENDING' || reportExport?.status === 'RUNNING';
  }

  returnToConfiguration(): void {
    this.deleteAndRestore();
  }

  startNewReport(): void {
    this.cleanupRequested = true;
    this.reportDraftStorage.clearExportIds(this.reportGenerationId);
    this.reportGenerationService.deleteGeneration(this.reportGenerationId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => void this.router.navigate(['/rapports']),
        error: () => void this.router.navigate(['/rapports']),
      });
  }

  private pollReportGeneration(): void {
    timer(0, EXPORT_POLLING_INTERVAL_MS).pipe(
      switchMap(() => this.reportGenerationService.getReportGeneration(this.reportGenerationId)),
      retry({
        delay: (error: HttpErrorResponse) => this.retryAfterTransientPollingError(error),
      }),
      takeWhile((reportGeneration) => !this.isGenerationTerminal(reportGeneration.status), true),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (reportGeneration) => {
        this.networkInterrupted.set(false);
        this.reportGeneration.set(reportGeneration);
        if (reportGeneration.status === 'EXPIRED') {
          void this.restoreConfiguration('Cette génération a expiré. Votre configuration a été restaurée.');
        }
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 404) {
          void this.restoreConfiguration('Cette génération a expiré. Votre configuration a été restaurée.');
          return;
        }
        this.errorMessage.set(this.problemMessage(error));
      },
    });
  }

  private pollExport(exportId: string, format: ReportExportFormat): void {
    timer(0, EXPORT_POLLING_INTERVAL_MS).pipe(
      switchMap(() => this.reportGenerationService.getReportExport(exportId)),
      retry({
        delay: (error: HttpErrorResponse) => this.retryAfterTransientPollingError(error),
      }),
      takeWhile((reportExport) => reportExport.status === 'PENDING' || reportExport.status === 'RUNNING', true),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (reportExport) => {
        this.networkInterrupted.set(false);
        this.setFormatError(
          format,
          reportExport.status === 'FAILED' ? this.failedExportMessage(reportExport) : null,
        );
        this.exportSignal(format).set(reportExport);
      },
      error: (error: HttpErrorResponse) => this.setFormatError(format, this.problemMessage(error)),
    });
  }

  private retryAfterTransientPollingError(error: HttpErrorResponse): Observable<number> {
    if (error.status !== 0 && error.status < 500) {
      return throwError(() => error);
    }
    this.networkInterrupted.set(true);
    return timer(TRANSIENT_POLLING_RETRY_DELAY_MS);
  }

  private resumeExports(): void {
    const savedExportIds = this.reportDraftStorage.loadExportIds(this.reportGenerationId);
    for (const format of ['PDF', 'XLSX'] as const) {
      const exportId = savedExportIds[format];
      if (exportId) {
        this.selectFormat(format);
        this.pollExport(exportId, format);
      }
    }
  }

  private exportSignal(format: ReportExportFormat) {
    return format === 'PDF' ? this.pdfExport : this.xlsxExport;
  }

  private failedExportMessage(reportExport: ReportExport): string {
    const displayNames = reportExport.unavailableElements?.map(({ displayName }) => displayName) ?? [];
    if (displayNames.length > 0) {
      return `Éléments devenus indisponibles : ${displayNames.join(', ')}.`;
    }
    return "L’export n’a pas pu être généré. Réessayez après avoir vérifié la configuration.";
  }

  private selectFormat(format: ReportExportFormat): void {
    this.selectedFormats.update((selectedExportFormats) => {
      const updatedSelectedFormats = new Set(selectedExportFormats);
      updatedSelectedFormats.add(format);
      return updatedSelectedFormats;
    });
  }

  private setCreatingFormat(format: ReportExportFormat, creating: boolean): void {
    this.creatingFormats.update((formatsBeingCreated) => {
      const updatedFormatsBeingCreated = new Set(formatsBeingCreated);
      if (creating) {
        updatedFormatsBeingCreated.add(format);
      } else {
        updatedFormatsBeingCreated.delete(format);
      }
      return updatedFormatsBeingCreated;
    });
  }

  private setFormatError(format: ReportExportFormat, message: string | null): void {
    this.formatErrors.update((exportErrors) => {
      const updatedExportErrors = { ...exportErrors };
      if (message) {
        updatedExportErrors[format] = message;
      } else {
        delete updatedExportErrors[format];
      }
      return updatedExportErrors;
    });
  }

  private isGenerationTerminal(status: ReportGeneration['status']): boolean {
    return status === 'READY' || status === 'FAILED' || status === 'EXPIRED';
  }

  private deleteAndRestore(): void {
    this.cleanupRequested = true;
    this.reportGenerationService.deleteGeneration(this.reportGenerationId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => void this.restoreConfiguration(),
        error: () => void this.restoreConfiguration(),
      });
  }

  private cleanupOnNavigation(): void {
    if (this.cleanupRequested || !this.reportGenerationId) {
      return;
    }
    this.cleanupRequested = true;
    this.reportDraftStorage.clearExportIds(this.reportGenerationId);
    this.reportGenerationService
      .deleteGeneration(this.reportGenerationId)
      .subscribe({ error: () => undefined });
  }

  private async restoreConfiguration(message?: string): Promise<void> {
    this.cleanupRequested = true;
    this.reportDraftStorage.clearExportIds(this.reportGenerationId);
    const draft = this.reportDraftStorage.load();
    if (!draft) {
      await this.router.navigate(['/rapports'], message ? { state: { reportMessage: message } } : undefined);
      return;
    }
    const relatedDatasetIds = [...draft.relatedDatasetIds].sort((a, b) => a - b).join(',');
    await this.router.navigate(['/rapports/configuration', draft.definition.rootDatasetId], {
      queryParams: relatedDatasetIds ? { relatedDatasetIds } : {},
      state: message ? { reportMessage: message } : undefined,
    });
  }

  private saveBlob(response: HttpResponse<Blob>, format: ReportExportFormat): void {
    if (!response.body) {
      this.errorMessage.set('Le fichier téléchargé est vide.');
      return;
    }
    const disposition = response.headers.get('Content-Disposition') ?? '';
    const match = /filename="?([^";]+)"?/i.exec(disposition);
    const fileName = match?.[1] ?? `rapport.${format === 'PDF' ? 'pdf' : 'xlsx'}`;
    const url = URL.createObjectURL(response.body);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  private problemMessage(error: HttpErrorResponse): string {
    const problem = error.error as ApiProblem | null;
    return problem && typeof problem === 'object' && typeof problem.detail === 'string'
      ? problem.detail
      : 'L’opération d’export a échoué.';
  }

  private rowCountLabel(rowCount: number): string {
    return `${rowCount} ligne${rowCount !== 1 ? 's' : ''}`;
  }
}
