# Administration des données du Report Builder — spécification Figma

Date : 2026-08-20
Statut : validé pour maquettage Figma
Livrable de cette étape : maquette haute fidélité, sans implémentation Angular/Spring

## 1. Objectif

Concevoir une interface d’administration permettant de configurer les tables et les champs exploitables par le Report Builder. L’interface doit rester efficace avec de nombreuses tables et plusieurs centaines de champs, distinguer clairement disponibilité, visibilité et suppression, et rendre explicites les modifications non sauvegardées.

La priorité est : modèle métier existant, prévention des erreurs, cohérence avec l’application actuelle, puis esthétique.

## 2. Sources analysées

L’analyse repose en priorité sur `C:/Users/Surface Pro/Downloads/RHIS/graphify-out/graph.json`, puis sur les fichiers qu’il relie directement :

- frontend Angular `Frontend/Rhis_report_gen` : pages de sélection des sources et de configuration, modèles `Dataset` et `DatasetField`, `DatasetService`, `ReportConfigurationLoader` et stockage du draft ;
- backend RHIS : `DataSetEntity`, `DataSetField`, controller, mapper, services, repositories, initialiseur et `ReportDefinitionResolver` ;
- documentation des flux existants dans `RHIS/docs/flows` ;
- versions réelles du frontend : Angular 20.3.26, Angular CDK 20.2.14, PrimeNG 20.4, PrimeIcons 8, Tailwind 4.3 et RxJS 7.8 ;
- documentation Context7 correspondant à Angular 20, PrimeNG 20 et Angular CDK 20.x.

Le design system actuel utilise Inter, PrimeNG Aura personnalisé avec une palette primaire indigo, des surfaces slate claires, des bordures discrètes et des ombres légères.

## 3. Architecture existante pertinente

### Frontend

- `RapportsComponent` charge les datasets et leurs relations, puis permet de choisir un dataset principal et des datasets directement liés.
- `ConfigurationComponent` orchestre la sélection des champs, les filtres, les tris, l’aperçu et la génération.
- `ColumnSelectorComponent` affiche les champs disponibles par dataset et les champs sélectionnés.
- `DatasetService` consomme `GET /api/v1/datasets`, `GET /api/v1/datasets/relations` et `GET /api/v1/datasets/{id}/fields`.
- `ReportConfigurationLoader` n’accepte que les relations directes sortantes depuis le dataset principal et exclut les types non supportés.
- `ReportDraftStorageService` conserve uniquement un draft en `sessionStorage`. Il n’existe pas de rapport sauvegardé persistant dans le modèle actuel.

### Backend

- `DataSetEntity` contient `displayName`, `sourceName`, `active` et `visible`.
- `DataSetField` contient notamment `displayName`, `sourceName`, `active`, `visible`, `position`, `dataType`, `nullable` et `primaryKey`.
- `DataSetInitializer` synchronise `active` avec la présence des tables et colonnes PostgreSQL et préserve la valeur de `visible` des éléments existants.
- Les endpoints publics et `ReportDefinitionResolver` exigent conjointement `active=true` et `visible=true`.
- Les relations sont découvertes dans `information_schema` et ne sont exposées qu’entre datasets actifs et visibles.
- Le `PUT /api/v1/datasets/{dataSetId}` existant est un stub. Aucun contrat d’administration fonctionnel ne doit être supposé dans la maquette.
- `/api/v1/admin/**` est réservé à `ROLE_ADMIN`, mais aucune route d’administration frontend correspondante n’existe actuellement.

## 4. Modèle fonctionnel retenu

La maquette respecte strictement les règles actuelles du code :

```text
Dataset exploitable = dataset.active && dataset.visible

Field exploitable = field.active && field.visible
                    && dataset.active && dataset.visible
```

### Matrice des états d’un dataset

| active | visible | Statut UI | Comportement Report Builder | Panneau d’administration |
| --- | --- | --- | --- | --- |
| true | true | Actif | Disponible comme source et dans les relations | Entièrement modifiable |
| true | false | Masqué | Indisponible partout | Champs configurables afin de préparer une future réexposition |
| false | true ou false | Inactif | Indisponible partout | Lecture seule, configuration conservée |

L’interface ne supprime jamais une table ou une colonne physique. Les termes autorisés sont « Activer », « Désactiver », « Afficher » et « Masquer ». Les termes « Supprimer la table » et « Supprimer le champ » sont exclus.

Lorsque le dataset devient inactif, les valeurs de visibilité précédentes restent affichées mais ne sont pas modifiables. Une réactivation restaure visuellement cette configuration. La maquette exprime ce comportement cible sans définir de contrat API.

Le contrôle `Table active` représente l’action administrative demandée et s’aligne sur le stub `PUT` existant. Il ne signifie pas que cette action est déjà persistée correctement : l’implémentation devra résoudre explicitement le conflit avec `DataSetInitializer`, qui remet actuellement `active=true` lorsqu’une table physique est redécouverte. Cette décision d’implémentation reste hors du périmètre Figma.

## 5. Approche UX retenue

Le pattern est un master-detail :

- panneau gauche de 320 px pour rechercher, filtrer et sélectionner une table ;
- panneau droit flexible pour configurer uniquement la table sélectionnée ;
- aucune navigation vers une autre page lors du changement de table ;
- aucun switch directement dans la liste des tables afin de limiter le bruit visuel ;
- changements locaux conservés lors du passage d’une table à une autre ;
- sauvegarde groupée au niveau de la page.

Les alternatives « tableau avec switches inline » et « tree table tables/champs » sont rejetées, car elles deviennent trop denses et difficiles à parcourir avec de grands volumes.

## 6. Structure de la page

### En-tête

- marque `ReportGen Pro` ;
- contexte `Administration` ;
- breadcrumb `Administration > Données du Report Builder` ;
- titre `Configuration des données` ;
- description `Gérez les tables et champs accessibles aux utilisateurs du Report Builder.`

### Panneau Tables

- titre et compteur total ;
- champ de recherche sur le nom utilisateur et le nom technique ;
- filtres `Toutes`, `Actives`, `Inactives`, `Masquées` ;
- liste scrollable de lignes sélectionnables ;
- nom utilisateur principal ;
- nom technique secondaire si utile ;
- badge textuel `Actif`, `Masqué` ou `Inactif` ;
- compteur `18/25 champs visibles` lorsque les données sont disponibles ;
- indicateur `Modifié` pour les tables comportant des changements locaux.

Une ligne utilise tout son espace comme zone cliquable. La sélection est exprimée par fond, bordure et état de focus, jamais uniquement par la couleur.

### Panneau Détail

- nom utilisateur et nom technique de la table ;
- badge de statut ;
- contrôle `Table active` ;
- contrôle `Visible pour les utilisateurs` ;
- texte d’aide expliquant que masquer ou désactiver ne supprime aucune donnée ;
- compteur global des champs visibles ;
- recherche de champ sur label et nom technique ;
- menu d’actions contenant `Tout afficher` et `Tout masquer` ;
- liste de champs scrollable et virtualisable ;
- chaque ligne affiche label, nom technique, type et checkbox de visibilité.

Le type du champ est une information secondaire. La colonne `supported` n’est pas exposée comme contrôle : les types non supportés sont actuellement filtrés par le loader utilisateur, mais l’administration peut les signaler comme non disponibles si nécessaire.

## 7. Comportement des états

### Dataset actif et visible

- les deux contrôles sont activés ;
- les champs sont modifiables ;
- le badge indique `Actif` ;
- le compteur de visibilité est affiché.

### Dataset actif mais masqué

- le badge indique `Masqué` ;
- un message précise qu’il n’apparaît plus dans le Report Builder ;
- les champs restent configurables afin de préparer sa future réexposition ;
- aucun réglage n’est réinitialisé automatiquement.

### Dataset inactif

- le badge indique `Inactif` ;
- le panneau affiche `Cette table n’est pas disponible dans le Report Builder.` ;
- la visibilité du dataset et des champs est présentée en lecture seule ;
- le contenu reste visible pour permettre à l’administrateur de comprendre la configuration conservée ;
- aucune apparence ne suggère une suppression physique.

### Résultats vides

- aucune table trouvée : message lié à la recherche ou aux filtres, avec action `Réinitialiser les filtres` ;
- aucun champ trouvé : message lié à la recherche, avec action `Effacer la recherche` ;
- aucun champ disponible : message distinct, sans action trompeuse.

## 8. Actions de masse et prévention des erreurs

- `Tout afficher` indique le nombre de champs qui changeront avant exécution ;
- `Tout masquer` ouvre une confirmation mentionnant explicitement le nombre de champs concernés et l’indisponibilité dans le Report Builder ;
- désactiver ou masquer une table présente une explication contextuelle, sans vocabulaire destructif ;
- les opérations individuelles de visibilité ne demandent pas de confirmation ;
- quitter la page avec des changements locaux déclenche une confirmation ;
- changer de table ne déclenche pas de confirmation, car les changements restent dans le draft global de la page.

## 9. Sauvegarde et feedback

La maquette utilise des modifications locales suivies d’une sauvegarde groupée.

Une barre sticky apparaît dès le premier changement :

```text
3 modifications non sauvegardées     Annuler     Sauvegarder
```

Règles :

- le compteur représente les propriétés effectivement modifiées, pas seulement le nombre de tables ;
- `Annuler` demande confirmation uniquement si plusieurs changements seraient perdus ;
- `Sauvegarder` affiche un état loading et empêche une seconde soumission ;
- le succès affiche un feedback discret `Modifications sauvegardées` puis masque la barre ;
- l’erreur conserve toutes les modifications locales et propose `Réessayer` ;
- aucune sauvegarde immédiate n’est simulée, car aucun contrat API stable n’existe aujourd’hui.

## 10. Frames Figma attendues

### `01 — Dataset active`

Vue desktop principale avec plusieurs tables, `Employee` sélectionnée, dataset actif et visible, et 18 champs visibles sur 25.

### `02 — Dataset inactive`

Table inactive sélectionnée, message explicatif, contrôles et champs en lecture seule, configuration antérieure toujours visible.

### `03 — Search & filters`

Recherche `contr`, filtre `Masquées` actif et exemple d’état sans résultat. Les variantes peuvent être regroupées dans une même section Figma.

### `04 — Unsaved changes`

Trois changements locaux répartis sur plusieurs tables, indicateurs `Modifié` dans la liste et barre de sauvegarde sticky.

### `05 — Save feedback`

Variants `Saving`, `Saved` et `Error`, avec conservation des changements dans l’état d’erreur.

### `Components`

Zone dédiée aux composants locaux réutilisables et à leurs variants.

## 11. Composants Figma

- `AdminPageHeader`
- `DatasetListItem`
- `DatasetStatusBadge`
- `DatasetSettingsPanel`
- `FieldVisibilityRow`
- `SearchInput`
- `DatasetFilterControl`
- `UnsavedChangesBar`
- `EmptyState`
- `SaveFeedback`

Un composant n’est créé que s’il est répété, possède plusieurs états ou représente une responsabilité UI distincte. Les frames utilisent Auto Layout et les instances de composants plutôt que des duplications manuelles.

## 12. Design system et iconographie

- police principale : Inter ;
- thème : PrimeNG Aura avec primaire indigo ;
- fond de page slate très clair, surfaces blanches, bordures slate et ombres légères ;
- radius cible cohérent avec l’existant : 8 à 12 px ;
- spacing principal sur une grille de 4 px ;
- aucune illustration décorative ;
- aucune Material Symbol.

Toutes les icônes proviennent de PrimeIcons 8 :

| Usage | PrimeIcon |
| --- | --- |
| Dataset | `pi pi-database` |
| Champs | `pi pi-list` |
| Recherche | `pi pi-search` |
| Filtres | `pi pi-filter` |
| Visible | `pi pi-eye` |
| Masqué | `pi pi-eye-slash` |
| Actif | `pi pi-check-circle` |
| Inactif | `pi pi-ban` |
| Sauvegarde | `pi pi-save` |
| Annulation | `pi pi-undo` |
| Succès | `pi pi-check` |
| Erreur | `pi pi-exclamation-circle` |
| Chargement | `pi pi-spinner` |
| Actions | `pi pi-ellipsis-v` |
| Retour | `pi pi-arrow-left` |
| Administration | `pi pi-cog` |

Une icône ne porte jamais seule la signification d’un état administratif.

## 13. Responsive

La cible principale est desktop.

- à partir d’une largeur confortable, les deux panneaux restent visibles ;
- lorsque le détail ne peut plus conserver une largeur utile, la page passe en navigation séquentielle ;
- la vue étroite présente d’abord la liste, puis le détail en plein écran avec `Retour aux tables` ;
- la barre de sauvegarde reste accessible et ses actions passent sur deux lignes si nécessaire ;
- aucune version ne compresse les deux panneaux au point de tronquer systématiquement les labels.

## 14. Accessibilité

- contraste WCAG AA minimum ;
- focus visible sur toutes les commandes et lignes sélectionnables ;
- labels explicites associés aux switches et checkboxes ;
- zones interactives d’au moins 44 × 44 px lorsque possible ;
- statut exprimé par texte, icône et traitement visuel ;
- ordre de tabulation cohérent : recherche, filtres, liste, réglages, champs, sauvegarde ;
- navigation fléchée plausible dans la liste master-detail ;
- annonces de sauvegarde et d’erreur via une zone de statut ;
- disabled et read-only différenciés : le read-only conserve la lisibilité du contenu ;
- modals de confirmation avec focus initial sûr et retour du focus au déclencheur.

## 15. Impacts fonctionnels à communiquer dans la maquette

- masquer ou désactiver un dataset le retire des sources, relations et champs disponibles ;
- masquer un champ l’empêche d’être utilisé comme colonne, filtre ou tri ;
- un draft en session peut ne plus être restaurable si une référence devient indisponible ;
- une génération qui revalide une définition peut échouer si la configuration change avant son exécution ;
- les snapshots et exports déjà produits ne sont pas modifiés rétroactivement ;
- une primary key masquée peut rester utilisée en interne pour le tri stable sans être proposée à l’utilisateur.

Ces impacts sont des informations de conception. La maquette ne définit ni migration, ni endpoint, ni politique de compatibilité des rapports persistants.

## 16. Hors périmètre

- implémentation Angular, Spring ou SQL ;
- définition des DTO et endpoints d’administration ;
- migration de base de données ;
- modification de `DataSetInitializer` ;
- résolution de la persistance d’une désactivation administrative face à la synchronisation de `active` ;
- gestion de concurrence ou versionnement serveur ;
- persistance de rapports sauvegardés ;
- suppression de tables ou colonnes physiques ;
- refonte générale de la navigation de l’application.

## 17. Critères d’acceptation de la maquette

- les cinq situations demandées sont visibles dans les frames ou variants ;
- le pattern master-detail reste lisible à 1440 px et possède une adaptation étroite documentée ;
- `active`, `visible` et `field.visible` sont distingués sans contredire les filtres actuels du code ;
- une table inactive est read-only sans perte apparente de configuration ;
- les changements non sauvegardés, loading, succès et erreur sont explicites ;
- les recherches et empty states sont représentés ;
- toutes les icônes sont des PrimeIcons 8 ;
- aucun wording ne suggère une suppression SQL ;
- les composants réutilisables possèdent des variants nommés proprement ;
- les layouts utilisent Auto Layout, une hiérarchie de layers compréhensible et les tokens/styles existants lorsqu’ils sont disponibles ;
- une vérification visuelle finale confirme l’absence de texte coupé, chevauchement, contraste insuffisant ou état reposant uniquement sur la couleur.
