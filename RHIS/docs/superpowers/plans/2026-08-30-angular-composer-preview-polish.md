# Angular Composer, Preview and Shared Layout Implementation Plan


**Goal:** Moderniser RHIS Bot, replier et animer l’aperçu de configuration, puis placer les pages configuration et export sous l’unique layout partagé.

**Architecture:** Les états existants restent dans leurs composants propriétaires. Le composer et l’indicateur sont dérivés de `draftMessage` et `isSubmitting`; l’aperçu conserve `previewCollapsed` dans `ConfigurationComponent`; les routes enfants délèguent uniquement leur chrome de page à `SharedPageLayoutComponent`.

**Tech Stack:** Angular 20 standalone, TypeScript 5.9, Signals, PrimeNG 20, HTML, SCSS, Jasmine/Karma.

## Global Constraints

- Ne modifier aucun fichier backend, contrat HTTP ou comportement métier.
- Conserver `format: 'XLSX'` dans `BotReportRequest`, sans contrôle ou texte de format dans `/assistant`.
- Préserver `draftMessage`, `isSubmitting`, les clarifications, `submit()`, les URLs et les paramètres de routes.
- Aucune nouvelle dépendance, abstraction partagée ou composant supplémentaire.
- Préserver tous les changements non liés déjà présents dans le worktree.
- Ne pas commit, push, merge ou rebase.

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.ts` | Modify | Requête XLSX technique sans signal de sélection |
| `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.html` | Modify | Capsule, textarea, bouton et indicateur temporaire |
| `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.scss` | Modify | Croissance, états visuels et animation accessible |
| `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.spec.ts` | Modify | Contrat DOM, loading et raccourci clavier |
| `Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration/configuration.component.ts` | Modify | Aperçu replié initialement |
| `Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration/configuration.component.html` | Modify | Suppression du header local et bouton natif de repli |
| `Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration/configuration.component.scss` | Modify | Hauteur sous layout partagé et chevron |
| `Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration/configuration.component.spec.ts` | Modify | État initial et intégration du repli |
| `Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.html` | Modify | Header compact et structure grid animable |
| `Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.scss` | Modify | Transition grid/opacité et reduced motion |
| `Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.spec.ts` | Modify | Classes collapsed/expanded et contenu |
| `Frontend/Rhis_report_gen/src/app/features/rapports/pages/export/export.component.html` | Modify | Suppression topbar et titre locaux |
| `Frontend/Rhis_report_gen/src/app/features/rapports/pages/export/export.component.scss` | Modify | Suppression des styles de topbar et hauteur viewport redondante |
| `Frontend/Rhis_report_gen/src/app/features/rapports/rapports.routes.ts` | Modify | Layout partagé autour des trois routes rapports |
| `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.ts` | Modify | Copies configuration/export et état nav Rapports |
| `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.html` | Modify | `aria-current` Rapports pour ses sous-pages |
| `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.spec.ts` | Modify | Variantes de page et navigation active |
| `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.routes.spec.ts` | Modify | Un seul header/titre sur configuration/export |

### Task 1: Composer RHIS Bot et attente assistant

**Interfaces:**
- Consumes: `BotReportRequest`, `draftMessage`, `isSubmitting`, `submit()` et `handleComposerKeydown()` existants.
- Produces: un body toujours compatible `{message, format: 'XLSX'}` et un DOM sans sélecteur de format.

- [ ] Ajouter aux tests du composant les assertions suivantes : aucun `select`, `textarea[rows="1"]`, placeholder exact, bouton nommé « Envoyer », body XLSX, `Ctrl+Enter` envoyé, `Enter` non envoyé, et message « Réflexion » visible uniquement pendant la requête.
- [ ] Exécuter `npm.cmd test -- --watch=false --include="src/app/features/report-assistant/report-assistant.component.spec.ts"` depuis `Frontend/Rhis_report_gen` et constater l’échec des nouvelles attentes DOM.
- [ ] Supprimer `ReportExportFormat` et le signal `format` du composant, puis retourner `format: 'XLSX'` dans les deux branches de `buildRequest()` et dans la requête technique du chemin `422`.
- [ ] Remplacer le formulaire actuel par une capsule contenant `textarea rows="1"` et le `p-button` circulaire avec `ariaLabel="Envoyer"`, tout en conservant les bindings et le raccourci existants.
- [ ] Rendre un `<li>` assistant conditionnel sur `isSubmitting()` avec « Réflexion » et trois spans de points `aria-hidden="true"`.
- [ ] Ajouter les styles minimaux : `field-sizing: content`, `max-height: 10rem`, `overflow-y: auto`, `resize: none`, capsule pleine largeur, focus violet, bouton circulaire et animation décalée des points désactivée sous reduced motion.
- [ ] Relancer le test ciblé et attendre zéro échec.
- [ ] Examiner le diff de la feature pour vérifier que clarifications, erreurs, idempotence et navigation READY sont inchangées.

### Task 2: Aperçu replié et transition accessible

**Interfaces:**
- Consumes: `previewCollapsed: WritableSignal<boolean>` et `togglePreview(): void` existants.
- Produces: `previewCollapsed` initialisé à `true`, bouton natif avec `aria-expanded`, structure `.preview-content > .preview-content-inner`.

- [ ] Mettre à jour les tests configuration/preview pour attendre le panneau replié au premier rendu, un bouton natif contrôlant `report-preview-body`, puis les classes expanded après clic.
- [ ] Exécuter les deux tests ciblés avec `npm.cmd test -- --watch=false --include="src/app/features/rapports/pages/configuration/configuration.component.spec.ts" --include="src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.spec.ts"` et constater l’échec initial.
- [ ] Initialiser `previewCollapsed` à `signal(true)` sans modifier `togglePreview()` ni le pipeline HTTP.
- [ ] Remplacer le bouton PrimeNG projeté par un vrai `<button type="button">` contenant un chevron existant, `aria-expanded`, `aria-controls` et l’appel à `togglePreview()`.
- [ ] Renommer le titre en « Aperçu du rapport » et envelopper le corps existant dans `.preview-content` et `.preview-content-inner`, avec classe expanded dérivée de `!collapsed()`.
- [ ] Implémenter les transitions demandées `grid-template-rows 220ms ease`, `opacity 180ms ease` et rotation 220 ms du chevron, puis les neutraliser sous `prefers-reduced-motion: reduce`.
- [ ] Relancer les tests ciblés et attendre zéro échec, notamment aucune requête supplémentaire lors du toggle.
- [ ] Examiner le diff pour confirmer que rendu des lignes, retry, stale, loading et erreurs sont inchangés.

### Task 3: Layout partagé pour configuration et export

**Interfaces:**
- Consumes: routes `configuration/:datasetId` et `export/:generationId`, `SharedPageLayoutComponent`, composants enfants lazy-loaded.
- Produces: variantes `LayoutPage` `configuration|export`, URLs identiques et un seul header/h1.

- [ ] Modifier les tests de layout/routing pour couvrir les cinq pages, vérifier la navigation Rapports active sur `configuration` et `export`, puis vérifier un seul `.shared-header` et un seul `h1` sur les deux URLs paramétrées.
- [ ] Exécuter `npm.cmd test -- --watch=false --include="src/app/shared/page-layout/shared-page-layout.component.spec.ts" --include="src/app/shared/page-layout/shared-page-layout.routes.spec.ts"` et constater l’échec des nouvelles variantes.
- [ ] Restructurer `rapports.routes.ts` avec un parent `SharedPageLayoutComponent` portant `data.page` sur chaque enfant ou via des parents dédiés, sans changer les segments ni les paramètres.
- [ ] Étendre `LayoutPage` et `PAGE_COPY` pour `configuration` et `export`, puis considérer ces variantes comme actives pour le lien `/rapports`.
- [ ] Supprimer le header local complet de `configuration.component.html` et conserver son `main`; supprimer `export-topbar` et le `h1` local de `export.component.html`, en conservant le stepper et le workflow.
- [ ] Retirer uniquement les styles devenus morts (`export-topbar*`, titre local, seconde grille de viewport) et adapter les conteneurs enfants à la hauteur disponible du layout partagé sans introduire de `100dvh` imbriqué.
- [ ] Relancer les tests ciblés et attendre zéro échec.
- [ ] Examiner le diff pour confirmer que `datasetId`, `generationId`, `startNewReport()`, polling, génération et export restent inchangés.

## Integrated Validation

- [ ] Exécuter les tests ciblés des Tasks 1 à 3 et enregistrer les résultats exacts.
- [ ] Exécuter `npm.cmd test -- --watch=false` depuis `Frontend/Rhis_report_gen`; distinguer toute défaillance préexistante.
- [ ] Exécuter `npm.cmd run build`; signaler warnings de budget ou échecs.
- [ ] Vérifier le diff final depuis la racine Git avec `git -c safe.directory='C:/Users/Surface Pro/Downloads/RHIS' diff -- Frontend/Rhis_report_gen` et confirmer l’absence de changement backend.
- [ ] Vérification manuelle recommandée : textarea à 1 ligne puis 160 px, Ctrl+Enter, attente, toggle preview, un seul header sur desktop/mobile configuration/export.

## Rollback

Restaurer uniquement les hunks listés dans le File Map. Aucun schéma, donnée, endpoint ou migration n’est concerné.

## Completion Criteria

- Le composer est une capsule sans format visible, pièce jointe ni débordement horizontal.
- L’attente affiche « Assistant — Réflexion » et trois points sans état supplémentaire.
- L’aperçu est replié par défaut, animé avec reduced motion respecté et continue de se mettre à jour.
- Configuration et export utilisent exclusivement le header partagé, avec URLs et logique inchangées.
- Les tests ciblés et le build sont verts, ou toute défaillance baseline est explicitement isolée.
