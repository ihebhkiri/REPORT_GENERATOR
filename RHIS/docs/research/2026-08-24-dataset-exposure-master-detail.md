# Research: refonte master-detail de l’exposition des datasets

Date: 2026-08-26

Status: Ready for planning

Related issue: N/A

## Question

Comment remplacer l’écran Angular d’administration de l’exposition des datasets, aujourd’hui
présenté en accordéons, par une interface master-detail responsive sans modifier le contrat API,
les règles métier ni le brouillon global, tout en ajoutant les notifications Toast et une protection
contre la perte de modifications non enregistrées ?

## Scope

Included:

- état actuel de `dataset-exposure`, de ses modèles, de son service et de ses tests ;
- contrat admin backend déjà exposé par `GET/PUT /api/v1/admin/dataset-exposure` ;
- layout master-detail desktop et navigation progressive sous 1024 px ;
- sélection, recherches distinctes, états UI, sauvegarde, réinitialisation et brouillon global ;
- infrastructure Toast PrimeNG et messages fonctionnels ;
- protection des modifications non enregistrées ;
- accessibilité, responsive et possibilités réelles de réutilisation.

Excluded:

- modification du backend, de la base de données ou du contrat HTTP ;
- modification des règles `active`, `displayMain`, `displayRelated` ou `field.visible` ;
- ajout d’un nom technique ou d’un type de champ au contrat admin ;
- refonte du shell global, des écrans de rapports ou du thème général ;
- implémentation pendant cette phase de recherche et de planification.

## Sources et méthode

- `../AGENTS.md`, `AGENTS.md` et `../Frontend/Rhis_report_gen/AGENTS.md` — workflow,
  conventions backend/frontend et emplacement canonique des documents.
- `../DESIGN.md` et `../docs/guidelines/frontend-ui.md` — hiérarchie, tokens, PrimeNG,
  responsive et accessibilité.
- `.agent/PLANS.md` via `../.agent/PLANS.md` — structure obligatoire de l’ExecPlan.
- `docs/research/2026-08-24-dataset-field-exposure.md` et
  `docs/plans/2026-08-24-dataset-field-exposure.md` — recherche et exécution précédentes ;
  leurs règles métier encore valides sont réutilisées au lieu d’être redémontrées.
- `docs/superpowers/specs/2026-08-21-database-tables-fields-admin-ui-design.md` — ancienne
  direction master-detail, utile pour les volumes, les états et le breakpoint, mais partiellement
  remplacée par le contrat et l’implémentation désormais réels.
- `docs/flows/01-dataset-selection-and-configuration-load.md` et
  `docs/flows/02-report-definition-fields-filters-sorts.md` — effet de l’exposition sur le parcours
  de rapport et distinction entre brouillon local et persistance serveur.
- `../graphify-out/graph.json` — requête ciblée sur l’exposition des datasets ; les conclusions
  importantes ont ensuite été vérifiées dans les sources.
- Documentation officielle [PrimeNG Toast 20.4.0](https://v20.primeng.org/toast) et
  [Angular `CanDeactivateFn`](https://angular.dev/api/router/CanDeactivateFn) — APIs vérifiées pour
  les versions utilisées.

Le dépôt Git commun est propre sur le périmètre, à l’exception de `CHANGELOG.md` non suivi et sans
rapport avec cette tâche. Aucun fichier source ne présente de diff local.

## Verified current behavior

### Contrat et règles métier

`src/main/java/RHIS/com/RHIS/dataset/controller/dto/DataSetExposureConfigurationResponse.java`

- Une table admin contient `id`, `displayName`, `active`, `displayMain`, `displayRelated`,
  `visibleFieldCount` et `fields`.
- Un champ admin contient uniquement `id`, `displayName`, `active` et `visible`.
- Le contrat ne contient ni `sourceName`, ni nom technique de champ, ni type de champ.
- Ajouter ces données à la grille demanderait un changement backend et une validation explicite ;
  ce n’est pas requis pour la refonte.

`../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.model.ts`

- `DatasetExposureMode` représente les quatre combinaisons des deux booléens existants.
- `toExposureMode()` et `fromExposureMode()` centralisent déjà la conversion ; le template n’a pas
  à réimplémenter cette matrice.
- Le modèle TypeScript reflète exactement le contrat admin et ne doit pas changer pour le layout.

Les règles suivantes, déjà établies dans la recherche précédente, restent invariantes :

- `active` est un état technique en lecture seule ;
- une table inactive et ses champs restent visibles mais non modifiables ;
- `NONE` désactive les contrôles de champs sans changer leurs valeurs `visible` ;
- un champ inactif conserve sa préférence mais ne peut pas être modifié ;
- les changements de mode et de visibilité sont envoyés ensemble dans une transaction atomique ;
- les règles d’utilisation comme source principale ou relation restent appliquées par le backend.

### État local et sauvegarde

`../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.ts: DatasetExposureComponent`

- `baseline` contient la dernière configuration chargée ou enregistrée ; `draft` contient les
  copies modifiables.
- `changes`, `changeCount` et `dirty` sont dérivés avec `computed()`.
- `buildChanges()` compare les datasets par ID et produit un delta par table modifiée. Il ignore
  les tables ou champs inactifs.
- `updateMode()` ne modifie que les deux booléens de la table ; les valeurs des champs restent dans
  le draft, y compris lorsque le mode devient `NONE`.
- `updateField()` crée de nouvelles structures et ne modifie que le champ actif demandé.
- `save()` n’émet aucun appel si le delta est vide ou si un enregistrement est déjà en cours.
- Un succès remplace atomiquement `baseline` et `draft` par la réponse serveur. Une erreur laisse le
  draft intact.

Changer de table n’existe pas encore : les tables sont des panneaux d’accordéon indépendants.
L’architecture baseline/draft est néanmoins déjà globale et convient directement au master-detail ;
la sélection peut rester un simple ID sans déplacer l’état métier dans un store.

### Présentation actuelle

`../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.html`

- La page utilise un `p-accordion` multiple ; chaque panneau répète mode et champs.
- Une seule recherche mélange tables et champs : un dataset est conservé si son `displayName` ou
  celui d’un champ correspond.
- Il n’existe pas de sélection maître, de recherche locale aux champs ni de retour mobile.
- L’action `Enregistrer` est sticky seulement sur desktop. Il n’existe pas d’action de
  réinitialisation.
- Le chargement initial, l’erreur avec `Réessayer`, la liste vide, l’absence de résultat et les
  éléments inactifs sont déjà représentés.
- Le succès et l’erreur de sauvegarde utilisent des `p-message` persistants dans la page.
- Il n’existe pas de protection de navigation ou de fermeture d’onglet lorsque `dirty()` est vrai.

`../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.scss`

- Le layout actuel est une colonne puis une grille de champs à deux colonnes.
- Le breakpoint actuel est 48 rem et empile seulement les blocs ; il ne fournit pas la navigation
  progressive demandée.
- Plusieurs couleurs, gradients, dimensions et ombres sont codés en dur. Le gradient indigo et la
  palette locale s’écartent de `DESIGN.md` et des tokens PrimeNG déjà employés dans l’écran export.
- Les lignes de champs restent utilisables au clavier grâce aux checkboxes, mais une future ligne de
  table sélectionnable devra utiliser un contrôle natif accessible et un focus visible.

### Erreurs et notifications

`DatasetExposureComponent.errorMessage()` lit aujourd’hui `error.error.detail` et l’affiche tel quel
pour le chargement et la sauvegarde. Ce détail provient du backend et peut être technique ; ce
comportement ne respecte pas la nouvelle exigence de message fonctionnel.

La recherche globale dans `src/app` ne trouve aucune occurrence de `p-toast`, `ToastModule` ou
`MessageService`. `app.html` ne contient qu’un `router-outlet` et `app.config.ts` ne fournit pas
`MessageService`. Il n’existe donc pas de Toast global à réutiliser.

PrimeNG 20.4.0 est verrouillé dans `package-lock.json`. Sa documentation officielle confirme :

- import `ToastModule` depuis `primeng/toast` ;
- publication par `MessageService.add()` ;
- rôle `alert` et annonce assertive intégrés au Toast ;
- adaptation mobile disponible sans ajouter de dépendance.

La plus petite infrastructure cohérente est une unique instance `p-toast` dans le composant racine,
un provider global `MessageService`, puis une injection directe dans la page. Aucun wrapper de Toast
n’est justifié.

L’erreur de chargement initial reste bloquante : elle doit continuer à occuper la page avec un message
fonctionnel et `Réessayer`. Les succès et erreurs de sauvegarde sont temporaires et conviennent à un
Toast. La réinitialisation locale produit déjà un résultat visible et ne nécessite pas de Toast.

### Route et modifications non enregistrées

`../Frontend/Rhis_report_gen/src/app/app.routes.ts`

- `/administration/datasets` possède seulement `canActivate: [adminGuard]`.
- Aucune route du projet n’utilise `canDeactivate`.
- Aucune occurrence de `beforeunload` ou d’un mécanisme équivalent n’existe dans le frontend.

Angular expose un `CanDeactivateFn<T>` stable pour bloquer une navigation interne. Ce guard ne couvre
pas la fermeture ou le rechargement de l’onglet ; un gestionnaire `beforeunload` conditionnel à
`dirty()` est également nécessaire pour la protection complète demandée. La confirmation native du
navigateur est préférable à un nouveau système de dialogue global pour ce cas précis.

### Tests existants

`dataset-exposure.model.spec.ts`

- couvre les quatre conversions booléens ↔ mode.

`dataset-exposure.service.spec.ts`

- couvre GET, PUT et `withCredentials`.

`dataset-exposure.component.spec.ts`

- couvre la conservation d’un champ lors du passage à `NONE` ;
- couvre l’ancienne recherche globale sur les seuls `displayName` ;
- couvre l’absence d’appel avant sauvegarde et le delta envoyé.

La fixture actuelle ne contient qu’une table et un champ. Elle ne peut pas prouver sélection,
navigation entre tables, brouillons multi-tables, recherches indépendantes, états responsives,
réinitialisation, erreurs asynchrones ou Toast.

Commande exécutée le 2026-08-26 :

```powershell
npm.cmd test -- --watch=false --include="src/app/features/administration/dataset-exposure/dataset-exposure.model.spec.ts" --include="src/app/features/administration/dataset-exposure/dataset-exposure.service.spec.ts" --include="src/app/features/administration/dataset-exposure/dataset-exposure.component.spec.ts"
```

Résultat : 6 tests réussis, 0 échec, Chrome 151. Cette commande établit uniquement la baseline ciblée ;
la suite complète et le build n’ont pas été relancés pendant la recherche.

## Data and control flow

### Lecture et sélection proposées

```text
Ouverture de /administration/datasets
  -> GET /api/v1/admin/dataset-exposure
  -> réponse complète
  -> copie immuable vers baseline et draft
  -> selectedDatasetId = null
  -> recherche table filtre seulement le panneau maître
  -> clic sur une ligne -> selectedDatasetId + détail dérivé du draft
  -> recherche champ filtre seulement les champs du dataset sélectionné
```

Le choix proposé de ne pas auto-sélectionner la première table rend l’état « aucune table
sélectionnée » observable et respecte l’affichage mobile initial de la liste. L’approbation de
l’ExecPlan validera ce choix ; sélectionner automatiquement la première table resterait une variante
locale sans impact API.

### Modification et navigation interne

```text
Modification mode/champ
  -> mise à jour immuable du draft global
  -> changes() recalcule tous les deltas
  -> dirtyDatasetIds marque les lignes concernées
  -> sélection d’une autre table : aucun HTTP, aucune copie, aucun reset
  -> retour à la première table : mêmes valeurs de draft
```

La sélection, les deux recherches et l’état mobile sont des états UI séparés du draft. Filtrer une
table sélectionnée hors du panneau gauche ne doit ni effacer la sélection ni fermer son détail sur
desktop.

### Sauvegarde et feedback

```text
Clic Enregistrer
  -> snapshot changes()
  -> PUT unique
  -> succès : réponse -> baseline + draft, dirty=false, Toast success
  -> erreur : draft inchangé, dirty=true, Toast error fonctionnel
```

Les erreurs HTTP complètes peuvent être journalisées avec un contexte stable dans la console de
développement, mais `ProblemDetail.detail` n’est jamais injecté dans le texte visible. L’échec du GET
suit un flux distinct : état d’erreur local persistant + bouton `Réessayer`, sans Toast unique.

## Invariants and constraints

- Les deux booléens backend restent l’unique source de vérité du mode table.
- `active` reste technique et read-only pour tables et champs.
- Passer à `NONE` ne change aucun `field.visible`.
- Sélectionner, rechercher ou revenir à la liste n’émet aucun HTTP et ne perd aucun draft.
- Le PUT reste explicite, unique, atomique et limité au delta de toutes les tables modifiées.
- Une erreur de PUT conserve intégralement le draft.
- Le backend et les DTO ne changent pas.
- Les noms techniques et types ne sont pas affichés puisqu’ils sont absents du contrat admin.
- Une seule instance globale `p-toast` existe dans l’application.
- Les messages visibles sont fonctionnels ; les détails backend bruts restent hors de l’UI.
- L’erreur initiale reste dans la page avec `Réessayer`.
- La barre d’actions reste accessible avec une longue liste et ne masque pas le contenu.
- Sous 1024 px, la liste et le détail sont des vues progressives ; à partir de 1024 px, ils restent
  simultanément visibles.
- La sélection et les statuts ne dépendent pas uniquement de la couleur.
- Aucun store, facade, mapper, wrapper PrimeNG ou dépendance supplémentaire n’est introduit.

## Réutilisabilité observée

La recherche a porté sur `src/app/shared`, les composants de rapports et les occurrences de lignes,
badges, états vides, erreurs et actions sticky.

| Candidat | Observation vérifiée | Décision de planification |
|---|---|---|
| Ligne de table maître | Nouvelle responsabilité propre à cette page ; aucune autre implémentation équivalente | Garder un bouton de sélection local dans la boucle `@for` |
| Ligne de champ | Répétée comme donnée dans une seule boucle, pas comme code dupliqué ; règles très liées au draft de la page | Garder la ligne locale ; aucun `input`/`output` de composant enfant |
| Indicateur actif/inactif | Des présentations voisines existent, mais pas de composant partagé ni d’API visuelle stable | Garder un markup local textuel avec icône ; ne pas créer de badge générique |
| États vide/erreur | Plusieurs écrans ont leur propre wording, actions et contexte de reprise | Ne pas extraire un empty/error state générique dans cette tâche |
| Barre d’actions | Le brouillon et le PUT groupé sont spécifiques à l’exposition | Garder la barre dans le composant page |
| Toast | Infrastructure transversale PrimeNG standard | Installer une seule instance racine, sans wrapper applicatif |

`ReportRelatedCardComponent`, les éditeurs de configuration et `PreviewDialogComponent` portent des
responsabilités métier différentes. Les réutiliser ou les généraliser couplerait des flux sans
supprimer de duplication réelle.

Aucun nouveau composant réutilisable n’est donc proposé. La règle conditionnelle demandant de
documenter une API `input`/`output` ne s’applique pas.

## Écarts entre l’interface actuelle et la cible

| Domaine | Actuel | Cible |
|---|---|---|
| Structure | Accordéons multiples | Liste maître + détail unique |
| Mobile | Empilement simple | Liste puis détail avec retour |
| Sélection | Ouverture de panneaux | `selectedDatasetId` explicite |
| Recherche | Une recherche table/champ | Deux recherches indépendantes |
| Dirty par table | Compteur global seulement | Marqueur sur chaque table modifiée + résumé global |
| Champs | Grille sans recherche locale | Grille/table filtrée de la table sélectionnée |
| Actions | Enregistrer seulement | Annuler les modifications + Enregistrer |
| Accessibilité sélection | Accordéon PrimeNG | Boutons natifs, focus visible, texte sélectionné |
| Feedback sauvegarde | `p-message` dans la page | Toast global temporaire |
| Erreur initiale | Message page + Réessayer | Conservé, avec wording fonctionnel |
| Détail backend | Potentiellement affiché brut | Jamais affiché à l’utilisateur |
| Sortie dirty | Aucune protection | `CanDeactivateFn` + `beforeunload` |
| Style | Palette locale et gradient | Tokens PrimeNG / direction `DESIGN.md` |

## Relevant files and symbols

- `../Frontend/Rhis_report_gen/src/app/app.config.ts` — providers globaux ; aucun
  `MessageService` aujourd’hui.
- `../Frontend/Rhis_report_gen/src/app/app.ts` et `app.html` — racine idéale pour l’unique Toast.
- `../Frontend/Rhis_report_gen/src/app/app.routes.ts` — route admin qui recevra
  `canDeactivate`.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.ts`
  — orchestration, baseline/draft, calcul du delta et feedback actuel.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.html`
  — accordéons et états à remplacer.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.scss`
  — layout, responsive et couleurs à réaligner.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.spec.ts`
  — principal fichier de tests à étendre.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.model.ts`
  — conversions à réutiliser sans modification prévue.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.service.ts`
  — transport à préserver sans modification prévue.
- `src/main/java/RHIS/com/RHIS/dataset/controller/dto/DataSetExposureConfigurationResponse.java`
  — preuve de l’absence de nom technique/type dans le contrat.

## Risks and unknowns

| Item | Type | Impact | How to resolve |
|---|---|---|---|
| Sélection initiale desktop non spécifiée | Assumption | Change le premier état du panneau droit | Le plan propose aucune sélection ; l’approbation valide ce choix |
| `beforeunload` affiche un texte contrôlé par le navigateur | Constraint | Le wording exact ne peut pas être garanti | Tester la présence du blocage, pas le texte natif |
| Responsive réel difficile à prouver uniquement par Karma | Risk | Régression CSS possible malgré tests unitaires verts | Test DOM de la navigation progressive + vérification manuelle 320/768/1024/1440 px |
| 200 champs sans virtualisation | Risk faible | Liste longue mais volume connu et borné | Garder rendu simple ; vérifier scroll/sticky, n’ajouter virtual scroll qu’après mesure |
| Toast racine absent des tests isolés | Risk | Injection manquante ou tests fragiles | Fournir `MessageService` globalement et un spy dédié dans le spec de page |
| Erreurs techniques déjà affichées ailleurs dans l’app | Hors périmètre | Incohérence globale persistante | Corriger uniquement cette page ; ouvrir une tâche séparée si harmonisation souhaitée |

## Conclusions for planning

- Le backend, le service Angular et les conversions de modèle répondent déjà au besoin ; la refonte
  est frontend-only.
- `baseline`/`draft` doit rester dans le composant page. Ajouter un store ou un facade ne résout aucun
  problème concret.
- Le master-detail requiert seulement des Signals UI (`selectedDatasetId`, recherches distinctes,
  vue mobile) et des `computed()` dérivés du draft existant.
- Le Toast doit être global et unique, avec `MessageService` direct. Les messages inline de
  sauvegarde et la lecture de `ProblemDetail.detail` doivent disparaître de cette page.
- La protection dirty nécessite deux frontières complémentaires : route Angular et navigateur.
- Aucun composant réutilisable ne se justifie par le code inspecté.
- Le plan doit étendre substantiellement les tests du composant, ajouter les tests du guard et du
  Toast racine, puis terminer par build, suite frontend et vérifications manuelles responsive/clavier.

## Open questions

Un seul choix de comportement est soumis implicitement à l’approbation de l’ExecPlan : ne sélectionner
automatiquement aucune table après le chargement. Cela rend l’état obligatoire « aucune table
sélectionnée » réel et garantit que le mobile commence sur la liste. Si le produit préfère afficher
immédiatement la première table sur desktop, le plan devra être ajusté avant implémentation pour éviter
un comportement dépendant du viewport difficile à maintenir.
