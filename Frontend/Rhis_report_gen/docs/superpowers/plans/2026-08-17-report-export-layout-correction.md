# Report Export Layout Correction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corriger la largeur utile, l’overflow, le responsive et la densité de la page Export sans modifier son workflow ni sa logique métier.

**Architecture:** Conserver le composant standalone et ses composants PrimeNG existants. Modifier uniquement les conditions de présentation du template, puis corriger le layout local en donnant toute la largeur disponible au contenu de `p-timeline` et en organisant chaque carte de format verticalement.

**Tech Stack:** Angular 20, TypeScript, templates Angular control flow, SCSS local, PrimeNG 20, Jasmine/Karma.

## Global Constraints

- Ne modifier ni `export.component.ts`, ni les services, modèles, routes, appels API, polling, stockage ou téléchargement Blob.
- Conserver `p-stepper`, `p-timeline`, `p-card`, `p-button`, `p-progressbar`, `p-message`, `p-tag` et PrimeIcons.
- N’ajouter aucune dépendance et n’utiliser ni Material Symbols ni utilities Tailwind dans la page Export.
- Laisser le marker de timeline strictement inchangé.
- Utiliser un axe de contenu fluide plafonné à `960px` et une grille responsive passant à une colonne sous `760px`.
- Conserver le SCSS du composant sous le budget Angular `anyComponentStyle` existant.
- Le dossier Export étant non suivi dans le worktree actuel, ne committer aucun fichier applicatif pendant l’exécution afin de ne pas capturer les modifications utilisateur préexistantes.
- Préserver les empreintes SHA-256 initiales : `export.component.ts` = `B1C435280BD0B5A2D1A6B1C5F6E312BC837EE313F7300B20A1784D1062709ECC`, `package.json` = `EFE222E0DFD28CE803DA79DC8400CE2023889B4DE13FDF5FE15094DF06AED3A5`, `package-lock.json` = `FF9A428A8495F0728629C7DA3B3E9B264645AF7048CB40AA84C597E650A81298`.

---

### Task 1: Alléger l’état READY et rendre les actions fluides

**Files:**
- Modify: `src/app/features/rapports/pages/export/export.component.spec.ts`
- Modify: `src/app/features/rapports/pages/export/export.component.html`

**Interfaces:**
- Consumes: `formatError(format)`, `isCreatingFormat(format)` et `reportExportFor(format)` déjà exposés par `ExportComponent`.
- Produces: un DOM dans lequel `.p-progressbar` n’existe que pour une création ou un export `PENDING/RUNNING`, le tag interne disparaît pour `READY`, et le bouton possède `.p-button-fluid`.

- [ ] **Step 1: Écrire le test de présentation READY en échec**

Ajouter ce test après le test `keeps the Option B action disabled until the selected file is ready` :

```typescript
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
```

Dans le test existant `keeps the Option B action disabled until the selected file is ready`, remplacer l’assertion globale sur la progress bar par une assertion limitée à la carte PDF :

```typescript
const pendingCard = fixture.nativeElement.querySelector(
  '[data-testid="download-card-PDF"]',
) as HTMLElement;
expect(pendingCard.querySelector('.p-progressbar-indeterminate')).not.toBeNull();
expect(pendingCard.querySelector('.p-tag')).not.toBeNull();
```

- [ ] **Step 2: Exécuter la spec Export et vérifier l’échec**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/features/rapports/pages/export/export.component.spec.ts
```

Expected: FAIL parce que la carte `READY` contient encore `.p-progressbar` et `.p-tag`, et que le bouton ne possède pas encore `.p-button-fluid`.

- [ ] **Step 3: Conditionner les indicateurs et rendre le bouton fluide**

Dans `#formatCard`, remplacer la condition du header par :

```html
@if (
  location === 'download' &&
  !formatError(definition.format) &&
  (
    isCreatingFormat(definition.format) ||
    reportExportFor(definition.format)?.status === 'PENDING' ||
    reportExportFor(definition.format)?.status === 'RUNNING'
  )
) {
  <ng-template #header>
    <p-progressbar
      role="progressbar"
      [attr.aria-label]="'Progression de la génération ' + definition.title"
      [mode]="formatProgressMode(definition.format)"
      [value]="reportExportFor(definition.format)?.progress ?? 0"
      [showValue]="false"
    />
  </ng-template>
}
```

Limiter le tag interne aux états autres que `READY` :

```html
@if (
  location === 'download' &&
  reportExportFor(definition.format)?.status !== 'READY'
) {
  <p-tag
    [value]="formatStatusLabel(definition.format)"
    [severity]="formatStatusSeverity(definition.format)"
  />
}
```

Ajouter l’input PrimeNG au bouton existant :

```html
[fluid]="true"
```

Ne changer aucune expression d’action, de label, de chargement ou de désactivation.

- [ ] **Step 4: Exécuter la spec Export et vérifier le passage au vert**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/features/rapports/pages/export/export.component.spec.ts
```

Expected: tous les tests Export passent ; les états de création et `PENDING` conservent leur progress bar, tandis que `READY` conserve seulement « Fichier prêt » et l’action de téléchargement.

- [ ] **Step 5: Contrôler le périmètre du premier checkpoint**

```powershell
git status --short -- src/app/features/rapports/pages/export
Select-String -Path src/app/features/rapports/pages/export/export.component.html -Pattern "p-progressbar|p-tag|fluid"
```

Expected: seuls le template et la spec ont été édités pendant cette tâche ; `export.component.ts` et le SCSS sont inchangés à ce checkpoint.

### Task 2: Donner toute la largeur au workflow et contenir les cartes

**Files:**
- Modify: `src/app/features/rapports/pages/export/export.component.spec.ts`
- Modify: `src/app/features/rapports/pages/export/export.component.scss`

**Interfaces:**
- Consumes: les classes existantes `.export-main`, `.export-intro`, `.report-stepper`, `.export-timeline`, `.export-format-grid`, `.export-format-card__row` et les `data-testid` des cartes/actions.
- Produces: un contenu de timeline occupant plus de 85 % de chaque événement, deux cartes alignées sur desktop, des actions contenues, puis une colonne sous `760px`.

- [ ] **Step 1: Écrire le test de largeur effective et de containment en échec**

Ajouter ce test après `renders PDF and Excel horizontally in the download step when both are ready` :

```typescript
it('uses the full timeline width and contains both desktop card actions', fakeAsync(() => {
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
  expect(cards[0].getBoundingClientRect().top).toBeCloseTo(
    cards[1].getBoundingClientRect().top,
    0,
  );

  for (const card of cards) {
    const cardBounds = card.getBoundingClientRect();
    const actionBounds = card.querySelector<HTMLButtonElement>('button')!
      .getBoundingClientRect();
    expect(actionBounds.left).toBeGreaterThanOrEqual(cardBounds.left);
    expect(actionBounds.right).toBeLessThanOrEqual(cardBounds.right);
  }
}));
```

Ajouter ensuite le test responsive suivant :

```typescript
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

    const cards = Array.from(fixture.nativeElement.querySelectorAll<HTMLElement>(
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
```

- [ ] **Step 2: Exécuter la spec Export et vérifier l’échec**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/features/rapports/pages/export/export.component.spec.ts
```

Expected: FAIL parce que `.p-timeline-event-opposite` occupe encore la moitié de la timeline, que les actions peuvent dépasser leurs cartes et que le breakpoint actuel ne force pas encore l’empilement à `700px`.

- [ ] **Step 3: Unifier le container et corriger la timeline**

Dans `export.component.scss` :

```scss
.export-main {
  box-sizing: border-box;
  width: 100%;
  max-width: 1040px;
  margin: 0 auto;
  padding: 32px 40px 28px;
}

.export-intro {
  display: flex;
  align-items: stretch;
  flex-direction: column;
  gap: 24px;
}

.report-stepper {
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
}

.export-timeline,
.export-page-actions,
.export-loading {
  width: 100%;
  max-width: none;
}

:host ::ng-deep .export-timeline .p-timeline-event {
  min-width: 0;
}

:host ::ng-deep .export-timeline .p-timeline-event-opposite {
  display: none;
  padding: 0;
}

:host ::ng-deep .export-timeline .p-timeline-event-content {
  min-width: 0;
  padding: 0 0 20px 16px;
  flex: 1 1 auto;
}
```

Avec `40px` de padding de chaque côté, `max-width: 1040px` donne exactement `960px` de largeur utile au container desktop. Supprimer les anciens `max-width: 960px` redondants de `.export-timeline`, `.export-page-actions` et `.export-loading`, ainsi que la media query `max-width: 900px` devenue inutile.

- [ ] **Step 4: Corriger la grille, la structure interne et le responsive**

Adapter les règles existantes sans créer de nouveau composant :

```scss
.export-workflow-card,
.export-format-card,
.export-format-card__copy {
  min-width: 0;
}

.export-format-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.export-format-card {
  display: block;
  height: 100%;
}

.export-format-card__row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: start;
  gap: 12px;
}

.export-format-card__copy {
  overflow-wrap: anywhere;
}

.export-format-card__row > p-button {
  display: block;
  min-width: 0;
  grid-column: 1 / -1;
}

:host ::ng-deep .export-format-card > .p-card {
  height: 100%;
}

@media (max-width: 760px) {
  .export-main,
  .export-topbar__inner {
    padding-right: 16px;
    padding-left: 16px;
  }

  .export-main {
    padding-top: 24px;
    padding-bottom: 20px;
  }

  .export-intro__title {
    font-size: 30px;
  }

  .export-format-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .export-page-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
```

Supprimer les anciennes règles mobiles de `.export-format-card__row`, car le bouton occupe désormais sa ligne dédiée à toutes les tailles.

- [ ] **Step 5: Exécuter la spec Export et vérifier le passage au vert**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/features/rapports/pages/export/export.component.spec.ts
```

Expected: tous les tests Export passent et le test géométrique confirme que le contenu utilise la largeur de la timeline et que les deux actions restent dans leurs cartes.

- [ ] **Step 6: Vérifier la suite complète et le budget de production**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless
npm.cmd run build
```

Expected: tous les tests Angular passent ; le build réussit sans warning `anyComponentStyle` pour `export.component.scss`. Le warning global préexistant du bundle initial peut rester sans modification de budget.

- [ ] **Step 7: Contrôler le périmètre final sans committer les fichiers non suivis**

Run:

```powershell
git status --short -- src/app/features/rapports/pages/export package.json package-lock.json
Select-String -Path src/app/features/rapports/pages/export/export.component.html -Pattern "material-symbols|class=.*(sm:|md:|lg:|bg-|text-|flex|grid)"
Get-FileHash src/app/features/rapports/pages/export/export.component.ts,package.json,package-lock.json
```

Expected: aucune nouvelle dépendance ou utility interdite ; les empreintes de `export.component.ts`, `package.json` et `package-lock.json` sont identiques aux empreintes relevées avant l’implémentation. Les fichiers applicatifs restent dans le worktree pour intégration par l’utilisateur.
