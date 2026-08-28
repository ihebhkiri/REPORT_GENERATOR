# Remplacer les accordéons d’exposition par un master-detail

This ExecPlan is a living document governed by `../.agent/PLANS.md`. Keep `Progress`,
`Surprises & Discoveries`, `Decision Log` and `Outcomes & Retrospective` current throughout
implementation.

Date: 2026-08-26

Status: Implemented — manual browser validation pending

Research: `docs/research/2026-08-24-dataset-exposure-master-detail.md`

Related issue: N/A

## Purpose and observable outcome

Un administrateur ouvre `/administration/datasets` et voit une liste de tables à gauche ainsi que le
détail de la table choisie à droite. Il peut rechercher séparément les tables et les champs, parcourir
plusieurs tables sans perdre son brouillon, modifier les quatre modes et les champs autorisés, annuler
le brouillon ou enregistrer toutes les modifications en une seule fois.

Sur un écran inférieur à 1024 px, la liste est affichée en premier, la sélection ouvre le détail et un
bouton revient à la liste sans perdre l’état. Les succès et échecs ponctuels utilisent l’unique Toast
PrimeNG global ; une erreur de chargement reste visible dans la page avec `Réessayer`. Une navigation
ou fermeture avec des changements non enregistrés demande confirmation.

## Global constraints

- Ne modifier aucun contrat API, fichier backend, schéma ou règle d’exposition.
- Conserver Angular 20 standalone, TypeScript 5.9, RxJS 7.8 et PrimeNG 20.4.0.
- Ne pas ajouter de dépendance, store, facade, mapper, wrapper PrimeNG ou composant générique.
- Conserver `baseline`/`draft`, le delta global, le PUT unique et l’atomicité backend.
- Ne jamais afficher `HttpErrorResponse.error.detail` ni un autre détail backend brut.
- Ne pas afficher de nom technique ou de type absent du contrat admin.
- Utiliser les tokens PrimeNG et la direction `DESIGN.md` ; éviter les couleurs/dimensions répétées
  en dur et le gradient indigo actuel.
- Conserver les éléments inactifs visibles et read-only.
- Ne pas commit, push, merge ou rebaser sans demande explicite.
- Préserver `CHANGELOG.md` non suivi et tout autre changement utilisateur hors périmètre.

## Scope and non-goals

In scope:

- remplacer le `p-accordion` par un master-detail local à la feature ;
- gérer la sélection, deux recherches, compteurs, marqueurs dirty et navigation mobile ;
- conserver ou compléter loading, erreur, empty, no-result, no-selection, inactive, disabled,
  saving et dirty ;
- ajouter `Annuler les modifications` et conserver `Enregistrer` dans une barre accessible ;
- installer un unique `p-toast` racine et utiliser `MessageService` pour le PUT ;
- remplacer les détails backend visibles par des messages fonctionnels ;
- ajouter une protection `CanDeactivateFn` et `beforeunload` ;
- adapter les tests et la documentation de flux.

Non-goals:

- filtres avancés par statut ou mode ;
- pagination, virtual scroll, tri de colonnes ou actions de masse ;
- exposition des noms techniques/types par modification du backend ;
- persistance du brouillon dans `sessionStorage` ;
- confirmation personnalisée PrimeNG pour quitter la page ;
- refactorisation des erreurs ou notifications des autres features ;
- extraction de lignes, badges, états vides ou barre d’actions en composants partagés.

## Current behavior

La recherche `docs/research/2026-08-24-dataset-exposure-master-detail.md` établit que :

- `DatasetExposureComponent` détient déjà un baseline et un draft globaux, calcule un delta par
  table, empêche les doubles sauvegardes et remplace la référence seulement après succès ;
- `toExposureMode()` et `fromExposureMode()` couvrent déjà les quatre modes ;
- `fieldsDisabled()` applique `!dataset.active || mode === 'NONE'` sans effacer `visible` ;
- l’écran utilise un accordéon multiple et une recherche commune aux tables/champs ;
- il n’existe ni sélection explicite, ni reset, ni guard dirty, ni `beforeunload` ;
- le GET initial a déjà un état page et `Réessayer` ;
- le PUT affiche actuellement succès/erreur avec `p-message` et peut afficher directement
  `ProblemDetail.detail` ;
- aucun `p-toast`, `ToastModule` ou `MessageService` n’est configuré dans l’application ;
- le DTO admin ne transporte que les `displayName`, sans noms techniques ni types.

La baseline ciblée exécutée le 2026-08-26 est verte : 6 tests sur les trois specs de la feature.
Cette observation ne vaut pas validation de la suite frontend complète.

## Proposed approach

### 1. Une infrastructure Toast globale, sans wrapper

Fournir `MessageService` dans `app.config.ts`, importer `ToastModule` dans le composant racine et
rendre exactement un `<p-toast>` dans `app.html`. La page injecte ensuite directement
`MessageService`.

Le PUT publie :

- succès : summary `Enregistrement réussi`, detail `La configuration a été enregistrée.` ;
- erreur : summary `Échec de l’enregistrement`, detail `Vos modifications sont conservées. Réessayez.`.

Le GET échoué ne publie pas de Toast. Il affiche dans la page `Impossible de charger la configuration
des données.` et le bouton `Réessayer`. Le `HttpErrorResponse` peut être journalisé avec un libellé de
contexte, mais son `detail` n’alimente jamais l’UI.

### 2. Un état master-detail local dérivé du draft

Conserver le composant page comme unique orchestrateur. Ajouter les Signals et `computed()` suivants,
avec ces responsabilités :

```typescript
readonly selectedDatasetId = signal<number | null>(null);
readonly tableSearchTerm = signal('');
readonly fieldSearchTerm = signal('');
readonly mobileDetailVisible = signal(false);
readonly selectedDataset = computed<DatasetExposure | null>(() =>
  this.draft().find((dataset) => dataset.id === this.selectedDatasetId()) ?? null,
);
readonly filteredDatasets = computed<readonly DatasetExposure[]>(() => {
  const search = this.normalizeSearch(this.tableSearchTerm());
  return search
    ? this.draft().filter((dataset) =>
        this.normalizeSearch(dataset.displayName).includes(search),
      )
    : this.draft();
});
readonly filteredFields = computed<readonly FieldExposure[]>(() => {
  const fields = this.selectedDataset()?.fields ?? [];
  const search = this.normalizeSearch(this.fieldSearchTerm());
  return search
    ? fields.filter((field) => this.normalizeSearch(field.displayName).includes(search))
    : fields;
});
readonly dirtyDatasetIds = computed<ReadonlySet<number>>(
  () => new Set(this.changes().map((dataset) => dataset.id)),
);
```

Les méthodes UI restent explicites et locales :

```typescript
selectDataset(datasetId: number): void;
showTableList(): void;
updateTableSearch(event: Event): void;
updateFieldSearch(event: Event): void;
resetDraft(): void;
hasUnsavedChanges(): boolean;
private normalizeSearch(value: string): string;
```

`selectDataset()` change seulement l’ID et ouvre la vue détail mobile. Elle n’appelle pas le service,
ne clone pas le dataset et ne réinitialise aucun Signal métier. `resetDraft()` recopie le baseline en
conservant si possible la sélection et les recherches. Après un nouveau GET ou un PUT réussi, aucune
table n’est auto-sélectionnée si la sélection courante n’existe plus.

Après le premier chargement, `selectedDatasetId` reste `null`. Ce choix rend l’état « aucune table
sélectionnée » observable et garantit l’ouverture mobile sur la liste. L’approbation de ce plan vaut
validation de ce comportement.

La recherche table filtre uniquement `dataset.displayName`. La recherche champ filtre uniquement
les `field.displayName` de la table sélectionnée. Une table sélectionnée filtrée hors de la liste garde
son détail et son draft. Aucun nom technique n’est recherché puisqu’il est absent du contrat.

### 3. Un layout CSS master-detail, sans `p-splitter`

Le layout utilise CSS Grid plutôt qu’un splitter redimensionnable : la largeur réglable n’est pas un
besoin et ajouterait une interaction inutile. À partir de 1024 px, la surface a deux colonnes, environ
20–22 rem pour la liste et `minmax(0, 1fr)` pour le détail. Sous 1024 px, les mêmes deux panneaux sont
présents mais une classe d’état montre soit la liste, soit le détail.

Le panneau maître contient : titre et nombre total, recherche avec label visible, liste de boutons de
table, statut textuel avec icône, marqueur `Modifiée` et état sélectionné non chromatique seulement.
Les boutons natifs assurent Tab/Entrée/Espace et un focus visible sans implémenter un faux listbox.
Sur mobile, la sélection place le focus sur le titre du détail après son rendu ; `Retour aux tables`
restaure le focus sur le bouton de la table sélectionnée. Sur desktop, le focus reste sur la ligne
activée. Un statut `aria-live="polite"` annonce aussi la table sélectionnée et le résumé dirty.

Le détail contient :

- un bouton `Retour aux tables` visible sous 1024 px ;
- nom fonctionnel, statut actif/inactif, libellé du mode et `p-select` associé ;
- compteur `n champs exposés sur total` calculé depuis le draft ;
- recherche de champs avec label visible ;
- une table/grille sémantique locale avec libellé, état et checkbox d’exposition ;
- des messages distincts pour aucun champ et aucun résultat de recherche.

Le nom technique et le type ne sont pas rendus. Les lignes inactives restent lisibles ; seule
l’interaction est désactivée. Une table `NONE` garde ses champs visibles mais leurs checkboxes sont
désactivées.

Une barre d’actions sticky commune à la surface contient le résumé dirty, `Annuler les modifications`
secondaire et `Enregistrer` primaire. Le layout réserve sa hauteur afin qu’elle ne recouvre aucune
ligne. Les deux boutons sont désactivés sans draft ou pendant le PUT ; la zone expose `aria-busy`
pendant l’enregistrement.

### 4. Deux protections complémentaires contre la sortie

Créer un guard fonctionnel dédié :

```typescript
export const pendingDatasetExposureChangesGuard:
  CanDeactivateFn<DatasetExposureComponent>;
```

Il retourne `true` sans changement ; sinon il appelle `window.confirm()` avec un message fonctionnel.
`app.routes.ts` l’ajoute après `adminGuard` dans `canDeactivate`.

Le composant intercepte également `window:beforeunload`. Il ne prévient le défaut et ne fixe
`returnValue` que si `dirty()` est vrai. Le navigateur contrôle le texte final de cette confirmation.
Une sauvegarde réussie ou une réinitialisation rend immédiatement les deux protections inactives.

### 5. Réutilisabilité volontairement limitée

Aucun composant enfant n’est créé. Les lignes de tables et de champs sont chacune implémentées dans
une seule boucle ; les extraire déplacerait du markup sans supprimer de duplication. Les badges,
empty states et barres d’actions observés ailleurs ont des contrats et comportements différents.

L’unique élément transversal est le Toast PrimeNG standard au niveau racine. Il ne reçoit aucun
wrapper, `input` ou `output`. Si une seconde feature adopte plus tard une politique commune de wording
ou de journalisation, cette duplication réelle pourra justifier un service applicatif séparé.

## Affected files and symbols

### Infrastructure globale frontend

- `../Frontend/Rhis_report_gen/src/app/app.config.ts`
  - fournir une seule instance racine de `MessageService`.
- `../Frontend/Rhis_report_gen/src/app/app.ts`
  - ajouter `ToastModule` aux imports standalone.
- `../Frontend/Rhis_report_gen/src/app/app.html`
  - rendre un seul `<p-toast>` avant le `router-outlet` ; aucune instance dans la page admin.
- `../Frontend/Rhis_report_gen/src/app/app.spec.ts`
  - fournir le service au test isolé et vérifier qu’une seule instance Toast est rendue.

### Feature dataset exposure

- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.ts`
  - retirer `AccordionModule` et le feedback inline de sauvegarde ;
  - injecter `MessageService` ;
  - ajouter sélection, recherches indépendantes, vue mobile, dirty IDs et reset ;
  - conserver `buildChanges`, conversions, règles disabled et snapshot baseline/draft ;
  - ajouter le handler `beforeunload` et des messages fonctionnels fixes.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.html`
  - remplacer l’accordéon par les panneaux maître/détail et la barre d’actions ;
  - conserver l’erreur de chargement inline avec `Réessayer` ;
  - ne rendre ni Toast local, ni détail backend, ni nom technique absent.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.scss`
  - remplacer la colonne/gradient par le grid desktop et les vues progressives sous 64 rem ;
  - utiliser les tokens `--p-surface-*`, `--p-text-*`, `--p-primary-*` et les rayons PrimeNG ;
  - définir focus, selected, inactive, sticky actions et réserve de scroll ;
  - assurer des targets proches de 44 px et supprimer les styles d’accordéon inutiles.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.spec.ts`
  - remplacer la fixture minimale par plusieurs tables/champs et couvrir tous les scénarios demandés.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/pending-dataset-exposure-changes.guard.ts` (new)
  - guard fonctionnel limité à cette page, utilisant `hasUnsavedChanges()`.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/pending-dataset-exposure-changes.guard.spec.ts` (new)
  - absence de confirmation si clean, refus/acceptation si dirty.
- `../Frontend/Rhis_report_gen/src/app/app.routes.ts`
  - ajouter le guard dans `canDeactivate` de `/administration/datasets`.

Les fichiers `dataset-exposure.model.ts`, `dataset-exposure.model.spec.ts`,
`dataset-exposure.service.ts` et `dataset-exposure.service.spec.ts` ne devraient pas changer. Ils font
partie de la validation de régression. Tout besoin de les modifier devra être consigné dans le
`Decision Log` avant l’édition.

### Documentation

- `docs/flows/01-dataset-selection-and-configuration-load.md`
  - remplacer le résumé de l’administration par le master-detail, les recherches, le brouillon, le
    Toast et la protection de sortie.
- `docs/plans/2026-08-24-dataset-exposure-master-detail.md`
  - tenir à jour progression, découvertes, décisions, commandes et résultats réels.

## Milestone 1: Installer l’unique Toast global

### Objective

L’application possède une seule infrastructure PrimeNG de notifications, disponible aux pages lazy,
sans afficher encore de Toast depuis la feature.

### Files

- `../Frontend/Rhis_report_gen/src/app/app.config.ts`
- `../Frontend/Rhis_report_gen/src/app/app.ts`
- `../Frontend/Rhis_report_gen/src/app/app.html`
- `../Frontend/Rhis_report_gen/src/app/app.spec.ts`

### Exact changes

- importer et fournir `MessageService` une seule fois dans la configuration racine ;
- importer `ToastModule` dans `App` ;
- ajouter exactement un `<p-toast position="top-right">` et son input `breakpoints` pour une largeur
  `calc(100% - 2rem)` avec marges de 1 rem sous 640 px, conformément aux inputs documentés de
  PrimeNG 20.4.0 ;
- adapter le spec racine et compter une seule instance de `p-toast`.

### Preserved behavior

- le `router-outlet`, le thème PrimeNG, les animations et les routes restent inchangés ;
- aucun composant métier n’est déplacé et aucune notification existante d’une autre page n’est
  modifiée.

### Tests

- le composant `App` se crée avec le provider ;
- le DOM contient un seul Toast et un seul outlet.

### Validation

- Command: `npm.cmd test -- --watch=false --include="src/app/app.spec.ts"`
- Expected observation: suite racine verte, aucune erreur `NullInjectorError`, une seule instance de
  Toast.

### Success criteria

- une feature lazy peut injecter le même `MessageService` ;
- aucun `p-toast` n’est ajouté dans `dataset-exposure.component.html`.

## Milestone 2: Introduire l’état master-detail, le feedback fonctionnel et la protection dirty

### Objective

La logique de sélection et de brouillon est testable indépendamment du layout final ; le PUT utilise
le Toast global, le reset existe et toute sortie dirty est protégée.

### Files

- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.ts`
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.html`
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.spec.ts`
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/pending-dataset-exposure-changes.guard.ts` (new)
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/pending-dataset-exposure-changes.guard.spec.ts` (new)
- `../Frontend/Rhis_report_gen/src/app/app.routes.ts`

### Exact changes

- enrichir la fixture de test avec au moins trois tables : deux actives dans des modes différents et
  une inactive, avec champs actifs, inactifs, visibles et masqués ;
- écrire d’abord les tests de sélection, recherches distinctes, draft multi-table, quatre modes,
  champ, `NONE`, inactive, reset, succès/erreur du PUT et erreur/retry du GET ;
- ajouter les Signals, computeds et méthodes listés dans `Proposed approach` ;
- faire de `setConfiguration()` une copie profonde cohérente pour baseline/draft, puis conserver ou
  invalider la sélection uniquement selon la présence de l’ID dans la réponse ;
- injecter un spy `MessageService` dans les tests et le service réel dans le composant ;
- au succès, publier le Toast fonctionnel après installation du nouveau baseline ;
- à l’erreur, journaliser le contexte, publier le Toast fonctionnel et ne modifier ni baseline ni
  draft ;
- remplacer provisoirement dans le template les `p-message` de save par la barre de feedback/action
  nécessaire afin d’éviter un double feedback avant le milestone visuel ;
- ajouter `resetDraft()` sans HTTP et sans Toast ;
- implémenter le guard fonctionnel, son test et `canDeactivate` ;
- implémenter et tester `beforeunload` uniquement quand `dirty()` vaut vrai.

### Preserved behavior

- aucun clic de sélection, recherche, mode ou champ n’émet de PUT ;
- le payload reste le delta global produit par `buildChanges()` ;
- une table `NONE` conserve les valeurs de champs ;
- les inactifs restent exclus du delta ;
- le GET initial reste relançable et le PUT reste protégé contre les doubles soumissions.

### Tests

- sélectionner la deuxième table change le détail dérivé sans HTTP ;
- modifier deux tables, naviguer entre elles et vérifier les deux drafts ;
- filtrer les tables ne filtre pas les champs et inversement ;
- parcourir les quatre modes conserve les conversions existantes ;
- un champ actif change, un champ/table inactive reste inchangé ;
- `NONE` désactive les champs sans changer leur visibilité ;
- reset restaure toutes les tables sans HTTP et rend `dirty()` faux ;
- succès : nouveau baseline, dirty faux, Toast success sans message inline ;
- erreur : draft et dirty conservés, Toast error fonctionnel, détail backend absent du message ;
- GET échoué : message fonctionnel page et retry, aucun Toast ;
- guard et `beforeunload` ne bloquent que si dirty.

### Validation

- Command: `npm.cmd test -- --watch=false --include="src/app/features/administration/dataset-exposure/dataset-exposure.model.spec.ts" --include="src/app/features/administration/dataset-exposure/dataset-exposure.service.spec.ts" --include="src/app/features/administration/dataset-exposure/dataset-exposure.component.spec.ts" --include="src/app/features/administration/dataset-exposure/pending-dataset-exposure-changes.guard.spec.ts"`
- Expected observation: toutes les transitions de brouillon, requêtes et notifications sont vertes ;
  un `detail` backend injecté par le test n’apparaît dans aucun message utilisateur.

### Success criteria

- la logique demandée est prouvée sans store ni nouveau service applicatif ;
- une erreur de PUT ne détruit jamais le draft ;
- une navigation Angular et une fermeture/recharge navigateur sont protégées quand nécessaire.

## Milestone 3: Remplacer l’accordéon par l’interface master-detail accessible et responsive

### Objective

La page finale respecte la structure maître-détail, les états obligatoires et la navigation progressive
mobile, avec des actions toujours accessibles.

### Files

- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.ts`
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.html`
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.scss`
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.spec.ts`

### Exact changes

- supprimer `AccordionModule` et tout markup/style d’accordéon devenu inutile ;
- construire le panneau maître avec titre `Tables`, total, label/recherche et boutons suivis par ID ;
- afficher sur chaque ligne nom fonctionnel, actif/inactif, `Modifiée` et sélection par plusieurs
  indices visuels/textuels ;
- construire le détail conditionnel avec état no-selection, en-tête table, mode courant, `p-select`,
  compteurs, recherche champ et grille/table de champs locale ;
- associer explicitement chaque label à son input/select/checkbox ;
- afficher le nom technique/type seulement si le contrat en fournit un — condition actuellement
  fausse, donc aucun placeholder ni valeur inventée ;
- distinguer aucune table, aucun résultat table, aucune sélection, aucun champ et aucun résultat
  champ ;
- garder l’erreur de chargement avant les panneaux avec `role="alert"` et `Réessayer` ;
- afficher les skeletons dans la géométrie des deux panneaux ;
- ajouter le retour mobile et basculer `mobileDetailVisible` sans toucher à la sélection/draft ;
- déplacer le focus vers le titre du détail lors d’une sélection mobile et le restaurer sur la ligne
  sélectionnée lors du retour ; conserver le focus de la ligne sur desktop ;
- rendre la barre sticky sans recouvrement, avec résumé dirty, reset secondaire et save primaire ;
- remplacer couleurs/rayons locaux par tokens PrimeNG, définir le grid desktop et le mode progressif
  sous 64 rem, respecter `prefers-reduced-motion` ;
- ajouter des sélecteurs `data-testid` uniquement lorsque le test ne peut pas cibler un rôle ou label
  accessible de façon stable.

### Preserved behavior

- mêmes DTO, mêmes IDs, mêmes modes et même ordre renvoyé par l’API ;
- aucune règle main/relation, active ou visible ne change ;
- les recherches n’altèrent jamais baseline/draft ;
- une table filtrée hors du maître peut rester sélectionnée dans le détail desktop ;
- aucune instance Toast ni composant partagé n’est ajouté dans la page.

### Tests

- DOM desktop : deux panneaux, liste complète, no-selection puis détail correspondant au clic ;
- recherches : compteur/résultats/empty states distincts ;
- dirty marker uniquement sur les tables modifiées ;
- compteur exposé/total mis à jour depuis le draft ;
- labels et noms accessibles sur recherche, mode, checkboxes, reset, save et retour ;
- focus mobile déplacé vers le détail puis restauré sur la table au retour ;
- état inactif lisible et contrôles disabled ;
- état saving bloque les deux actions et affiche un indicateur perceptible ;
- navigation mobile logique : liste initiale, sélection ouvre le détail, retour réaffiche la liste tout
  en conservant recherche, sélection et draft ;
- le template ne contient aucun accordéon ni Toast local.

### Validation

- Command: `npm.cmd test -- --watch=false --include="src/app/features/administration/dataset-exposure/dataset-exposure.component.spec.ts"`
- Command build: `npm.cmd run build`
- Expected observation: tests DOM et logique verts ; build sans nouvelle erreur ; aucun import ou style
  d’accordéon inutilisé.

### Success criteria

- à partir de 1024 px, liste et détail sont simultanément exploitables sans scroll horizontal ;
- sous 1024 px, le parcours liste → détail → liste conserve tout le brouillon ;
- avec une longue liste de champs, les actions restent atteignables et ne masquent aucune ligne ;
- les états et le focus ne reposent pas uniquement sur la couleur.

## Milestone 4: Vérifier la régression, l’accessibilité et documenter le flux réel

### Objective

Le changement est validé au-delà du test focalisé, le diff est limité au plan et la documentation
décrit exactement le comportement livré.

### Files

- `docs/flows/01-dataset-selection-and-configuration-load.md`
- `docs/plans/2026-08-24-dataset-exposure-master-detail.md`
- tous les fichiers modifiés aux milestones 1–3, pour revue finale seulement.

### Exact changes

- mettre à jour la section administration du flow avec master-detail, Toast, reset et sortie dirty ;
- exécuter la suite frontend complète puis le build production ;
- vérifier le diff et `git diff --check` depuis la racine commune ;
- exécuter manuellement les scénarios desktop/mobile, clavier, erreurs et longue liste ;
- consigner résultats exacts, échecs de baseline, déviations et risques dans cet ExecPlan.

### Preserved behavior

- aucun test backend ni changement backend n’est requis parce que le contrat et les règles ne changent
  pas ;
- les échecs préexistants sont rapportés comme tels, jamais présentés comme succès.

### Tests

- suite frontend complète ;
- build production ;
- contrôle manuel aux largeurs 320, 768, 1024 et 1440 px ;
- parcours clavier sans souris et inspection du focus ;
- simulation GET/PUT échoués et vérification de l’absence du détail backend visible ;
- navigation route, reload/fermeture, reset puis save avec/sans draft.

### Validation

- Command: `npm.cmd test -- --watch=false`
- Command: `npm.cmd run build`
- Command: `git -c safe.directory="C:/Users/Surface Pro/Downloads/RHIS" -C "C:/Users/Surface Pro/Downloads/RHIS" diff --check`
- Expected observation: aucun nouvel échec imputable au changement, build réussi, diff sans erreur de
  whitespace et aucun fichier backend/source hors liste modifié.

### Success criteria

- tous les critères d’acceptation ci-dessous disposent d’une preuve automatisée ou manuelle consignée ;
- `Progress`, `Surprises & Discoveries`, `Decision Log` et `Outcomes & Retrospective` reflètent
  l’exécution réelle ;
- `CHANGELOG.md` et les changements utilisateur hors périmètre restent intacts.

## Validation and acceptance

Automated:

- [x] la conversion des quatre modes reste exhaustive ;
- [x] sélection et détail correspondant sont couverts ;
- [x] recherches table et champ sont indépendantes ;
- [x] le draft de plusieurs tables survit aux changements de sélection ;
- [x] `NONE` conserve les valeurs de champs et désactive leur édition ;
- [x] table/champ inactifs restent lisibles et read-only ;
- [x] reset restaure le baseline sans HTTP ;
- [x] le PUT réussi remplace baseline/draft et publie un Toast success ;
- [x] le PUT échoué conserve le draft et publie un Toast fonctionnel sans détail backend ;
- [x] loading, erreur GET avec retry, empty et no-result sont couverts ;
- [x] guard et `beforeunload` protègent seulement un draft dirty ;
- [x] une seule instance globale de Toast est rendue ;
- [x] la suite frontend et le build sont exécutés.

Manual:

- [ ] à 1440 et 1024 px, liste et détail restent simultanément visibles et alignés ;
- [ ] à 768 et 320 px, la page démarre sur la liste, ouvre le détail puis revient sans perte ;
- [ ] une liste longue ne masque pas `Annuler les modifications` ni `Enregistrer` ;
- [ ] Tab, Maj+Tab, Entrée et Espace permettent la navigation et l’édition avec focus visible ;
- [ ] les états selected, dirty, inactive, disabled, saving et error restent compréhensibles sans
  couleur ;
- [ ] un message backend volontairement technique n’est visible ni dans la page ni dans le Toast ;
- [ ] aucun scroll horizontal inutile n’apparaît aux quatre largeurs ;
- [ ] le Toast reste lisible sur mobile et peut être fermé au clavier.

Acceptance criteria:

- [x] les accordéons sont entièrement remplacés par le master-detail ;
- [x] sélectionner/rechercher ne sauvegarde, ne réinitialise et ne perd rien ;
- [x] les quatre modes restent fondés uniquement sur les deux booléens existants ;
- [x] les champs exposés/total reflètent le draft courant ;
- [x] l’absence de noms techniques/types est explicitement assumée, sans donnée inventée ;
- [x] toutes les modifications sont envoyées ensemble dans le PUT delta existant ;
- [x] une erreur de sauvegarde laisse l’utilisateur libre de corriger ou réessayer ;
- [x] le chargement initial échoué bloque la page avec une action `Réessayer` ;
- [x] il n’existe qu’un Toast global et aucun wrapper PrimeNG ;
- [x] aucun composant réutilisable prématuré n’est créé ;
- [x] aucune règle backend, autorisation ou donnée persistée n’est modifiée.

## Risks and rollback

| Risk | Prevention/Detection | Rollback |
|---|---|---|
| Perte de draft lors d’une sélection ou recherche | Tests multi-table et Signals séparés | Revenir au composant précédent ; aucune donnée serveur n’est modifiée avant PUT |
| Double feedback Toast + inline | Test DOM et suppression explicite de `saveError/successMessage` | Restaurer temporairement le feedback inline et retirer l’appel Toast |
| Deux instances Toast | Instance uniquement dans `app.html`, test de comptage | Retirer l’instance ajoutée ailleurs ; conserver la racine |
| Confirmation dirty trop fréquente | Condition unique `dirty()` testée | Retirer guard/handler indépendamment du reste du layout |
| Régression mobile CSS | Breakpoint unique 64 rem + vérification 320/768/1024 | Revenir au SCSS précédent sans impact état/API |
| Barre sticky recouvrant les champs | Réserve de padding et test manuel longue liste | Rendre la barre statique comme correction locale |
| Détail backend exposé par régression | Tests avec `ProblemDetail.detail` sentinelle | Revenir aux constantes fonctionnelles, conserver erreur dans logs |
| Charge DOM avec 200 champs | Aucun composant par ligne, mesure manuelle | Ajouter virtualisation dans une tâche distincte seulement si mesure défavorable |

Le rollback est exclusivement frontend et ne requiert ni migration ni restauration de données. Un PUT
déjà réussi reste une modification métier volontaire et n’est pas annulé par le rollback du layout.

## Progress

- [x] 2026-08-26 21:55 +02:00 — Instructions, design, guidelines, sources, tests et documentation
  précédente inspectés.
- [x] 2026-08-26 21:55 +02:00 — Baseline ciblée exécutée : 6 tests réussis, 0 échec.
- [x] 2026-08-26 21:55 +02:00 — Research créée dans
  `docs/research/2026-08-24-dataset-exposure-master-detail.md`.
- [x] 2026-08-26 21:55 +02:00 — ExecPlan rédigé ; aucun fichier source modifié.
- [x] 2026-08-26 — Plan approuvé explicitement par l’utilisateur ; implémentation autorisée.
- [x] 2026-08-26 22:19 +02:00 — Milestone 1 terminé : test racine red puis green,
  2 tests réussis avec une seule instance Toast.
- [x] 2026-08-26 22:46 +02:00 — Milestone 2 terminé : état baseline/draft, Toasts
  fonctionnels, reset, guard et `beforeunload` validés dans la suite ciblée.
- [x] 2026-08-26 22:46 +02:00 — Milestone 3 terminé : master-detail desktop/mobile,
  recherches indépendantes, focus et états DOM validés ; 16 tests de composant réussis.
- [x] 2026-08-26 22:50 +02:00 — Milestone 4 automatisé et documentation terminés :
  23/23 tests ciblés réussis, build production réussi et flow mis à jour. La suite complète obtient
  123/125, avec deux échecs `ExportComponent` reproductibles isolément et sans fichier export modifié.
- [x] 2026-08-26 22:51 +02:00 — Revue finale effectuée : `git diff --check` sans erreur,
  aucun fichier backend source modifié et `CHANGELOG.md` non suivi préservé.

Exact next action: démarrer la stack frontend/backend puis exécuter les scénarios manuels aux largeurs
320, 768, 1024 et 1440 px consignés ci-dessus.

## Surprises & Discoveries

- 2026-08-26 — Contrairement à l’hypothèse « réutiliser si existant », aucune infrastructure Toast
  n’est présente dans `src/app`; l’instance globale doit être créée à la racine.
- 2026-08-26 — Le composant affiche actuellement `ProblemDetail.detail` brut via `errorMessage()` ;
  la refonte doit corriger ce point même si d’autres features ont encore un comportement similaire.
- 2026-08-26 — Le contrat admin livré après la spécification UI du 21 août a volontairement retiré
  les noms techniques et types ; la cible doit afficher uniquement les noms fonctionnels.
- 2026-08-26 — La baseline Git est un dépôt commun backend/frontend, malgré les instructions de
  sous-modules séparés ; les commandes Git doivent utiliser la racine commune avec `safe.directory`.
- 2026-08-26 — `CHANGELOG.md` est non suivi avant cette tâche et doit rester intact.
- 2026-08-26 — Le premier build a dépassé le seuil d’erreur `anyComponentStyle` de 8 kB. Le SCSS a
  été réduit localement sans relever le budget ; le build final réussit à 7,99 kB, tout en gardant
  un avertissement au-dessus du seuil de 4 kB.
- 2026-08-26 — La suite complète courante a deux échecs dans `ExportComponent` : action Option B
  introuvable et assertion responsive. Les mêmes deux échecs sont reproductibles dans sa spec seule
  (17/19) et aucun fichier export n’est modifié par ce plan.
- 2026-08-26 — Aucun service n’écoute sur les ports locaux 4200 ou 8080 ; la validation navigateur
  de bout en bout n’a donc pas été simulée à partir d’une stack applicative fonctionnelle.

## Decision Log

- 2026-08-26 — **Decision:** conserver toute l’orchestration dans `DatasetExposureComponent`.
  - Reason: baseline/draft, delta et HTTP sont déjà cohérents et la refonte n’ajoute qu’un état UI
    local.
  - Alternatives rejected: store/facade, qui ajouteraient une couche sans partage ni concurrence.
- 2026-08-26 — **Decision:** utiliser CSS Grid plutôt que `p-splitter`.
  - Reason: deux colonnes fixes/responsives satisfont le besoin ; aucun redimensionnement utilisateur
    n’est demandé.
  - Alternatives rejected: splitter redimensionnable et drawer mobile, plus complexes sans bénéfice.
- 2026-08-26 — **Decision:** ne créer aucun composant de ligne, badge, empty state ou action bar.
  - Reason: aucune duplication concrète entre fichiers ou responsabilités réutilisées n’a été trouvée.
  - Alternatives rejected: composants génériques à nombreux inputs/outputs, wrappers PrimeNG.
- 2026-08-26 — **Decision:** fournir un Toast PrimeNG unique à la racine et injecter directement
  `MessageService`.
  - Reason: infrastructure réellement transversale et API officielle suffisante.
  - Alternatives rejected: Toast local dupliqué ; service wrapper sans politique partagée existante.
- 2026-08-26 — **Decision:** conserver l’erreur initiale inline et réserver le Toast au PUT.
  - Reason: le GET échoué empêche l’utilisation complète de l’écran et nécessite un retry persistant.
  - Alternatives rejected: Toast seul, qui disparaîtrait sans restaurer l’écran.
- 2026-08-26 — **Decision:** ne jamais dériver le wording utilisateur de `HttpErrorResponse.error.detail`.
  - Reason: le détail backend peut être technique et instable.
  - Alternatives rejected: fallback conditionnel actuel ; nouveau mapper global hors périmètre.
- 2026-08-26 — **Decision:** ne sélectionner aucune table après le premier chargement.
  - Reason: rend l’état no-selection réel et respecte l’ouverture mobile sur la liste.
  - Alternatives rejected: auto-sélection de la première table, qui masque cet état ; sélection
    conditionnelle par viewport, qui complexifie l’état.
- 2026-08-26 — **Decision:** protéger route et navigateur séparément.
  - Reason: `CanDeactivateFn` ne couvre pas reload/fermeture et `beforeunload` ne couvre pas proprement
    la navigation Angular.
  - Alternatives rejected: une seule des deux protections ; dialogue PrimeNG global supplémentaire.

## Outcomes & Retrospective

Complete after implementation:

- Delivered behavior: master-detail responsive avec recherches indépendantes, brouillon multi-table,
  reset, delta PUT unique, Toast global, erreur GET persistante, guard et `beforeunload`. Aucun
  composant partagé prématuré ni wrapper PrimeNG n’a été ajouté.
- Commands run and results: 23/23 tests ciblés réussis ; build production réussi ; suite complète
  123/125 ; spec export isolée 17/19 avec les deux mêmes échecs hors périmètre ; `git diff --check`
  réussi.
- Deviations from the approved plan: aucune déviation fonctionnelle. Le SCSS a été simplifié pour
  respecter le seuil d’erreur existant sans modifier `angular.json`.
- Remaining risks or unverified checks: validation visuelle et clavier sur stack réelle non exécutée ;
  warnings de budget (initial 601,57 kB, composant 7,99 kB) ; deux tests export de baseline en échec ;
  avertissements 404 des polices PrimeIcons sous Karma.
- Required follow-up: vérifier manuellement les quatre largeurs, le focus clavier, le Toast mobile et
  les confirmations de sortie avec backend/authentification disponibles.
- Exact next action if incomplete: lancer la stack locale, exécuter la checklist manuelle puis corriger
  séparément la baseline `ExportComponent` si elle fait partie du prochain périmètre.
