import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';

import { ReportExport } from '../../models/report-generation.model';
import { ReportDraftStorageService } from '../../services/report-draft-storage.service';
import { ReportGenerationService } from '../../services/report-generation.service';
import { routes } from '../../rapports.routes';
import { ExportComponent } from './export.component';

describe('ExportComponent', () => {
  let fixture: ComponentFixture<ExportComponent>;
  let component: ExportComponent;

  const generation = {
    generationId: '66a6b933-3bd8-4d9b-99ae-4eac2d95f73a',
    status: 'READY' as const,
    phase: 'FINALIZING' as const,
    progress: 100,
    processedRowCount: 50,
    totalRowCount: 50,
    createdAt: '2026-08-12T08:00:00Z',
    expiresAt: '2026-08-12T08:30:00Z',
    errorCode: null,
  };
  const reportGenerationService = {
    getReportGeneration: jasmine.createSpy().and.returnValue(of(generation)),
    startReportExport: jasmine.createSpy().and.callFake((_id: string, format: 'PDF' | 'XLSX') => of({
      exportId: `${format}-id`, generationId: generation.generationId, format,
      status: 'READY' as const, progress: 100, errorCode: null,
    })),
    getReportExport: jasmine.createSpy(),
    downloadExportFile: jasmine.createSpy(),
    deleteGeneration: jasmine.createSpy().and.returnValue(of(void 0)),
  };
  const reportDraftStorage = {
    loadExportIds: jasmine.createSpy().and.returnValue({}),
    saveExportId: jasmine.createSpy(),
    load: jasmine.createSpy().and.returnValue(null),
  };

  beforeEach(async () => {
    reportGenerationService.getReportGeneration.calls.reset();
    reportGenerationService.startReportExport.calls.reset();
    reportGenerationService.getReportExport.calls.reset();
    reportGenerationService.downloadExportFile.calls.reset();
    reportGenerationService.deleteGeneration.calls.reset();
    reportGenerationService.startReportExport.and.callFake((_id: string, format: 'PDF' | 'XLSX') => of({
      exportId: `${format}-id`, generationId: generation.generationId, format,
      status: 'READY' as const, progress: 100, errorCode: null,
    }));
    reportGenerationService.getReportExport.and.callFake((exportId: string) => {
      const format = exportId.startsWith('PDF') ? 'PDF' as const : 'XLSX' as const;
      return of({
        exportId,
        generationId: generation.generationId,
        format,
        status: 'READY' as const,
        progress: 100,
        errorCode: null,
      });
    });
    reportDraftStorage.loadExportIds.calls.reset();
    reportDraftStorage.loadExportIds.and.returnValue({});
    reportDraftStorage.saveExportId.calls.reset();
    reportDraftStorage.load.calls.reset();

    await TestBed.configureTestingModule({
      imports: [ExportComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ generationId: generation.generationId }) } },
        },
        { provide: ReportGenerationService, useValue: reportGenerationService },
        { provide: ReportDraftStorageService, useValue: reportDraftStorage },
      ],
    }).compileComponents();
  });

  it('polls by URL id and stops on a ready generation', fakeAsync(() => {
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
    tick(0);
    fixture.detectChanges();

    expect(reportGenerationService.getReportGeneration).toHaveBeenCalledOnceWith(generation.generationId);
    expect(component.reportGeneration()).toEqual(generation);
    tick(4_000);
    expect(reportGenerationService.getReportGeneration).toHaveBeenCalledTimes(1);
  }));

  it('retries report generation polling after a transient server failure', fakeAsync(() => {
    let generationRequestCount = 0;
    reportGenerationService.getReportGeneration.and.callFake(() => {
      generationRequestCount += 1;
      return generationRequestCount === 1
        ? throwError(() => new HttpErrorResponse({ status: 500 }))
        : of(generation);
    });
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;

    tick(0);

    expect(reportGenerationService.getReportGeneration).toHaveBeenCalledTimes(1);
    expect(component.networkInterrupted()).toBeTrue();

    tick(2_000);

    expect(reportGenerationService.getReportGeneration).toHaveBeenCalledTimes(2);
    expect(component.networkInterrupted()).toBeFalse();
    expect(component.reportGeneration()).toEqual(generation);
  }));

  it('retries export polling after a transient server failure', fakeAsync(() => {
    const pendingPdfExport: ReportExport = {
      exportId: 'PDF-pending',
      generationId: generation.generationId,
      format: 'PDF',
      status: 'PENDING',
      progress: 10,
      errorCode: null,
    };
    const readyPdfExport: ReportExport = {
      ...pendingPdfExport,
      status: 'READY',
      progress: 100,
    };
    let exportRequestCount = 0;
    reportGenerationService.startReportExport.and.returnValue(of(pendingPdfExport));
    reportGenerationService.getReportExport.and.callFake(() => {
      exportRequestCount += 1;
      return exportRequestCount === 1
        ? throwError(() => new HttpErrorResponse({ status: 500 }))
        : of(readyPdfExport);
    });
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
    tick(0);

    component.createExport('PDF');
    tick(0);

    expect(reportGenerationService.getReportExport).toHaveBeenCalledTimes(1);
    expect(component.networkInterrupted()).toBeTrue();

    tick(2_000);

    expect(reportGenerationService.getReportExport).toHaveBeenCalledTimes(2);
    expect(component.networkInterrupted()).toBeFalse();
    expect(component.pdfExport()).toEqual(readyPdfExport);
  }));

  it('creates PDF and XLSX independently from the same generation', fakeAsync(() => {
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
    tick(0);

    component.createExport('PDF');
    component.createExport('XLSX');

    expect(reportGenerationService.startReportExport.calls.allArgs()).toEqual([
      [generation.generationId, 'PDF'],
      [generation.generationId, 'XLSX'],
    ]);
    expect(component.pdfExport()?.status).toBe('READY');
    expect(component.xlsxExport()?.status).toBe('READY');
  }));

  it('moves only PDF to download immediately when its export action is clicked', fakeAsync(() => {
    const pdfRequest = new Subject<ReportExport>();
    reportGenerationService.startReportExport.and.returnValue(pdfRequest);
    fixture = TestBed.createComponent(ExportComponent);
    tick(0);
    fixture.detectChanges();

    const pdfAction = fixture.nativeElement.querySelector(
      '[data-testid="formats-action-PDF"] button',
    ) as HTMLButtonElement | null;
    expect(pdfAction).not.toBeNull();
    pdfAction?.click();
    fixture.detectChanges();

    expect(reportGenerationService.startReportExport).toHaveBeenCalledOnceWith(generation.generationId, 'PDF');
    expect(fixture.nativeElement.querySelector('[data-testid="formats-card-PDF"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="formats-card-XLSX"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="download-card-PDF"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="download-card-XLSX"]')).toBeNull();

    const pendingAction = fixture.nativeElement.querySelector(
      '[data-testid="download-action-PDF"] button',
    ) as HTMLButtonElement;
    expect(pendingAction.disabled).toBeTrue();
    expect(pendingAction.textContent).toContain('Exporter PDF');
  }));

  it('moves Excel independently and moves both cards after both formats are selected', fakeAsync(() => {
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
    tick(0);
    fixture.detectChanges();

    const excelAction = fixture.nativeElement.querySelector(
      '[data-testid="formats-action-XLSX"] button',
    ) as HTMLButtonElement;
    excelAction.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="formats-card-PDF"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="formats-card-XLSX"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="download-card-XLSX"]')).not.toBeNull();

    const pdfAction = fixture.nativeElement.querySelector(
      '[data-testid="formats-action-PDF"] button',
    ) as HTMLButtonElement;
    pdfAction.click();
    fixture.detectChanges();

    expect(component.availableExportOptions()).toEqual([]);
    expect(fixture.nativeElement.textContent).toContain('Tous les formats ont été sélectionnés');
    expect(fixture.nativeElement.querySelectorAll('[data-testid^="download-card-"]').length).toBe(2);
    expect(reportGenerationService.startReportExport.calls.allArgs()).toEqual([
      [generation.generationId, 'XLSX'],
      [generation.generationId, 'PDF'],
    ]);
  }));

  it('keeps the Option B action disabled until the selected file is ready', fakeAsync(() => {
    const pdfRequest = new Subject<ReportExport>();
    reportGenerationService.startReportExport.and.returnValue(pdfRequest);
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
    tick(0);
    fixture.detectChanges();

    (fixture.nativeElement.querySelector(
      '[data-testid="formats-action-PDF"] button',
    ) as HTMLButtonElement).click();
    fixture.detectChanges();

    const pendingCard = fixture.nativeElement.querySelector(
      '[data-testid="download-card-PDF"]',
    ) as HTMLElement;
    expect(pendingCard.querySelector('.p-progressbar-indeterminate')).not.toBeNull();
    expect(pendingCard.querySelector('.p-tag')).not.toBeNull();

    pdfRequest.next({
      exportId: 'PDF-id', generationId: generation.generationId, format: 'PDF',
      status: 'PENDING', progress: 12, errorCode: null,
    });
    pdfRequest.complete();
    fixture.detectChanges();

    let action = fixture.nativeElement.querySelector(
      '[data-testid="download-action-PDF"] button',
    ) as HTMLButtonElement;
    expect(action.disabled).toBeTrue();
    expect(action.textContent).toContain('Exporter PDF');
    expect(fixture.nativeElement.textContent).toContain('Prêt dans quelques secondes');

    component.pdfExport.set({
      exportId: 'PDF-id', generationId: generation.generationId, format: 'PDF',
      status: 'READY', progress: 100, errorCode: null,
    });
    fixture.detectChanges();

    action = fixture.nativeElement.querySelector(
      '[data-testid="download-action-PDF"] button',
    ) as HTMLButtonElement;
    expect(action.disabled).toBeFalse();
    expect(action.textContent).toContain('Télécharger PDF');
    expect(fixture.nativeElement.textContent).toContain('Fichier prêt');
  }));

  it('removes redundant ready indicators and keeps the download action fluid', fakeAsync(() => {
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
    tick(0);

    component.createExport('PDF');
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector(
      '[data-testid="download-card-PDF"]',
    ) as HTMLElement;
    const action = card.querySelector(
      '[data-testid="download-action-PDF"] button',
    ) as HTMLButtonElement;

    expect(card.querySelector('.p-progressbar')).toBeNull();
    expect(card.querySelector('.p-tag')).toBeNull();
    expect(card.textContent).toContain('Fichier prêt');
    expect(action.textContent).toContain('Télécharger PDF');
    expect(action.classList).toContain('p-button-fluid');
  }));

  it('keeps a request failure in download and retries only that format', fakeAsync(() => {
    reportGenerationService.startReportExport.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 500,
      error: { detail: 'Le PDF ne peut pas être généré.' },
    })));
    fixture = TestBed.createComponent(ExportComponent);
    tick(0);
    fixture.detectChanges();

    (fixture.nativeElement.querySelector(
      '[data-testid="formats-action-PDF"] button',
    ) as HTMLButtonElement).click();
    fixture.detectChanges();

    const failedCard = fixture.nativeElement.querySelector(
      '[data-testid="download-card-PDF"]',
    ) as HTMLElement;
    const retryAction = failedCard.querySelector(
      '[data-testid="download-action-PDF"] button',
    ) as HTMLButtonElement;
    expect(failedCard.textContent).toContain('Le PDF ne peut pas être généré.');
    expect(retryAction.textContent).toContain('Réessayer PDF');
    expect(retryAction.disabled).toBeFalse();
    expect(fixture.nativeElement.querySelector('[data-testid="formats-card-XLSX"]')).not.toBeNull();

    reportGenerationService.startReportExport.and.callFake((_id: string, format: 'PDF' | 'XLSX') => of({
      exportId: `${format}-retry`, generationId: generation.generationId, format,
      status: 'READY' as const, progress: 100, errorCode: null,
    }));
    retryAction.click();
    fixture.detectChanges();

    expect(reportGenerationService.startReportExport).toHaveBeenCalledTimes(2);
    expect(failedCard.textContent).toContain('Fichier prêt');
  }));

  it('keeps a polling failure in download with an enabled retry action', fakeAsync(() => {
    reportGenerationService.startReportExport.and.returnValue(of({
      exportId: 'PDF-pending', generationId: generation.generationId, format: 'PDF',
      status: 'PENDING', progress: 15, errorCode: null,
    }));
    reportGenerationService.getReportExport.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 404,
      error: { detail: 'L’export PDF est introuvable.' },
    })));
    fixture = TestBed.createComponent(ExportComponent);
    tick(0);
    fixture.detectChanges();

    (fixture.nativeElement.querySelector(
      '[data-testid="formats-action-PDF"] button',
    ) as HTMLButtonElement).click();
    tick(0);
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector(
      '[data-testid="download-card-PDF"]',
    ) as HTMLElement;
    const retryAction = card.querySelector(
      '[data-testid="download-action-PDF"] button',
    ) as HTMLButtonElement;
    expect(card.textContent).toContain('L’export PDF est introuvable.');
    expect(retryAction.textContent).toContain('Réessayer PDF');
    expect(retryAction.disabled).toBeFalse();
  }));

  it('restores only formats with saved export ids into download', fakeAsync(() => {
    reportDraftStorage.loadExportIds.and.returnValue({ PDF: 'PDF-resumed' });
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
    tick(0);
    fixture.detectChanges();

    expect(component.selectedExportOptions().map(({ format }) => format)).toEqual(['PDF']);
    expect(fixture.nativeElement.querySelector('[data-testid="download-card-PDF"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="formats-card-XLSX"]')).not.toBeNull();
    expect(reportGenerationService.getReportExport).toHaveBeenCalledWith('PDF-resumed');
  }));

  it('derives independent workflow states from generation and selected formats', fakeAsync(() => {
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
    tick(0);

    component.reportGeneration.set({
      ...generation,
      status: 'RUNNING',
      phase: 'READING_ROWS',
      progress: 71,
      processedRowCount: 32,
    });
    expect(component.workflowState('preparation')).toBe('active');
    expect(component.workflowState('formats')).toBe('pending');

    component.reportGeneration.set(generation);
    expect(component.workflowState('preparation')).toBe('completed');
    expect(component.workflowState('formats')).toBe('active');
    expect(component.workflowState('download')).toBe('pending');

    component.createExport('PDF');

    expect(component.availableExportOptions().map(({ format }) => format)).toEqual(['XLSX']);
    expect(component.selectedExportOptions().map(({ format }) => format)).toEqual(['PDF']);
    expect(component.workflowState('formats')).toBe('active');
    expect(component.workflowState('download')).toBe('active');
    expect(component.readyExportCount()).toBe(1);
  }));

  it('renders the report wizard and the three local export steps', fakeAsync(() => {
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
    tick(0);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Source de données');
    expect(text).toContain('Configuration');
    expect(text).toContain('Export');
    expect(text).toContain('Préparation');
    expect(text).toContain('Formats');
    expect(text).toContain('Téléchargement');
    expect(fixture.nativeElement.querySelector('p-stepper')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('p-timeline')).not.toBeNull();
    expect(fixture.nativeElement.querySelectorAll('[data-testid^="workflow-"]').length).toBe(3);
    expect(fixture.nativeElement.querySelector('.material-symbols-outlined')).toBeNull();
  }));

  it('renders PDF and Excel horizontally in the download step when both are ready', fakeAsync(() => {
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
    tick(0);
    component.createExport('PDF');
    component.createExport('XLSX');
    fixture.detectChanges();

    const formats = fixture.nativeElement.querySelectorAll('[data-testid^="download-card-"]');
    expect(formats.length).toBe(2);
    expect(formats[0].textContent).toContain('Télécharger PDF');
    expect(formats[1].textContent).toContain('Télécharger Excel');
    expect(fixture.nativeElement.querySelector('.pi-file-pdf')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.pi-file-excel')).not.toBeNull();
  }));

  it('uses the full timeline width and contains both format actions', fakeAsync(() => {
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
    tick(0);

    component.createExport('PDF');
    component.createExport('XLSX');
    fixture.detectChanges();

    const timelineEvent = fixture.nativeElement.querySelector(
      '.p-timeline-event',
    ) as HTMLElement;
    const opposite = timelineEvent.querySelector(
      '.p-timeline-event-opposite',
    ) as HTMLElement;
    const content = timelineEvent.querySelector(
      '.p-timeline-event-content',
    ) as HTMLElement;
    const grid = fixture.nativeElement.querySelector(
      '[data-testid="workflow-download"] .export-format-grid',
    ) as HTMLElement;
    const cards = Array.from(grid.querySelectorAll<HTMLElement>(
      '[data-testid^="download-card-"]',
    ));

    expect(getComputedStyle(opposite).display).toBe('none');
    expect(content.getBoundingClientRect().width)
      .toBeGreaterThan(timelineEvent.getBoundingClientRect().width * 0.85);
    for (const card of cards) {
      const cardBounds = card.getBoundingClientRect();
      const actionBounds = card.querySelector<HTMLButtonElement>('button')!
        .getBoundingClientRect();
      expect(actionBounds.left).toBeGreaterThanOrEqual(cardBounds.left);
      expect(actionBounds.right).toBeLessThanOrEqual(cardBounds.right);
    }
  }));

  it('stacks download cards below the responsive breakpoint without horizontal overflow', fakeAsync(() => {
    const initialWidth = window.outerWidth;
    const initialHeight = window.outerHeight;

    try {
      window.resizeTo(700, 800);
      fixture = TestBed.createComponent(ExportComponent);
      component = fixture.componentInstance;
      tick(0);

      component.createExport('PDF');
      component.createExport('XLSX');
      fixture.detectChanges();

      const host = fixture.nativeElement as HTMLElement;
      const cards = Array.from(host.querySelectorAll<HTMLElement>(
        '[data-testid^="download-card-"]',
      ));
      const firstBounds = cards[0].getBoundingClientRect();
      const secondBounds = cards[1].getBoundingClientRect();

      expect(window.innerWidth).toBeLessThan(760);
      expect(secondBounds.top).toBeGreaterThanOrEqual(firstBounds.bottom);
      expect(document.documentElement.scrollWidth)
        .toBeLessThanOrEqual(document.documentElement.clientWidth);
    } finally {
      window.resizeTo(initialWidth, initialHeight);
    }
  }));

  it('keeps independent actions when only one format is ready', fakeAsync(() => {
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
    tick(0);
    component.createExport('PDF');
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(component.selectedExportOptions().map(({ format }) => format)).toEqual(['PDF']);
    expect(text).toContain('Télécharger PDF');
    expect(text).toContain('Exporter Excel');
    expect(fixture.nativeElement.querySelector('[data-testid="formats-card-XLSX"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="download-card-XLSX"]')).toBeNull();
  }));

  it('keeps a failed generation on the preparation step with a recovery action', fakeAsync(() => {
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
    tick(0);
    component.reportGeneration.set({
      ...generation,
      status: 'FAILED',
      phase: 'FINALIZING',
      progress: 92,
      errorCode: 'REPORT_DEFINITION_UNAVAILABLE',
      unavailableElements: [
        { kind: 'FIELD', id: 42, displayName: 'Date de naissance', reason: 'FIELD_HIDDEN' },
      ],
    });
    fixture.detectChanges();

    const preparation = fixture.nativeElement.querySelector(
      '[data-testid="workflow-preparation"]',
    ) as HTMLElement;
    expect(component.workflowState('preparation')).toBe('error');
    expect(preparation.textContent).toContain('Préparation');
    expect(preparation.textContent).toContain('Date de naissance');
    expect(preparation.textContent).toContain('Revenir et réessayer');
  }));

  it('is registered with a generation id route', async () => {
    const exportRoute = routes.find((route) => route.path === 'export/:generationId');
    expect(exportRoute).toBeDefined();
    expect(await exportRoute?.loadComponent?.()).toBe(ExportComponent);
  });
});
