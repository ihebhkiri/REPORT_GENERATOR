import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';

import {BotReportService} from './bot-report.service';
import {BotReportResponse} from './report-assistant.model';
import {ReportAssistantComponent} from './report-assistant.component';

describe('ReportAssistantComponent', () => {
  const service = {createReport: jasmine.createSpy()};

  beforeEach(async () => {
    service.createReport.calls.reset();
    service.createReport.and.returnValue(of({status: 'READY', question: null,
      generationId: 'generation-1', format: 'XLSX', planSummary: 'Rapport employés', errors: []}));
    await TestBed.configureTestingModule({
      imports: [ReportAssistantComponent],
      providers: [provideRouter([]), {provide: BotReportService, useValue: service}],
    }).compileComponents();
  });

  it('renders the minimal message composer without a format selector', () => {
    const fixture = TestBed.createComponent(ReportAssistantComponent);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const textarea = element.querySelector('textarea');
    const sendButton = element.querySelector<HTMLButtonElement>('button[aria-label="Envoyer"]');

    expect(element.querySelector('select')).toBeNull();
    expect(textarea?.getAttribute('rows')).toBe('1');
    expect(textarea?.getAttribute('placeholder')).toBe('Envoyer un message…');
    expect(sendButton).not.toBeNull();
  });

  it('submits XLSX with Ctrl+Enter but keeps Enter for a new line', () => {
    const component = TestBed.createComponent(ReportAssistantComponent).componentInstance;
    component.draftMessage.set('Liste des employés');

    const enter = new KeyboardEvent('keydown', {key: 'Enter'});
    component.handleComposerKeydown(enter);
    expect(service.createReport).not.toHaveBeenCalled();

    const ctrlEnter = new KeyboardEvent('keydown', {key: 'Enter', ctrlKey: true});
    component.handleComposerKeydown(ctrlEnter);
    expect(service.createReport).toHaveBeenCalledOnceWith({
      message: 'Liste des employés',
      format: 'XLSX',
    });
  });

  it('shows Réflexion only while the assistant request is pending', () => {
    const pending = new Subject<BotReportResponse>();
    service.createReport.and.returnValue(pending);
    const fixture = TestBed.createComponent(ReportAssistantComponent);
    fixture.componentInstance.draftMessage.set('Liste des employés');
    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Réflexion');

    pending.next({status: 'READY', question: null, generationId: 'generation-1',
      format: 'XLSX', planSummary: 'Rapport employés', errors: []});
    pending.complete();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Réflexion');
  });

  it('ignores empty and duplicate submissions', () => {
    const component = TestBed.createComponent(ReportAssistantComponent).componentInstance;
    component.submit();
    const pending = new Subject<BotReportResponse>();
    service.createReport.and.returnValue(pending);
    component.draftMessage.set('Liste des employés');
    component.submit();
    component.draftMessage.set('Deuxième demande');
    component.submit();
    expect(service.createReport).toHaveBeenCalledTimes(1);
  });

  it('keeps clarification context in the next request', () => {
    service.createReport.and.returnValues(
      of({status: 'NEEDS_CLARIFICATION', question: 'Quel restaurant ?', generationId: null,
        format: null, planSummary: null, errors: []}),
      of({status: 'READY', question: null, generationId: 'generation-2', format: 'PDF',
        planSummary: 'Restaurant central', errors: []}),
    );
    const component = TestBed.createComponent(ReportAssistantComponent).componentInstance;
    component.draftMessage.set('Liste des employés');
    component.submit();
    component.draftMessage.set('Le restaurant central');
    component.submit();
    expect(service.createReport.calls.mostRecent().args[0]).toEqual({
      message: 'Liste des employés',
      format: 'XLSX',
      clarificationQuestion: 'Quel restaurant ?',
      clarificationAnswer: 'Le restaurant central',
    });
  });

  it('keeps the original request across multiple clarification questions', () => {
    service.createReport.and.returnValues(
      of({status: 'NEEDS_CLARIFICATION', question: 'Actifs ou tous ?', generationId: null,
        format: null, planSummary: null, errors: []}),
      of({status: 'NEEDS_CLARIFICATION', question: 'Quels champs ?', generationId: null,
        format: null, planSummary: null, errors: []}),
      of({status: 'READY', question: null, generationId: 'generation-3', format: 'XLSX',
        planSummary: 'Tous les contrats', errors: []}),
    );
    const component = TestBed.createComponent(ReportAssistantComponent).componentInstance;
    component.draftMessage.set('Liste des contrats');
    component.submit();
    component.draftMessage.set('Tous');
    component.submit();
    component.draftMessage.set('Nom et salaire');
    component.submit();

    expect(service.createReport.calls.mostRecent().args[0]).toEqual({
      message: 'Liste des contrats',
      format: 'XLSX',
      clarificationQuestion: 'Quels champs ?',
      clarificationAnswer: 'Nom et salaire',
    });
  });

  it('shows READY and maps session errors', () => {
    const fixture = TestBed.createComponent(ReportAssistantComponent);
    fixture.componentInstance.draftMessage.set('Liste des employés');
    fixture.componentInstance.submit();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector(
      'a[href="/rapports/export/generation-1"]')).not.toBeNull();

    service.createReport.and.returnValue(throwError(() => new HttpErrorResponse({status: 401})));
    fixture.componentInstance.draftMessage.set('Autre rapport');
    fixture.componentInstance.submit();
    expect(fixture.componentInstance.sessionExpired()).toBeTrue();
  });
});
