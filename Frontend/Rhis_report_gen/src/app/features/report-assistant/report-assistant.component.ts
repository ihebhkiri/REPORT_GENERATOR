import {HttpErrorResponse} from '@angular/common/http';
import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {RouterLink} from '@angular/router';
import {ButtonModule} from 'primeng/button';
import {finalize} from 'rxjs';

import {BotReportService} from './bot-report.service';
import {BotReportRequest, BotReportResponse} from './report-assistant.model';

interface AssistantMessage {
  readonly id: number;
  readonly author: 'user' | 'assistant';
  readonly text: string;
}

interface ClarificationContext {
  readonly originalMessage: string;
  readonly question: string;
}

interface ReadyGeneration {
  readonly generationId: string;
  readonly summary: string;
}

@Component({
  selector: 'app-report-assistant',
  imports: [ButtonModule, RouterLink],
  templateUrl: './report-assistant.component.html',
  styleUrl: './report-assistant.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportAssistantComponent {
  private readonly service = inject(BotReportService);
  private nextMessageId = 2;

  readonly messages = signal<readonly AssistantMessage[]>([{
    id: 1,
    author: 'assistant',
    text: 'Décrivez le rapport souhaité, par exemple « Liste des employés » ou « Heures travaillées ce mois-ci ».',
  }]);
  readonly draftMessage = signal('');
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly lastHttpStatus = signal<number | null>(null);
  readonly sessionExpired = computed(() => this.lastHttpStatus() === 401);
  readonly readyGeneration = signal<ReadyGeneration | null>(null);
  private readonly clarificationContext = signal<ClarificationContext | null>(null);

  submit(): void {
    const answer = this.draftMessage().trim();
    if (this.isSubmitting() || answer.length === 0) return;

    const request = this.buildRequest(answer);
    if (this.requestLength(request) > 2000) {
      this.errorMessage.set('La demande avec son contexte dépasse 2000 caractères. Raccourcissez votre réponse.');
      return;
    }

    this.addMessage('user', answer);
    this.draftMessage.set('');
    this.errorMessage.set(null);
    this.lastHttpStatus.set(null);
    this.readyGeneration.set(null);
    this.isSubmitting.set(true);

    this.service.createReport(request).pipe(
      finalize(() => this.isSubmitting.set(false)),
    ).subscribe({
      next: (response) => this.handleResponse(response, request),
      error: (error: HttpErrorResponse) => {
        this.draftMessage.set(answer);
        this.handleHttpError(error);
      },
    });
  }

  handleComposerKeydown(event: KeyboardEvent): void {
    if (event.ctrlKey && event.key === 'Enter') {
      event.preventDefault();
      this.submit();
    }
  }

  private buildRequest(answer: string): BotReportRequest {
    const context = this.clarificationContext();
    return context === null
      ? {message: answer, format: 'XLSX'}
      : {
          message: context.originalMessage,
          format: 'XLSX',
          clarificationQuestion: context.question,
          clarificationAnswer: answer,
        };
  }

  private requestLength(request: BotReportRequest): number {
    return request.message.length
      + (request.clarificationQuestion?.length ?? 0)
      + (request.clarificationAnswer?.length ?? 0);
  }

  private handleResponse(response: BotReportResponse, request: BotReportRequest): void {
    if (response.status === 'NEEDS_CLARIFICATION' && response.question) {
      this.clarificationContext.set({originalMessage: request.message, question: response.question});
      this.addMessage('assistant', response.question);
      return;
    }
    if (response.status === 'READY' && response.generationId && response.format) {
      const summary = response.planSummary ?? 'Le rapport est prêt à être vérifié.';
      this.clarificationContext.set(null);
      this.readyGeneration.set({generationId: response.generationId, summary});
      this.addMessage('assistant', summary);
      return;
    }
    if (response.status === 'FAILED') {
      this.clarificationContext.set(null);
      this.addMessage('assistant', response.errors.join(' ') || 'La création du rapport a échoué.');
      return;
    }
    this.errorMessage.set('La réponse de l’assistant est incomplète.');
  }

  private handleHttpError(error: HttpErrorResponse): void {
    this.lastHttpStatus.set(error.status);
    const body = error.error as Partial<BotReportResponse> | null;
    if (error.status === 422 && body?.status === 'FAILED' && Array.isArray(body.errors)) {
      this.clarificationContext.set(null);
      this.addMessage('assistant', body.errors.join(' ') || 'La création du rapport a échoué.');
      return;
    }
    const messages: Record<number, string> = {
      400: 'La demande est invalide. Vérifiez votre texte.',
      401: 'Votre session a expiré. Reconnectez-vous.',
      502: 'L’assistant est temporairement indisponible. Réessayez.',
    };
    this.errorMessage.set(messages[error.status] ?? 'Impossible de contacter le serveur. Réessayez.');
  }

  private addMessage(author: AssistantMessage['author'], text: string): void {
    this.messages.update((messages) => [...messages, {id: this.nextMessageId++, author, text}]);
  }
}
