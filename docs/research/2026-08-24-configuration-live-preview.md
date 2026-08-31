# Research: configuration des rapports et aperçu existant

Date de référence demandée : 2026-08-24  
Recherche effectuée : 2026-08-29 (Europe/Berlin)  
Status: Ready for planning  
Related issue: N/A — demande utilisateur « aperçu intégré et actualisé automatiquement »

## Question

Comment la page de configuration construit-elle sa définition, affiche-t-elle son aperçu et démarre-t-elle une génération aujourd’hui ? Quels invariants, contraintes responsive et tests doivent être préservés lors du remplacement de la popup ? Ce document décrit l’existant ; les choix de réalisation figurent dans l’ExecPlan.

## Scope

Inclus : page configuration, trois éditeurs, aperçu, transport HTTP, modèles, brouillon, navigation vers la génération, styles et tests proches. Lecture du backend limitée à la confirmation du contrat, des limites et des erreurs.

Exclus : changement de production, refonte des éditeurs, modification métier, SQL, sécurité, dépendances ou génération/export.

### Racine et état du checkout

Racine effective : `C:/Users/Surface Pro/Downloads/RHIS`. Le répertoire initial de travail est son enfant `RHIS/`. Les chemins ci-dessous sont relatifs à la racine effective.

Pour alléger les références : **F** = `Frontend/Rhis_report_gen` ; **C** = `Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration`. Ces préfixes désignent des chemins, pas de nouveaux modules.

Les AGENTS décrivent deux dépôts/submodules séparés. Dans ce checkout précis, `git rev-parse --show-toplevel` exécuté à la racine et depuis F renvoie la même racine ; `git ls-files --stage` indique des fichiers frontend/backend suivis en mode 100644 dans le dépôt parent. Ne pas supposer que des opérations Git indépendantes sont possibles sans revérification.

État initial et après tests ciblés : `git status --short` et `git diff --stat` vides. HEAD `394ad29` ; commits précédents `6070dbc` et `06259e0`. Aucun commit, reset, push ou changement de configuration Git effectué. Les lectures Git ont nécessité une exécution autorisée hors sandbox à cause du propriétaire Windows.

### Sources obligatoires consultées

- `AGENTS.md`, `RHIS/AGENTS.md`, `F/AGENTS.md`.
- `DESIGN.md`, `docs/guidelines/frontend-ui.md`.
- `.agent/PLANS.md`, `docs/research/TEMPLATE.md`, `docs/plans/TEMPLATE.md`.
- `RHIS/docs/flows/01-dataset-selection-and-configuration-load.md`.
- `RHIS/docs/flows/02-report-definition-fields-filters-sorts.md`.
- `RHIS/docs/flows/03-report-preview.md`.
- `RHIS/docs/flows/04-report-generation.md`.
- `F/docs/superpowers/specs/2026-08-10-configuration-report-ux-design.md`.
- F : sources, templates, styles et tests de configuration, éditeurs, preview ; services de transport et de brouillon ; `package.json`, `angular.json`, styles globaux et configuration applicative.

Graphify est disponible. La commande `graphify query 'ConfigurationComponent createPreviewRequest ReportPreviewService PreviewDialogComponent'` a localisé les symboles attendus. Sa réponse a trouvé 254 nœuds, avec une sortie limitée à 36 ; elle confond aussi les deux services homonymes Angular/Spring. Les conclusions ci-dessous ont donc été confirmées dans les sources. Aucun `graphify update .` : aucun code modifié.

## Verified current behavior

### Chargement et propriété de l’état

La route `configuration/:datasetId`, déclarée dans `F/src/app/features/rapports/rapports.routes.ts`, charge directement `ConfigurationComponent`. Elle n’est pas enveloppée dans `SharedPageLayoutComponent`.

`C/configuration.component.ts: constructor/loadConfiguration` lit l’ID principal et les IDs liés, valide les entiers positifs, déduplique/trie les liés puis appelle `ReportConfigurationLoader.load`. Le loader recharge le catalogue, résout les relations directes sortantes, charge les champs par `forkJoin`, retire les types non supportés et ajoute le contexte de dataset à chaque champ.

Le parent possède les Signals de référence `selectedFields`, `filters`, `filtersValid`, `sorts`, les datasets/champs et l’état de preview. `filterFieldGroups` est dérivé des colonnes sélectionnées. Les enfants n’appellent pas l’API de preview.

Nuance essentielle : le parent possède la **dernière définition exécutable**, mais le brouillon de filtre incomplet, les contrôles touched et le picker en cours appartiennent au `FormArray` de `FilterEditorComponent`. Reconstruire cet enfant ferait perdre ce brouillon, même si les Signals du parent sont conservés.

### Éditeurs et invariants

| Sujet | Fait confirmé | Preuve |
|---|---|---|
| Sélection et ordre | Ajout/retrait et réordonnancement émettent une nouvelle liste ; suivi UI par `field.key`. Drag CDK et boutons haut/bas existent déjà. | `C/components/column-selector/column-selector.component.ts: addField/removeField/dropSelectedField/moveField` et tests |
| Retrait de colonne | Le parent retire immédiatement les filtres et tris associés à l’ID retiré. | `C/configuration.component.ts: updateSelectedFields` ; test ligne 268 |
| Filtres | Formulaire typé ; valeurs sérialisées en chaînes ; `BETWEEN` demande deux valeurs ; les options sont limitées aux champs sélectionnés et opérateurs visibles. | `C/components/filter-editor/filter-editor.component.ts: emitConfiguration/configureValueControls/visibleOperators` |
| Brouillon invalide | `updateFilters` actualise toujours `filtersValid`, mais ne remplace `filters` que si `state.valid`. | `C/configuration.component.ts:188` ; test ligne 207 |
| Suppression de filtre devenu interdit | Un effect de l’éditeur retire les lignes dont le champ n’est plus autorisé et réémet la validité. | constructeur de `FilterEditorComponent` |
| Reset/restauration | L’éditeur reconstruit ses lignes uniquement lorsque `resetToken` change ; changer les `initialFilters` ne doit pas réinitialiser une saisie en cours. | `lastResetToken` et second effect de l’éditeur |
| Tris | Champs sélectionnés uniquement, sans doublon ; directions ASC/DESC ; l’ordre de la liste encode la priorité. | `C/components/sort-editor/sort-editor.component.ts: updateField/updateDirection/moveSort` |
| Simple ouverture du picker | Ouvrir « Ajouter » ne crée pas encore de ligne de filtre invalide ; choisir un champ crée le brouillon. | test `opens the grouped searchable field picker without creating a draft row` |

### Aperçu manuel

`openPreview()` vérifie `canPreview()`, rend la popup visible, puis appelle `loadPreview()`. `canPreview` impose au moins une colonne, filtres valides, aucun chargement d’aperçu et aucune génération en cours.

`createPreviewRequest(rootDataset)` (ligne 401) construit exactement :

```typescript
{
  rootDatasetId: rootDataset.id,
  selectedFieldIds: this.selectedFields().map(field => field.id),
  filters: this.filters(),
  sorts: this.sorts()
}
```

L’ordre des colonnes et tris est transmis sans tri supplémentaire. Le même constructeur sert à la génération.

`F/src/app/features/rapports/services/report-preview.service.ts: preview` effectue un POST typé vers `${environment.apiBaseUrl}/reports/preview`, avec `withCredentials: true`. L’endpoint Spring est `POST /api/v1/reports/preview` : `RHIS/src/main/java/RHIS/com/RHIS/report/controller/ReportController.java`.

`loadPreview` conserve `previewResult`, active loading et stale, efface l’erreur, puis souscrit. Au succès seulement, il remplace le résultat et enlève stale. À l’échec, seul le message d’erreur change. `finalize` termine loading ; `takeUntilDestroyed` coupe la souscription à la destruction de la page.

**Limite actuelle :** aucune annulation lors d’une modification de configuration, aucun debounce et aucune comparaison d’identité des requêtes. Une réponse commencée avant une modification peut encore s’installer et remettre stale à false. Le verrou loading empêche un second appel manuel concurrent, mais ne protège pas contre une modification locale pendant l’appel.

`markPreviewAsPrevious` marque le résultat précédent et efface l’erreur. `retryPreview` relance `loadPreview`, donc reconstruit la définition courante valide ; il ne possède pas de cache d’une ancienne requête échouée. Les garde-fous de `canPreview` restent appliqués.

### Tableau et états existants

`C/components/preview-dialog/preview-dialog.component.ts/html/scss` est standalone et OnPush. Inputs : visible, loading, response, error, stale. Outputs : visibleChange et retry. Le `p-dialog` est modal, non draggable/non resizable ; le tableau n’a aucun drag-and-drop.

La présentation réutilisable existe déjà :

- boucle sur `response.columns` dans l’ordre de l’API, accès aux cellules par `column.key` ;
- `formatValue` : null/undefined → « — », true/false → « Oui/Non », sinon `String(value)` ; pas de formatage temporel supplémentaire ;
- `tableRows = computed(() => [...rows])`, `p-table` scrollable, hauteur actuelle 360 px ;
- cellules tronquées avec title, sans supprimer les colonnes intrinsèquement larges ;
- premier loading, loading avec ancienne table, résultat vide, erreur avec ou sans ancienne table, retry et stale ;
- stale contient actuellement l’instruction « Cliquez sur Aperçu », liée au fonctionnement manuel.

`returnedRowCount` et `hasMore` existent dans le modèle mais ne sont pas affichés dans le template actuel. Le compteur n’est donc pas un comportement visuel déjà acquis.

`ReportPreviewExecutor` expose au maximum **6 lignes**, utilise une septième pour `hasMore`, et applique un timeout JDBC de **5 s**. `ReportSqlBuilder.buildPreview` fixe LIMIT 7. `returnedRowCount` est le nombre de lignes retournées, pas un total métier ; `hasMore` n’est pas une pagination.

### Génération et brouillon

`continueToExport()` vérifie `canGenerate` : sélection non vide, filtres valides, génération non déjà démarrée. La réussite d’une preview n’est pas requise ; preview loading n’interdit pas de générer.

La méthode construit la définition courante, sauvegarde `{version: 1, definition, relatedDatasetIds}`, appelle `startReportGeneration(definition, crypto.randomUUID())` et navigue vers `/rapports/export/:generationId`. Les erreurs restent inline dans la barre d’actions. `Précédent` mène à `/rapports`.

`ReportDraftStorageService` utilise `sessionStorage['rhis.report.draft.v1']`. Aucune écriture à chaque frappe ni état responsive n’y est enregistré. `restoreDraft` exige la même racine, les mêmes datasets liés et des IDs de champs encore présents ; il remet les Signals et incrémente la révision du filtre. Le dernier résultat de preview n’est pas persisté.

### Chargement initial et erreurs de configuration

Le template affiche un spinner initial, puis les éditeurs ou une erreur avec retry. URL invalide/erreur HTTP : reset de configuration et preview. Erreur métier du loader : message d’erreur, mais conservation possible de la configuration précédente. Rechargement réussi : restauration du draft compatible, sinon reset. Ces différences sont couvertes par les tests et ne doivent pas être uniformisées par la refonte.

## Data and control flow

```text
Route → ReportConfigurationLoader → champs/datasets → ConfigurationComponent
Colonnes → selectedFieldsChange → updateSelectedFields
Filtres → configurationChange(valid, filters) → updateFilters
Tris → sortsChange → updateSorts
                  ↓
      Signals de définition exécutable
                  ↓ createPreviewRequest
       ┌──────────┴─────────────────┐
clic Aperçu                    clic Générer
ReportPreviewService           save draft → startReportGeneration
POST /reports/preview          POST /report-generations + Idempotency-Key
résultat/error/stale            navigation /rapports/export/:id
PreviewDialogComponent         ou generationError inline
```

Backend preview : controller validant le DTO → resolver de métadonnées/types/relations → SQL paramétré → exécuteur JDBC → réponse. Pas de changement de contrat nécessaire identifié. L’annulation HTTP frontend n’est pas une garantie d’annulation immédiate du SQL côté serveur.

## Invariants and constraints

- Maintenir sélection ordonnée, filtres valides, purge des références après retrait, ordre des tris et constructeur de payload commun.
- Préserver la dernière réponse réussie, y compris une réponse vide, pendant loading/erreur/invalidité locale.
- Ne pas confondre la définition sauvegardée pour générer avec les lignes affichées.
- Aucun modèle public, endpoint, règle métier ou changement backend requis par le besoin UX.
- Préserver standalone, OnPush, fichiers TS/HTML/SCSS séparés, Signals locaux et formulaires existants.
- Ne pas recréer les éditeurs pour gérer une largeur ou un onglet.
- L’aperçu est un échantillon associé à une définition, pas une donnée actualisée en continu indépendamment des modifications.

### Responsive, style et dépendances vérifiés

Versions déclarées/installées : Angular 20.3.26 ; PrimeNG déclaré `^20.4.0`, installé 20.4.0 ; RxJS déclaré `~7.8.0` ; Tailwind 4 ; Angular CDK déjà présent. Aucune API supposée sur la seule base de la version majeure.

La page a son propre header h-16, un main max-width 1440 px et une grille `lg:grid-cols-3`. `ColumnSelector` passe en deux moitiés à `md`. Les tokens Tailwind installés sont sm=40rem, md=48rem, lg=64rem (`F/node_modules/tailwindcss/theme.css:327`). Les autres écrans n’utilisent pas tous le même seuil ; il n’existe pas de breakpoint applicatif central.

Les éditeurs filtre/tri ont des container queries à 24rem. Les Select/DatePicker de filtres sont attachés à body et bornés au viewport. Ces overlays devront être contrôlés avec de nouvelles régions scrollables.

DESIGN privilégie l’échelle 8 px avec micro-espacement 4 px ; le guide UI autorise aussi 12 px. Les valeurs communes 8/16/24/32 px évitent cette petite divergence. Les couleurs/tokens existants suffisent ; des valeurs codées en dur existent déjà mais ne justifient pas d’en introduire de nouvelles.

### PrimeNG Tabs 20.4.0 : vérification effective

Sources locales : `F/node_modules/primeng/tabs/index.d.ts` et `F/node_modules/primeng/fesm2022/primeng-tabs.mjs:672-746`. Documentation primaire : [Tabs PrimeNG v20](https://v20.primeng.org/tabs) et [TabPanel au tag 20.4.0](https://github.com/primefaces/primeng/blob/20.4.0/packages/primeng/src/tabs/tabpanel.ts).

Disponibles : `TabsModule`, `Tabs`, `TabList`, `Tab`, `TabPanels`, `TabPanel` ; sélecteurs `p-tabs/p-tablist/p-tab/p-tabpanels/p-tabpanel`. Valeur contrôlée par `value/valueChange`, type string | number | undefined. `lazy` et `selectOnFocus` valent false par défaut.

Avec lazy=false, le contenu est rendu, mais le host de TabPanel impose `[hidden]="!active()"`, role=tabpanel et un aria-labelledby construit depuis l’onglet. L’API de value active une seule valeur, pas deux panneaux simultanés. L’accessibilité clavier est implémentée par Tab. **Conserver les instances n’implique donc pas que les deux panneaux puissent être visibles sur desktop sans adaptation.** Aucun usage de Tabs n’a été trouvé dans src.

### Notifications et sécurité

`F/src/app/app.config.ts` fournit déjà `MessageService`. `F/src/app/app.html` contient un unique `p-toast`, importé à la racine. Une seconde infrastructure serait redondante. La preview actuelle n’émet pas de toast.

`previewErrorMessage` restitue n’importe quel `error.error.detail` de type string avant les cas 401/0. `ReportExceptionHandler` produit des ProblemDetail ; l’exécuteur traduit timeout/SQL en messages fixes sans SQL. Les erreurs de validation peuvent inclure la valeur saisie et un libellé. Le frontend ne distingue pas actuellement une erreur métier reconnue d’un detail technique inattendu venant d’un proxy ou d’un autre handler. L’interpolation n’exécute pas du HTML, mais cela ne garantit pas la confidentialité du texte affiché.

`RHIS/src/main/java/RHIS/com/RHIS/auth/SecurityConfig.java:57-63` exige **authenticated()** pour datasets/reports/generations/exports. L’UI n’est pas la frontière d’autorisation.

### Désaccords documentation/code

1. La spec historique du 10 août décrit Générer et Enregistrer brouillon désactivés. Aujourd’hui Générer lance un job et Enregistrer brouillon n’existe pas. Préserver le code et ses tests.
2. Les flows 01/03 indiquent encore permitAll pour datasets/preview ; le code exige désormais l’authentification. Ne pas modifier la sécurité pour reproduire ces textes.
3. La spec historique dit que Java connaît NOT_EQUALS/IS_NULL/IS_NOT_NULL ; `RHIS/.../dataset/model/FilterOperator.java` ne contient que les sept opérateurs visibles. Sans incidence requise sur la refonte ; aucune extension à prévoir.
4. Les sources Git effectives ne correspondent pas à la description en submodules. Revérifier avant création d’une branche d’implémentation.

## Existing tests and validation commands

### Ancien comportement à remplacer explicitement

`C/configuration.component.spec.ts:191` :

`it('never calls preview automatically when columns, filters or sorts change', ...)`

Il modifie sélection/filtres/tris et exige zéro appel. Ce test exprime l’ancien produit : **à remplacer**, pas à conserver en contournant l’actualisation.

Le test `removes filters and sorts whose selected column is removed` (ligne 268) contient aussi une assertion zéro appel ligne 290. Conserver ses assertions métier de purge et adapter seulement l’attente d’appel après debounce.

### Couverture et limites

- Configuration : 21 tests ; route/relations, sélection, purge, ordre du payload, filtres invalides, précédent résultat après erreur, action bar, génération/navigation, reset/retry du chargement.
- Preview : 4 tests ; six lignes, headers/booleans, empty, lignes pendant loading, erreur avec lignes et retry. Le test nommé « in column order » vérifie la présence des headers, pas strictement leur ordre : assertion à renforcer.
- Éditeurs et loader : ajout/retrait/réordonnancement, arité/validators, opérateurs, chaînes date/heure, focus et layout local.
- Service preview : POST exact, ordre de définition, credentials, réponse.
- Manquent notamment : debounce, annulation, late responses, onglets et changement de breakpoint, header réduit, loading sans résultat, erreur sans résultat et stale seul du présentateur.

### Commandes réellement exécutées

Depuis F :

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/**/*.spec.ts" --include="src/app/features/rapports/services/report-preview.service.spec.ts"
npm.cmd run build
```

Résultats : **54 tests ciblés réussis**, aucun skip signalé ; build réussi (exit 0).

Warnings **préexistants**, mesurés avant modification :
- bundle initial 604,67 kB pour un seuil warning 500 kB (+104,67 kB), seuil erreur 1 MB ;
- `src/app/features/administration/dataset-exposure/dataset-exposure.component.scss` 7,97 kB pour warning 4 kB, erreur 8 kB.
- chunk lazy configuration : 479,62 kB (repère, pas budget dédié).

La commande `npm.cmd test -- --watch=false --browsers=ChromeHeadless` a également été exécutée deux fois : **130 succès, 6 échecs sur 136**, exit 1, aucun skip signalé. Les échecs concernent SharedPageLayout (1), Export (1) et DatasetExposure (4) ; noms et assertions consignés dans l’ExecPlan. Warnings 404 de polices PrimeIcons sous Karma. Cette baseline n’est pas verte. Aucun test backend ni parcours manuel responsive effectué pendant cette recherche. Les tests unitaires ne constituent pas une validation de l’UX cible, encore non implémentée.

## Risks and unknowns

| Item | Type | Impact | How to resolve |
|---|---|---|---|
| Réponse ancienne après édition | Fait / risque | Faux état « à jour » | Orchestration testée avec requêtes contrôlées et invalidation pendant debounce |
| Perte du filtre incomplet si enfant recréé | Fait / risque | Perte de saisie et changement de validité | Tester l’identité des instances et contrôles au changement d’onglet/breakpoint |
| Volume d’appels avec auto-preview | Risque | Charge SQL ; annulation client non garantie serveur | Debounce, déduplication et vérification Network ; pas de polling |
| Layout docké et overlays attachés à body | Risque | Contrôle masqué / focus hors panneau visible | Matrice responsive, zoom, clavier et overlays |
| Utilisation desktop de TabPanel | Contrainte vérifiée | Un seul panneau actif via API standard | Choix explicite dans le plan, sans supposer une API multi-active |
| Detail backend arbitraire | Risque confirmé | Exposition technique dans l’UI | Politique locale de messages preview sûrs à valider |
| Sortie « temps réel » | Ambiguïté de vocabulaire | Attente de polling des données | Définir l’actualisation après changement de configuration seulement |
| Dimensions réelles et lecteurs d’écran | Non vérifié | Usabilité cible inconnue | Qualification après implémentation, pas de prétention de validation actuelle |

## Relevant files and symbols

Sources de modification possibles à confirmer par le plan : les quatre `C/configuration.component.*` et les quatre fichiers `C/components/preview-dialog/preview-dialog.component.*`.

Sources de contrat à préserver : `C/configuration.models.ts`, loader, les trois éditeurs, modèles preview, services preview/génération/brouillon. Le test du service preview couvre déjà le transport ; aucun manque obligeant à le modifier n’a été trouvé.

Documentation courante directement touchée par le futur changement de comportement : flows 02/03. La spec historique reste historique ; ne pas la réécrire comme si l’auto-preview existait déjà.

## Conclusions for planning

L’état et les composants nécessaires existent déjà. Le changement concerne la présentation de l’aperçu et son déclenchement dans la page, sans service global supplémentaire. Les invariants métier et la génération sont conservables. Les nouvelles garanties sont l’invalidation immédiate, le debounce, la déduplication, l’absence de destruction responsive des éditeurs et la qualification accessible.

## Open questions

Les arbitrages de conception ne sont pas des faits : breakpoint précis, stratégie d’onglets, état développé initial et traitement du détail d’erreur figurent dans le Decision Log du plan. Aucun besoin backend démontré. L’implémentation exige l’approbation explicite de l’ExecPlan.

