# rhis_bot Angular Interface Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Repository instructions forbid delegation unless the user explicitly requests it.

**Goal:** Ajouter une page Angular `/assistant` qui transforme une conversation locale en appels à `POST /api/v1/bot/reports`, gère la clarification et conduit l'utilisateur vers l'écran d'export existant.

**Architecture:** Une feature standalone `report-assistant` contient ses modèles, son service HTTP et son unique composant. Le composant garde l'état conversationnel dans des Signals ; le layout partagé fournit la navigation et les textes de page. Aucun store, historique persistant, streaming ou dépendance supplémentaire.

**Tech Stack:** Angular 20 standalone, TypeScript 5.9, Signals, RxJS 7.8, PrimeNG 20, Jasmine/Karma, SCSS.

## Global Constraints

- Branche unique `rhis_bot`, racine Git `C:/Users/Surface Pro/Downloads/RHIS`.
- Frontend : `Frontend/Rhis_report_gen`; documentation canonique : `RHIS/docs/superpowers`.
- Préserver tous les changements live-preview et fichiers indexés/non suivis préexistants.
- Ne pas modifier les composants `configuration/**` ni les flows 02/03.
- Ne pas ajouter de dépendance, store, facade, interceptor, persistance ou composant partagé.
- Utiliser `environment.apiBaseUrl`, `withCredentials: true` et un UUID d'idempotence.
- Format explicite `PDF|XLSX`, défaut UI `XLSX`; aucune extraction depuis la phrase.
- Limite de message finale : 2000 caractères, contexte de clarification compris.
- Aucun `aria-live`, conformément à la décision utilisateur ; conserver labels, focus visible et noms de boutons.
- Ne pas committer sans autorisation utilisateur explicite. Les étapes « checkpoint » préparent des diffs séparables mais ne lancent pas `git commit`.

---

## File Map

| Fichier | Action | Responsabilité |
| --- | --- | --- |
| `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.model.ts` | Créer | Contrats backend bot et types UI minimaux. |
| `Frontend/Rhis_report_gen/src/app/features/report-assistant/bot-report.service.ts` | Créer | POST authentifié avec idempotence. |
| `Frontend/Rhis_report_gen/src/app/features/report-assistant/bot-report.service.spec.ts` | Créer | Contrat HTTP. |
| `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.ts` | Créer | État Signals et orchestration conversationnelle. |
| `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.html` | Créer | Messages, formulaire, erreurs et lien génération. |
| `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.scss` | Créer | Mise en page desktop/mobile et focus. |
| `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.spec.ts` | Créer | Comportement du composant. |
| `Frontend/Rhis_report_gen/src/app/app.routes.ts` | Modifier | Route lazy `/assistant`. |
| `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.ts` | Modifier | Copie de page reports/datasets/assistant. |
| `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.html` | Modifier | Lien Assistant et textes dynamiques. |
| `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.spec.ts` | Modifier | Page assistant et navigation active. |
| `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.routes.spec.ts` | Modifier | Route lazy dans le layout partagé. |
| `RHIS/docs/superpowers/progress/2026-08-30-rhis-bot-angular-interface.md` | Créer | État exact, validations et handoff. |

### Task 0: Baseline Ponytail et état du worktree

**Files:**
- Create ignored artifact: `RHIS/target/ponytail-rhis-bot-angular-interface/baseline-dirty-files.json`

**Interfaces:**
- Produces: empreintes SHA256 des fichiers sales préexistants, comparées à la Task 4.

- [ ] **Step 1: Vérifier branche, état et diff**

Depuis `C:/Users/Surface Pro/Downloads/RHIS` :

```powershell
git branch --show-current
git status --porcelain=v1
git diff --stat
git diff --cached --stat
git log -5 --oneline
```

Attendu : branche `rhis_bot`; changements live-preview Angular, flows 02/03, suppressions
`javac.*.args` et documents du 24/08 présents. Aucun fichier `report-assistant` existant.

- [ ] **Step 2: Enregistrer les empreintes des fichiers préexistants**

Utiliser la liste de `git status --porcelain=v1`, exclure uniquement les nouveaux documents
`2026-08-30-rhis-bot-angular-interface*`, puis écrire pour chaque fichier existant :

```json
{
  "path": "chemin/relatif/a/la/racine/git",
  "sha256": "HASH_SHA256"
}
```

dans `RHIS/target/ponytail-rhis-bot-angular-interface/baseline-dirty-files.json`.

- [ ] **Step 3: Exécuter la baseline ciblée du layout**

Depuis `Frontend/Rhis_report_gen` :

```powershell
npm.cmd test -- --watch=false --include="src/app/shared/page-layout/*.spec.ts"
```

Enregistrer le nombre exact de succès/échecs. La baseline connue peut contenir l'échec de
libellé du layout reports ; ne pas le corriger implicitement.

### Task 1: Contrats bot et service HTTP

**Files:**
- Create: `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.model.ts`
- Create: `Frontend/Rhis_report_gen/src/app/features/report-assistant/bot-report.service.ts`
- Test: `Frontend/Rhis_report_gen/src/app/features/report-assistant/bot-report.service.spec.ts`

**Interfaces:**
- Produces: `ReportExportFormat`, `BotReportRequest`, `BotReportResponse`,
  `BotReportService.createReport(request): Observable<BotReportResponse>`.
- Consumes: `environment.apiBaseUrl`, `HttpClient`, `crypto.randomUUID()`.

- [ ] **Step 1: Écrire le test HTTP en échec**

Créer `bot-report.service.spec.ts` :

```typescript
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { BotReportService } from './bot-report.service';

describe('BotReportService', () => {
  let service: BotReportService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BotReportService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('posts an authenticated bot request with a generated idempotency key', () => {
    service.createReport({message: 'Liste des employés', format: 'XLSX'}).subscribe();

    const request = http.expectOne(`${environment.apiBaseUrl}/bot/reports`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({message: 'Liste des employés', format: 'XLSX'});
    expect(request.request.withCredentials).toBeTrue();
    expect(request.request.headers.get('Idempotency-Key'))
      .toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
    request.flush({
      status: 'READY', question: null, generationId: 'generation-1', format: 'XLSX',
      planSummary: 'Rapport employés', errors: [],
    });
  });
});
```

- [ ] **Step 2: Vérifier l'échec rouge**

```powershell
npm.cmd test -- --watch=false --include="src/app/features/report-assistant/bot-report.service.spec.ts"
```

Attendu : échec de compilation car `BotReportService` n'existe pas.

- [ ] **Step 3: Créer les contrats exacts**

Créer `report-assistant.model.ts` :

```typescript
export type ReportExportFormat = 'PDF' | 'XLSX';
export type BotReportStatus = 'READY' | 'NEEDS_CLARIFICATION' | 'FAILED';

export interface BotReportRequest {
  readonly message: string;
  readonly format: ReportExportFormat;
}

export interface BotReportResponse {
  readonly status: BotReportStatus;
  readonly question: string | null;
  readonly generationId: string | null;
  readonly format: ReportExportFormat | null;
  readonly planSummary: string | null;
  readonly errors: readonly string[];
}
```

- [ ] **Step 4: Implémenter le service minimal**

Créer `bot-report.service.ts` :

```typescript
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { BotReportRequest, BotReportResponse } from './report-assistant.model';

@Injectable({providedIn: 'root'})
export class BotReportService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/bot/reports`;

  createReport(request: BotReportRequest): Observable<BotReportResponse> {
    return this.http.post<BotReportResponse>(this.url, request, {
      headers: {'Idempotency-Key': crypto.randomUUID()},
      withCredentials: true,
    });
  }
}
```

- [ ] **Step 5: Vérifier le vert et le diff**

```powershell
npm.cmd test -- --watch=false --include="src/app/features/report-assistant/bot-report.service.spec.ts"
git diff --check -- Frontend/Rhis_report_gen/src/app/features/report-assistant
```

Attendu : 1/1 PASS. Checkpoint séparé prêt, sans commit tant que l'utilisateur ne l'autorise pas.

### Task 2: Composant conversationnel standalone

**Files:**
- Create: `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.ts`
- Create: `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.html`
- Create: `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.scss`
- Test: `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.spec.ts`

**Interfaces:**
- Consumes: `BotReportService.createReport()`, contrats de Task 1.
- Produces: composant standalone lazy-loadable `ReportAssistantComponent`.
- UI state: `messages`, `draftMessage`, `format`, `isSubmitting`, `errorMessage`,
  `clarificationContext`, `readyGeneration`.

- [ ] **Step 1: Écrire les tests de comportement en échec**

Créer `report-assistant.component.spec.ts` avec un service mocké et les cas suivants :

```typescript
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { BotReportService } from './bot-report.service';
import { BotReportResponse } from './report-assistant.model';
import { ReportAssistantComponent } from './report-assistant.component';

describe('ReportAssistantComponent', () => {
  let fixture: ComponentFixture<ReportAssistantComponent>;
  let component: ReportAssistantComponent;
  const service = {createReport: jasmine.createSpy()};

  beforeEach(async () => {
    service.createReport.calls.reset();
    service.createReport.and.returnValue(of({
      status: 'READY', question: null, generationId: 'generation-1', format: 'XLSX',
      planSummary: 'Rapport employés', errors: [],
    } satisfies BotReportResponse));
    await TestBed.configureTestingModule({
      imports: [ReportAssistantComponent],
      providers: [provideRouter([]), {provide: BotReportService, useValue: service}],
    }).compileComponents();
    fixture = TestBed.createComponent(ReportAssistantComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('ignores blank and duplicate submissions', () => {
    component.submit();
    expect(service.createReport).not.toHaveBeenCalled();

    const pending = new Subject<BotReportResponse>();
    service.createReport.and.returnValue(pending);
    component.draftMessage.set('Liste des employés');
    component.submit();
    component.draftMessage.set('Deuxième demande');
    component.submit();
    expect(service.createReport).toHaveBeenCalledTimes(1);
  });

  it('renders READY and exposes the existing export route', () => {
    component.draftMessage.set('Liste des employés');
    component.submit();
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    expect(host.textContent).toContain('Rapport employés');
    expect(host.querySelector('a[href="/rapports/export/generation-1"]')).not.toBeNull();
  });

  it('sends the initial request, clarification question and answer together', () => {
    service.createReport.and.returnValues(
      of({status: 'NEEDS_CLARIFICATION', question: 'Quel restaurant ?', generationId: null,
        format: null, planSummary: null, errors: []} satisfies BotReportResponse),
      of({status: 'READY', question: null, generationId: 'generation-2', format: 'PDF',
        planSummary: 'Restaurant central', errors: []} satisfies BotReportResponse),
    );
    component.draftMessage.set('Liste des employés');
    component.submit();
    component.draftMessage.set('Le restaurant central');
    component.submit();
    expect(service.createReport.calls.mostRecent().args[0].message).toBe(
      'Demande initiale : Liste des employés\n' +
      'Question de clarification : Quel restaurant ?\n' +
      'Réponse : Le restaurant central',
    );
  });

  it('maps a structured 422 response and a 401 session error', () => {
    service.createReport.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 422,
      error: {status: 'FAILED', question: null, generationId: null, format: null,
        planSummary: null, errors: ['Champ inconnu'] satisfies readonly string[]},
    })));
    component.draftMessage.set('Rapport invalide');
    component.submit();
    expect(component.messages().at(-1)?.text).toContain('Champ inconnu');

    service.createReport.and.returnValue(throwError(() => new HttpErrorResponse({status: 401})));
    component.draftMessage.set('Nouvelle demande');
    component.submit();
    expect(component.errorMessage()).toContain('session');
  });
});
```

Ajouter ensuite des tests ciblés pour : format `PDF`, message combiné >2000 caractères,
`Ctrl+Enter`, `Enter` seul et restauration du texte après erreur HTTP.

- [ ] **Step 2: Vérifier l'échec rouge**

```powershell
npm.cmd test -- --watch=false --include="src/app/features/report-assistant/report-assistant.component.spec.ts"
```

Attendu : échec de compilation car le composant n'existe pas.

- [ ] **Step 3: Implémenter l'état et l'orchestration minimale**

Créer `report-assistant.component.ts`. Utiliser ces types et règles :

```typescript
interface AssistantMessage {
  readonly id: number;
  readonly author: 'user' | 'assistant';
  readonly text: string;
}

interface ClarificationContext {
  readonly request: string;
  readonly question: string;
}

interface ReadyGeneration {
  readonly generationId: string;
  readonly format: ReportExportFormat;
  readonly summary: string;
}
```

Le composant importe `ButtonModule`, `RouterLink`, `finalize`, `HttpErrorResponse` et expose :

```typescript
readonly messages = signal<readonly AssistantMessage[]>([{
  id: 1,
  author: 'assistant',
  text: 'Décrivez le rapport que vous souhaitez créer.',
}]);
readonly draftMessage = signal('');
readonly format = signal<ReportExportFormat>('XLSX');
readonly isSubmitting = signal(false);
readonly errorMessage = signal<string | null>(null);
readonly clarificationContext = signal<ClarificationContext | null>(null);
readonly readyGeneration = signal<ReadyGeneration | null>(null);
```

Implémenter `submit()` selon cet ordre exact :

1. retourner si `isSubmitting()` ou si `draftMessage().trim()` est vide ;
2. construire le message final via `buildRequestMessage(answer)` ;
3. si le message final dépasse 2000 caractères, poser une erreur locale sans POST ;
4. ajouter le texte visible de l'utilisateur aux messages ;
5. vider la saisie, l'erreur et la génération prête, puis activer `isSubmitting` ;
6. appeler `createReport({message, format: this.format()})` ;
7. dans `finalize`, désactiver `isSubmitting` ;
8. dans `next`, appeler `handleResponse(response, requestMessage)` ;
9. dans `error`, restaurer la réponse visible dans la saisie et appeler `handleHttpError`.

Implémenter les helpers privés :

```typescript
private buildRequestMessage(answer: string): string {
  const context = this.clarificationContext();
  return context === null
    ? answer
    : `Demande initiale : ${context.request}\n` +
      `Question de clarification : ${context.question}\n` +
      `Réponse : ${answer}`;
}
```

`handleResponse()` doit :

- `NEEDS_CLARIFICATION` : ajouter la question et enregistrer `{request, question}` ;
- `READY` avec `generationId` et `format` non null : vider le contexte, enregistrer
  `readyGeneration` et ajouter le résumé ;
- `FAILED` : vider le contexte et ajouter `errors.join(' ')` ;
- toute forme incohérente : poser « La réponse de l’assistant est incomplète. ».

`handleHttpError()` doit reconnaître :

- `422` avec `error.error.status === 'FAILED'` et tableau `errors` : réutiliser le mapping FAILED ;
- `400` : « La demande est invalide. Vérifiez votre texte. » ;
- `401` : « Votre session a expiré. Reconnectez-vous. » ;
- `502` : « L’assistant est temporairement indisponible. Réessayez. » ;
- autre : « Impossible de contacter le serveur. Réessayez. ».

`handleComposerKeydown(event)` appelle `preventDefault()` puis `submit()` uniquement pour
`event.ctrlKey && event.key === 'Enter'`.

- [ ] **Step 4: Créer le template sans aria-live**

Créer `report-assistant.component.html` avec :

- `<section class="assistant-shell" aria-labelledby="assistant-title">` ;
- `<ol class="conversation">` et `@for (message of messages(); track message.id)` ;
- classes `message--user` / `message--assistant` ;
- exemples « Liste des employés… » et « Heures travaillées… » dans le message d'accueil ;
- `<textarea id="assistant-message" maxlength="2000">` avec label visible ;
- `<select id="assistant-format">` natif avec options XLSX/PDF ;
- `p-button` désactivé pour texte vide ou chargement ;
- bloc erreur avec `role="alert"`, sans `aria-live` ;
- lien `routerLink` vers `['/rapports/export', ready.generationId]` seulement si READY ;
- lien `/login` seulement pour l'erreur 401, représentée par un booléen dérivé
  `sessionExpired = computed(() => this.lastHttpStatus() === 401)`.

- [ ] **Step 5: Créer les styles scoped**

Créer `report-assistant.component.scss` avec une largeur `min(56rem, calc(100% - 2rem))`,
une carte blanche, une conversation scrollable sur desktop, des bulles limitées à 75%,
un formulaire en grid, des couleurs via `--p-surface-*`/`--p-primary-*`, un focus visible et
un media query `max-width: 48rem` passant les bulles à 90% et le formulaire sur une colonne.

Ne pas utiliser `::ng-deep`, animation, position fixed ou style global.

- [ ] **Step 6: Vérifier le vert et le diff**

```powershell
npm.cmd test -- --watch=false --include="src/app/features/report-assistant/*.spec.ts"
git diff --check -- Frontend/Rhis_report_gen/src/app/features/report-assistant
```

Attendu : tous les tests de la feature passent. Checkpoint séparé prêt, sans commit.

### Task 3: Route `/assistant` et layout partagé

**Files:**
- Modify: `Frontend/Rhis_report_gen/src/app/app.routes.ts`
- Modify: `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.ts`
- Modify: `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.html`
- Modify: `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.spec.ts`
- Modify: `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.routes.spec.ts`

**Interfaces:**
- Consumes: `ReportAssistantComponent` de Task 2.
- Produces: route lazy `/assistant`, lien de navigation et copie de page.

- [ ] **Step 1: Étendre les tests du layout en premier**

Dans `shared-page-layout.component.spec.ts` :

- étendre la boucle à `['reports', 'datasets', 'assistant']` ;
- toujours attendre le lien `/assistant` ;
- attendre `aria-current="page"` sur « Assistant » quand `page === 'assistant'` ;
- attendre le titre « Assistant de rapports » et la description validée pour cette page ;
- conserver les assertions admin et reports existantes sans les réécrire.

Dans `shared-page-layout.routes.spec.ts`, ajouter :

```typescript
it('loads the report assistant inside the shared layout', async () => {
  const harness = await RouterTestingHarness.create('/assistant');
  expect(TestBed.inject(Router).url).toBe('/assistant');
  expect(harness.routeNativeElement?.querySelectorAll('.shared-header').length).toBe(1);
  expect(harness.routeNativeElement?.querySelector('app-report-assistant')).not.toBeNull();
  expect(harness.routeNativeElement?.textContent).toContain('Assistant de rapports');
  expect(harness.routeNativeElement?.querySelector('a[aria-current="page"]')?.textContent)
    .toContain('Assistant');
});
```

- [ ] **Step 2: Exécuter et confirmer l'échec**

```powershell
npm.cmd test -- --watch=false --include="src/app/shared/page-layout/*.spec.ts"
```

Attendu : échec car la route/lien/copie assistant n'existent pas.

- [ ] **Step 3: Ajouter la route lazy**

Dans `app.routes.ts`, ajouter avant la route administration :

```typescript
{
  path: 'assistant',
  data: {page: 'assistant'},
  loadComponent: () =>
    import('./shared/page-layout/shared-page-layout.component').then(
      (m) => m.SharedPageLayoutComponent,
    ),
  children: [{
    path: '',
    loadComponent: () =>
      import('./features/report-assistant/report-assistant.component').then(
        (m) => m.ReportAssistantComponent,
      ),
  }],
},
```

- [ ] **Step 4: Généraliser uniquement la copie du layout**

Dans `shared-page-layout.component.ts`, définir :

```typescript
type LayoutPage = 'reports' | 'datasets' | 'assistant';

const PAGE_COPY: Record<LayoutPage, {readonly breadcrumb: string; readonly title: string;
  readonly description: string}> = {
  reports: {
    breadcrumb: 'Rapports · Source de données',
    title: 'Créer un rapport dynamique',
    description: 'Sélectionnez une source de données, configurez les colonnes et exportez votre rapport.',
  },
  datasets: {
    breadcrumb: 'Administration · Données',
    title: 'Exposition des données',
    description: 'Choisissez les tables et les champs proposés lors de la création des rapports.',
  },
  assistant: {
    breadcrumb: 'Rapports · Assistant',
    title: 'Assistant de rapports',
    description: 'Décrivez le rapport souhaité en français, puis vérifiez ce que l’assistant a compris.',
  },
};
```

Exposer `page`, `pageCopy`, `isDatasets` et `isAssistant`. Dans le HTML :

- ajouter le lien `/assistant` avec icône `pi-sparkles` ;
- poser `aria-current` sur le lien reports seulement pour `page === 'reports'` ;
- poser `aria-current` sur assistant seulement pour `page === 'assistant'` ;
- rendre `pageCopy.breadcrumb`, `pageCopy.title`, `pageCopy.description` ;
- conserver l'affichage conditionnel admin existant.

- [ ] **Step 5: Vérifier routes, layout et feature ensemble**

```powershell
npm.cmd test -- --watch=false --include="src/app/shared/page-layout/*.spec.ts" --include="src/app/features/report-assistant/*.spec.ts"
```

Attendu : nouveaux tests verts ; tout échec baseline antérieur doit être comparé au résultat
de Task 0, pas corrigé sans lien avec assistant.

### Task 4: Validation complète, empreintes et documentation

**Files:**
- Create: `RHIS/docs/superpowers/progress/2026-08-30-rhis-bot-angular-interface.md`
- Create ignored artifact: `RHIS/target/ponytail-rhis-bot-angular-interface/after-dirty-files.json`

**Interfaces:**
- Consumes: Tasks 0-3.
- Produces: résultat vérifiable et prochain handoff.

- [ ] **Step 1: Exécuter la suite frontend complète**

Depuis `Frontend/Rhis_report_gen` :

```powershell
npm.cmd test -- --watch=false
```

Enregistrer succès, failures, skips et éventuel problème ChromeHeadless. Comparer chaque
échec à la baseline connue ; un nouvel échec assistant/layout est une régression.

- [ ] **Step 2: Construire la production**

```powershell
npm.cmd run build
```

Attendu : build réussi. Enregistrer exactement les warnings de budget sans les masquer.

- [ ] **Step 3: Contrôler le diff et les secrets**

Depuis la racine Git :

```powershell
git diff --check
git status --short --branch
git diff --stat
git diff -- Frontend/Rhis_report_gen/src/app/features/report-assistant Frontend/Rhis_report_gen/src/app/app.routes.ts Frontend/Rhis_report_gen/src/app/shared/page-layout
```

Vérifier qu'aucune clé Mistral, donnée personnelle ou fichier généré n'est présent.

- [ ] **Step 4: Comparer les empreintes Ponytail**

Recalculer les SHA256 de tous les chemins de
`baseline-dirty-files.json`, écrire `after-dirty-files.json`, puis comparer par chemin.
Attendu : aucune différence pour les fichiers live-preview, flows 02/03, suppressions
préexistantes et documents du 24/08. Si un fichier a changé concurremment, le préserver et
documenter le mismatch ; ne jamais restaurer son contenu.

- [ ] **Step 5: Écrire la progression canonique**

Créer `RHIS/docs/superpowers/progress/2026-08-30-rhis-bot-angular-interface.md` avec :

- branche, HEAD initial/final et chemins sales préexistants ;
- fichiers frontend créés/modifiés ;
- décisions Ponytail (pas de store, persistance, streaming, dépendance, `aria-live`) ;
- tableau des commandes ciblées, suite complète et build avec résultats réels ;
- résultat des empreintes ;
- E2E réel marqué `SKIPPED` tant qu'une clé Mistral renouvelée et un compte local ne sont
  pas disponibles ;
- prochaine action exacte.

- [ ] **Step 6: Revue finale sans commit implicite**

Relire les diffs exacts, confirmer loading/empty/error/disabled/success/mobile/focus, puis
présenter le résultat à l'utilisateur. Ne committer que si l'utilisateur donne ensuite une
autorisation explicite de commit.

## Completion Criteria

- `/assistant` est lazy-loadée dans le layout partagé et visible dans la navigation.
- Un texte vide ou un double clic ne produit aucun POST.
- Chaque POST contient `message`, `format`, cookies et un UUID d'idempotence.
- `NEEDS_CLARIFICATION` conserve et renvoie le contexte côté Angular sans mémoire serveur.
- `READY` affiche le résumé et un bouton vers `/rapports/export/{generationId}` sans
  navigation automatique.
- `FAILED`, `400`, `401`, `502` et réseau sont distingués.
- Aucun `aria-live`, store, persistance, streaming, dépendance ou modification backend.
- Tests ciblés et build réussis ; suite complète sans nouvelle régression par rapport à la baseline.
- Empreintes des changements préexistants inchangées ou mismatch concurrent documenté.
- Aucun commit, push ou merge sans autorisation explicite.
