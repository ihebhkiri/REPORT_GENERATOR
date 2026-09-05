# Configuration Reference Visual Clone Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reproduire fidèlement le contenu principal de la page Configuration d’après la capture 1664 × 970, sans modifier le header, la sidebar ni le comportement fonctionnel.

**Architecture:** Conserver la structure de `SharedPageLayoutComponent` et la composition Angular actuelle. Adapter les trois textes `PAGE_COPY.configuration`, puis modifier seulement les templates et SCSS locaux des composants existants ; ne toucher à aucune règle fonctionnelle.

**Tech Stack:** Angular 20.3.26, TypeScript 5.9, PrimeNG 20.4.0, PrimeIcons 8, Tailwind/PostCSS, SCSS.

## Global Constraints

- Viewport desktop de référence : 1664 × 970.
- Conserver le header et la sidebar existants sans modification, duplication ou nouvelle navigation.
- Conserver services, API, modèles, formulaires, validations, événements, routing et règles métier.
- Conserver le responsive mobile/tablette existant, dont les tabs Configuration/Aperçu sous 768 px.
- Aucun nouveau composant, package, service ou niveau d’abstraction.
- Réutiliser les tokens et bibliothèques installés ; aucune modification globale de thème.
- Ne pas créer de commit sans demande explicite.

Design: `docs/superpowers/specs/2026-09-02-configuration-reference-visual-clone-design.md`  
Research: `../../docs/research/2026-09-02-configuration-reference-visual-clone.md`

---

### Task 1: Verrouiller les invariants de composition

**Files:**
- Modify: `src/app/features/rapports/pages/configuration/configuration.component.spec.ts`
- Verify unchanged: `src/app/features/rapports/rapports.routes.ts`
- Modify: `src/app/shared/page-layout/shared-page-layout.component.spec.ts`
- Verify unchanged: `src/app/shared/page-layout/shared-page-layout.component.html`

**Interfaces:**
- Consumes: route `/rapports/configuration/:datasetId`, `SharedPageLayoutComponent`, composants enfants actuels.
- Produces: assertions de non-régression sur le titre et la composition.

- [ ] **Step 1: Ajouter les assertions structurelles**

```ts
expect(fixture.nativeElement.querySelector('app-report-steps')).not.toBeNull();
expect(fixture.nativeElement.querySelector('app-column-selector')).not.toBeNull();
expect(fixture.nativeElement.querySelector('app-filter-editor')).not.toBeNull();
expect(fixture.nativeElement.querySelector('app-sort-editor')).not.toBeNull();
expect(fixture.nativeElement.querySelector('app-preview-panel')).not.toBeNull();
```

- [ ] **Step 2: Vérifier l’échec initial attendu**

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/configuration.component.spec.ts"
```

Expected: la composition existante est trouvée. Le test du texte de page est porté par `SharedPageLayoutComponent`.

### Task 2: Reconstruire la hiérarchie du contenu principal

**Files:**
- Modify: `src/app/features/rapports/pages/configuration/configuration.component.html`
- Modify: `src/app/features/rapports/pages/configuration/configuration.component.scss`
- Modify: `src/app/shared/page-layout/shared-page-layout.component.ts` (`PAGE_COPY.configuration` uniquement)
- Modify: `src/app/shared/page-layout/shared-page-layout.component.spec.ts`
- Modify: `src/app/shared/report-steps/report-steps.component.scss`
- Test: `src/app/features/rapports/pages/configuration/configuration.component.spec.ts`

**Interfaces:**
- Consumes: `selectedCount()`, `filtersValid()`, `canGenerate()`, `continueToExport()`, `mobileTab()` et tous les composants enfants actuels.
- Produces: même flux fonctionnel dans une composition desktop conforme.

- [ ] **Step 1: Adapter le texte d’introduction existant**

```ts
configuration: {
  breadcrumb: 'Rapports · Créer un rapport dynamique',
  title: 'Créer un rapport dynamique',
  description: 'Sélectionnez une source de données, configurez les colonnes et exportez votre rapport.',
},
```

Ne modifier ni le template ni la structure du layout partagé.

- [ ] **Step 2: Créer la card pleine largeur du stepper**

Conserver `<app-report-steps [stepValue]="2" />`, supprimer la limite `max-w-4xl` et appliquer une card locale. Ajuster les variables PrimeNG locales pour des cercles de 40–44 px, des séparateurs fins et l’accent indigo de la référence.

- [ ] **Step 3: Aligner la grille desktop**

Utiliser au-dessus de 1024 px `grid-template-columns: minmax(0, 1.7fr) minmax(20rem, 1fr)` avec un gap de 16–20 px. Conserver une pile sous ce breakpoint et les tabs sous 768 px.

- [ ] **Step 4: Recomposer l’aide et les actions**

Conserver conditions et handlers. Ajouter l’icône d’information, placer Précédent avant l’action primaire et présenter celle-ci comme « Suivant » :

```html
<a routerLink="/rapports" class="secondary-action">Précédent</a>
<p-button
  [label]="isGenerating() ? 'Démarrage…' : 'Suivant'"
  [disabled]="!canGenerate()"
  (onClick)="continueToExport()"
/>
```

- [ ] **Step 5: Exécuter le test ciblé**

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/configuration.component.spec.ts"
```

Expected: PASS ; composants enfants, liens et handler de génération toujours présents.

### Task 3: Harmoniser Colonnes, Filtres et Ordre de tri

**Files:**
- Modify: `src/app/features/rapports/pages/configuration/components/column-selector/column-selector.component.html`
- Modify: `src/app/features/rapports/pages/configuration/components/column-selector/column-selector.component.scss`
- Modify: `src/app/features/rapports/pages/configuration/components/filter-editor/filter-editor.component.html`
- Modify: `src/app/features/rapports/pages/configuration/components/sort-editor/sort-editor.component.html`
- Test: specs existantes des trois composants.

**Interfaces:**
- Consumes: bindings et événements actuels de sélection, drag/drop, filtres et tris.
- Produces: mêmes interactions dans des cards visuellement harmonisées.

- [ ] **Step 1: Ajuster la card Colonnes**

Conserver les deux panneaux et leurs contrôles. Aligner les titres à 16 px, labels à 12 px, recherche à environ 44 px, hauteur vide desktop à 190–250 px, séparations, badge et empty state sur la référence. Employer des classes locales uniquement lorsque les utilitaires actuels ne suffisent pas.

- [ ] **Step 2: Ajuster Filtres et Ordre de tri**

Conserver formulaires et contrôles. Harmoniser header, bordure, rayon, ombre, padding et empty state ; garder une hauteur compacte à vide et une croissance naturelle quand des lignes sont ajoutées.

- [ ] **Step 3: Vérifier les états interactifs**

Préserver hover, focus visible, disabled, erreurs, drag/drop, labels `sr-only`, attributs ARIA et `prefers-reduced-motion`.

- [ ] **Step 4: Exécuter les tests ciblés**

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/components/column-selector/column-selector.component.spec.ts" --include="src/app/features/rapports/pages/configuration/components/filter-editor/filter-editor.component.spec.ts" --include="src/app/features/rapports/pages/configuration/components/sort-editor/sort-editor.component.spec.ts"
```

Expected: PASS ; sélection, suppression, réordonnancement et validation conservent leurs résultats.

### Task 4: Rapprocher l’aperçu de la référence

**Files:**
- Modify: `src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.html`
- Modify: `src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.scss`
- Test: `src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.spec.ts`

**Interfaces:**
- Consumes: `status()`, `incompleteMessage()`, `collapsed()`, `response()`, `loading()`, `error()`, `stale()` et le slot `preview-actions`.
- Produces: mêmes états avec titre, badge et empty state fidèles.

- [ ] **Step 1: Exposer le titre et le statut**

```html
<h2 id="preview-title">Aperçu en temps réel</h2>
<span class="preview-status">{{ status() }}</span>
```

Conserver tous les messages conditionnels et le slot d’action.

- [ ] **Step 2: Ajuster panneau et état vide**

Aligner padding, rayon, ombre et hauteur sur la référence. Garder le tableau PrimeNG et tous les états loading/error/stale. Dans l’état incomplet, conserver l’icône et le message sans changer la condition.

- [ ] **Step 3: Exécuter le test ciblé**

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.spec.ts"
```

Expected: PASS ; titre, statut, réduction et états du preview sont observables.

### Task 5: Vérification intégrée et revue

**Files:**
- Verify: fichiers modifiés dans Tasks 1–4.
- Verify unchanged: structure/template/styles de `src/app/shared/page-layout/**`, `src/app/features/rapports/rapports.routes.ts`, services, modèles et TypeScript métier. Seuls les textes `PAGE_COPY.configuration` changent dans le layout.
- Update: ce plan.

**Interfaces:**
- Consumes: résultat des tâches précédentes.
- Produces: preuve automatisée et visuelle de conformité.

- [ ] **Step 1: Exécuter les tests Configuration**

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/**/*.spec.ts"
```

Expected: PASS sans régression fonctionnelle.

- [ ] **Step 2: Exécuter le build**

```powershell
npm.cmd run build
```

Expected: exit 0 ; consigner les éventuels warnings préexistants.

- [ ] **Step 3: Contrôler visuellement**

Comparer à 1664 × 970 : introduction, stepper, grille, proportions, espacements, alignements, typographie, couleurs, bordures, rayons, ombres, badges, icônes, densité et états actifs. Vérifier aussi 768 px, 390 px, zoom 200 %, navigation Tab/Shift+Tab, tabs mobiles et réduction de l’aperçu.

- [ ] **Step 4: Contrôler le diff**

```powershell
git -c safe.directory="C:/Users/Surface Pro/Downloads/RHIS" diff --check
git -c safe.directory="C:/Users/Surface Pro/Downloads/RHIS" status --short
git -c safe.directory="C:/Users/Surface Pro/Downloads/RHIS" diff -- Frontend/Rhis_report_gen
```

Expected: aucun changement du layout partagé, routing, services, modèles ou logique métier ; aucune duplication du header/sidebar.

## Progress

- [x] 2026-09-02 — Recherche terminée.
- [x] 2026-09-02 — Design validé.
- [x] 2026-09-02 — Plan rédigé ; approbation d’exécution en attente.
- [x] 2026-09-02 — Tasks 1–4 terminées ; 69 tests ciblés passent.
- [~] 2026-09-02 — Task 5 partielle : build et contrôles visuels passent, suite complète bloquée par 10 tests hors périmètre.

## Surprises & Discoveries

- 2026-09-02 — La composition existante correspond déjà globalement à la référence ; aucune nouvelle abstraction n’est nécessaire.
- 2026-09-02 — Le preview est replié par défaut sur desktop alors que la référence montre son contenu vide. Préserver le comportement reste prioritaire ; toute évolution de cet état devra être limitée à la présentation et validée par les tests.
- 2026-09-02 — La suite complète contient 10 échecs hors flux Configuration : 1 dans `ExportComponent`, 4 dans `DatasetExposureComponent` et 5 attentes de navigation obsolètes dans `SharedPageLayoutComponent`. Les 69 tests directement concernés passent.
- 2026-09-02 — Le backend local n’étant pas disponible, la comparaison navigateur a validé le shell, le stepper, l’état d’erreur et l’aperçu vide ; la grille chargée a été couverte par les tests composants mais pas par une capture end-to-end alimentée.

## Decision Log

- 2026-09-02 — **Decision:** conserver la structure de `SharedPageLayoutComponent` et modifier uniquement `PAGE_COPY.configuration`.
  - Reason: l’introduction appartient déjà au contenu principal fourni par le layout ; la dupliquer serait incorrect.
  - Alternatives rejected: recréer l’introduction dans `ConfigurationComponent`, ce qui produirait deux titres et deux breadcrumbs.
- 2026-09-02 — **Decision:** adapter les composants existants sans wrapper générique.
  - Reason: ils portent déjà les responsabilités nécessaires.
  - Alternatives rejected: nouveaux composants de card ou navigation sans réutilisation réelle.
- 2026-09-02 — **Decision:** ouvrir l’aperçu par défaut sur desktop.
  - Reason: la référence montre explicitement son état vide et la commande Réduire ; le toggle et les mises à jour automatiques restent inchangés et testés.
  - Alternatives rejected: conserver l’état initial replié, qui masquerait une partie importante de la référence.

## Outcomes & Retrospective

- Delivered behavior: contenu principal rapproché de la référence, stepper pleine largeur, grille et cards compactes, actions Précédent/Suivant, aperçu en temps réel ouvert avec statut et compte de lignes. Header/sidebar inchangés.
- Commands run and results: 69 tests ciblés PASS ; `npm.cmd run build` PASS avec warnings de budgets préexistants ; suite complète 158 PASS / 10 FAIL hors périmètre ; `git diff --check` sans erreur.
- Deviations from the approved plan: les textes d’introduction ont été adaptés dans `PAGE_COPY.configuration` car ils étaient déjà fournis par le layout ; aucun breadcrumb/titre dupliqué dans la page.
- Remaining risks or unverified checks: absence de backend local, donc pas de capture end-to-end de la grille avec données réelles ; 10 tests globaux restent rouges hors périmètre.
- Required follow-up: corriger séparément les tests Export, DatasetExposure et les attentes de navigation du layout si une branche totalement verte est exigée.
- Exact next action if incomplete: fournir un backend local ou un environnement intégré pour une dernière comparaison de l’état nominal chargé.
