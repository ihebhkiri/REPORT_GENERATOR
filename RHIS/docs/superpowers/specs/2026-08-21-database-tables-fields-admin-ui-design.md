# Administration des tables et champs — spécification UI/UX

Date : 2026-08-21  
Statut : validé  
Livrable : spécification textuelle, sans implémentation Angular, backend ou Figma

Cette spécification remplace, pour le périmètre UI/UX courant, la proposition Figma du
20 août 2026. La navigation latérale, la palette indigo et le livrable Figma ne font pas
partie de la direction validée.

## 1. Objectif

Concevoir une page d’administration permettant de parcourir les tables exposées par
l’application, de sélectionner une table et de gérer ses contextes d’affichage ainsi que la
visibilité de ses champs.

Le modèle courant comporte trois booléens au niveau d’une table : `active`, `displayMain` et
`displayRelated`. Les champs conservent deux booléens : `active` et `visible`. Les propriétés
`active` sont techniques et affichées en lecture seule ; `displayMain`, `displayRelated` et
`field.visible` sont les préférences administrables.

La page doit rester compacte et lisible avec 20 à 100 tables et 10 à 200 champs par table.
Elle utilise uniquement des données mockées et ne définit aucun comportement de base de
données, contrat API ou mécanisme de persistance.

## 2. Sources techniques analysées

La cartographie de référence est
`C:/Users/Surface Pro/Downloads/RHIS/graphify-out/graph.json`. Le nœud `DataSetEntity` y est
relié notamment à `DataSetInitializer`, `DataSetRepository`, `DataSetServiceImpl`,
`ReportDefinitionResolver`, `DataSetField` et au modèle Angular `Dataset`.

Le graphe fournit la structure des dépendances, mais son extraction actuelle est antérieure
au renommage de `visible` en `displayMain` et `displayRelated` : ces deux identifiants ne sont
pas encore indexés dans `graph.json`. La sémantique détaillée est donc vérifiée dans les
sources reliées par le graphe :

- `DataSetEntity` déclare `active`, `displayMain` et `displayRelated` ;
- `DataSetInitializer` synchronise `active` avec la présence physique des tables et des
  colonnes ;
- `DataSetRepository.findByActiveTrueAndDisplayMainTrue()` sélectionne les sources
  principales ;
- `DataSetRepository.findVisibleTableRelations()` exige `active=true` et
  `display_related=true` pour les deux extrémités d’une relation ;
- `DataSetField` conserve `active` et `visible` ;
- `DataSetFieldRepository` exige actuellement table active, table `displayMain`, champ actif
  et champ visible pour exposer les champs.

Le frontend Angular ne transporte actuellement pas ces indicateurs dans son modèle
`Dataset`. Cette lacune est un constat d’architecture, pas un contrat à résoudre dans la
présente phase UI/UX.

## 3. Sémantique des tables

Les trois propriétés ont des responsabilités différentes :

| Propriété | Présentation UI | Mutabilité | Signification actuelle |
| --- | --- | --- | --- |
| `active` | `Disponibilité technique` avec `Active / Inactive` | Lecture seule | Indique si la table physique est actuellement découverte. |
| `displayMain` | `Afficher comme source principale` | Modifiable | Autorise la table dans le catalogue des sources principales. |
| `displayRelated` | `Afficher dans les tables liées` | Modifiable | Autorise la table à participer aux relations exposées. |

Les deux propriétés d’affichage sont indépendantes :

| `displayMain` | `displayRelated` | Comportement présenté |
| --- | --- | --- |
| `true` | `true` | Disponible comme source principale et dans les relations. |
| `true` | `false` | Disponible comme source principale, sans relations exposées. |
| `false` | `true` | Non proposée comme source principale, mais disponible comme table liée. |
| `false` | `false` | Masquée dans les deux contextes utilisateur. |

Une table `Inactive` reste visible dans l’administration afin d’expliquer son état et de
conserver sa configuration. `displayMain` et `displayRelated` sont alors affichés en lecture
seule jusqu’à ce que la synchronisation technique redécouvre la table.

Le contrôle `active` n’est jamais présenté comme une commande administrative. Cela évite de
simuler une action que `DataSetInitializer` pourrait annuler lors de la synchronisation
suivante.

## 4. Sémantique des champs

| Propriété | Présentation UI | Mutabilité | Signification actuelle |
| --- | --- | --- | --- |
| `active` | `Active / Inactive` | Lecture seule | Indique si la colonne physique est actuellement découverte. |
| `visible` | `Visible pour les utilisateurs` | Modifiable | Autorise le champ dans les écrans de configuration concernés. |

Un champ inactif conserve sa préférence `visible`, mais celle-ci est affichée en lecture seule
jusqu’à la redécouverte de la colonne. Masquer un champ ne le désactive pas et ne suggère
aucune suppression physique.

## 5. Structure de la page

La page conserve le header actuel de l’application et n’ajoute aucune sidebar.

Sous le header :

- breadcrumb `Administration / Configuration des données` ;
- titre `Configuration des données` en 30 px, poids 600 ;
- description `Gérez les tables disponibles et leurs contextes d’affichage.` ;
- surface master-detail unique, limitée à 1280 px et dimensionnée pour occuper l’espace
  vertical restant sans dépasser le viewport.

La surface principale utilise deux panneaux :

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ Tables — 84              │ Employees                        Active           │
│ Recherche        Filtres │ employees                                         │
│                          │ Source principale  ON   Table liée  ON             │
│ Employees        >       ├───────────────────────────────────────────────────┤
│ Contracts                │ Champs — 126                                      │
│ Departments              │ Recherche                                         │
│ Work hours               │                                                   │
│ …                        │ Libellé / nom technique          État     Visible │
│                          │ Identifiant / id                 Active       ☑   │
│                          │ Ancien code / legacy_code        Inactive     ☐   │
└──────────────────────────┴───────────────────────────────────────────────────┘
```

Ce schéma décrit uniquement la hiérarchie. La réalisation visuelle suit les tokens de
`DESIGN.md`.

## 6. Panneau Tables

Le panneau mesure 340 px à partir de 1200 px et 300 px entre 1024 et 1199 px. Il possède un
header sticky contenant :

- titre `Tables` et compteur total ;
- recherche sur le nom humain et le nom technique ;
- bouton `Filtres` avec compteur de filtres actifs ;
- compteur filtré, par exemple `12 sur 84 tables`.

Le panneau de filtres contient trois groupes indépendants :

1. `Disponibilité technique` : `Toutes`, `Active`, `Inactive` ;
2. `Source principale` : `Toutes`, `Shown`, `Hidden` ;
3. `Table liée` : `Toutes`, `Shown`, `Hidden`.

Les filtres actifs sont résumés sous la recherche avec des libellés supprimables, par exemple
`Main: Hidden` et `Related: Shown`. Cette présentation évite de compresser trois selects dans
un panneau étroit et rappelle que les dimensions sont indépendantes.

Chaque ligne de table est une zone de sélection d’au moins 44 px de hauteur et affiche :

- nom humain sur la ligne principale ;
- nom technique tronqué sur la ligne secondaire lorsque l’espace manque ;
- indicateur technique `Active` ou `Inactive` avec icône et texte ;
- indicateur `Main shown` ou `Main hidden` ;
- indicateur `Related shown` ou `Related hidden` ;
- libellé discret `Modifié` en présence de changements locaux ;
- chevron sur la ligne sélectionnée.

La liste ne contient aucun contrôle de modification inline. L’état sélectionné combine un
fond tonal, un indicateur latéral sombre, une graisse typographique renforcée et le chevron.
Il ne dépend donc pas uniquement de la couleur.

## 7. Panneau de la table sélectionnée

Le panneau droit est flexible. Son header sticky présente :

- nom humain et nom technique de la table ;
- statut `Disponibilité technique` en lecture seule ;
- réglage checkbox `Afficher comme source principale` avec la référence secondaire
  `displayMain` ;
- réglage checkbox `Afficher dans les tables liées` avec la référence secondaire
  `displayRelated` ;
- résumé `98 champs actifs · 87 visibles`.

Les deux réglages sont placés dans deux lignes distinctes. Leur libellé, leur aide et leur
valeur courante restent visibles sans tooltip obligatoire.

Lorsque la table est inactive, les réglages et la liste des champs restent lisibles mais en
lecture seule. Un message neutre indique :

> Cette table n’est plus détectée dans la base. Ses réglages sont conservés en lecture seule.

Pour une table active mais masquée des deux contextes, les champs restent configurables afin
de préparer une réexposition ultérieure.

## 8. Liste des champs

La barre sticky de la liste contient :

- titre `Champs` et compteur total ;
- recherche sur label, nom technique et type ;
- compteur filtré, par exemple `7 sur 126 champs`.

La liste utilise des lignes compactes d’environ 56 px avec trois zones alignées :

1. label humain, nom technique et type ;
2. colonne `État` avec le statut textuel `Active` ou `Inactive` en lecture seule ;
3. colonne `Visible` avec une checkbox modifiable lorsque la table et le champ sont actifs.

Les en-têtes `État` et `Visible` restent affichés pendant le défilement. Les checkboxes sont
alignées verticalement pour permettre un balayage rapide de 200 lignes. Aucun switch sans
libellé, badge répété ou menu d’actions individuel n’est ajouté.

Exemple d’état mixte :

| Label | Nom technique | Type | État technique | Visible |
| --- | --- | --- | --- | --- |
| Identifiant | `id` | UUID | Active | Oui |
| Prénom | `first_name` | VARCHAR | Active | Oui |
| Code interne | `internal_code` | VARCHAR | Active | Non |
| Ancien code | `legacy_code` | VARCHAR | Inactive | Non, lecture seule |
| Créé le | `created_at` | TIMESTAMP | Active | Non |

## 9. Sélection, recherche et filtres

- L’état par défaut sélectionne `employees` et affiche immédiatement ses champs.
- Sélectionner une autre table remplace le détail sans navigation vers une nouvelle page.
- Les modifications locales des autres tables sont conservées pendant la navigation.
- Rechercher ou filtrer ne supprime jamais les changements locaux.
- Si la table sélectionnée ne correspond plus aux filtres, son détail reste affiché pendant
  que le panneau gauche présente les nouveaux résultats.
- Effacer une recherche restaure la liste complète et conserve la sélection.
- La recherche ne déclenche aucune sauvegarde et ne modifie aucune valeur.
- Les filtres `Main` et `Related` peuvent être combinés afin d’isoler chaque quadrant de la
  matrice d’affichage.

## 10. Sauvegarde groupée

Le premier changement sur `displayMain`, `displayRelated` ou `field.visible` affiche une barre
sticky réservée dans le layout afin qu’elle ne recouvre aucune ligne :

```text
3 modifications non sauvegardées                 Annuler   Sauvegarder
```

Les propriétés `active` ne participent jamais au compteur, car elles ne sont pas modifiables.
Le compteur représente le nombre de propriétés administrables modifiées, et non le nombre de
tables.

- `Annuler` restaure les valeurs du dernier état validé.
- `Sauvegarder` affiche `Sauvegarde en cours…` et empêche une seconde soumission.
- Le succès affiche brièvement `Modifications sauvegardées`, puis masque la barre.
- L’erreur affiche `Échec de la sauvegarde. Vos modifications sont conservées.` et propose
  `Réessayer`.
- Changer de table ne demande aucune confirmation puisque le draft reste conservé.

Ces comportements sont uniquement définis pour le prototype UI. Aucun contrat de
persistance n’est supposé.

## 11. États à représenter

### État par défaut

`employees` est sélectionnée. La table est active, visible comme source principale et
autorisée dans les relations. Ses champs présentent des états techniques et de visibilité
mixtes.

### Aucune table sélectionnée

Le panneau droit affiche `Sélectionnez une table pour gérer ses contextes et ses champs.` avec
une icône fonctionnelle discrète. Aucun faux réglage désactivé n’est affiché.

### Recherche active

La valeur saisie reste visible, les correspondances sont mises en évidence uniquement dans
le texte et le compteur annonce le nombre de résultats.

### Aucun résultat

- Tables : `Aucune table ne correspond à votre recherche ou à vos filtres.` avec
  `Réinitialiser les filtres`.
- Champs : `Aucun champ ne correspond à votre recherche.` avec `Effacer la recherche`.

### Source principale uniquement

`Main shown` et `Related hidden` sont visibles ensemble. Un texte secondaire précise que la
table peut être choisie comme source principale mais ne participe pas aux relations exposées.

### Table liée uniquement

`Main hidden` et `Related shown` sont visibles ensemble. La table n’apparaît pas dans le
catalogue principal, mais peut être proposée dans un contexte de relation.

### Table masquée des deux contextes

`Main hidden` et `Related hidden` sont visibles ensemble. La table reste active et ses champs
restent configurables.

### Table inactive

L’indicateur `Inactive`, le message explicatif et la lecture seule sont affichés ensemble. Le
contenu n’est pas rendu illisible par une opacité globale.

### Champ inactif

Le statut `Inactive` et la préférence de visibilité conservée restent lisibles. La checkbox de
visibilité est en lecture seule jusqu’à la redécouverte de la colonne.

### Loading

Les deux panneaux utilisent des skeleton rows de mêmes hauteur et alignement que le contenu
final afin d’éviter les déplacements de layout.

### Aucun contenu

`Aucune table disponible` et `Cette table ne contient aucun champ` sont deux états distincts,
sans action trompeuse.

## 12. Données mockées

La liste principale couvre les quatre combinaisons d’affichage et les états techniques :

| Label | Nom technique | État | Main | Related |
| --- | --- | --- | --- | --- |
| Employees | `employees` | Active | Shown | Shown |
| Contracts | `contracts` | Active | Shown | Shown |
| Departments | `departments` | Active | Hidden | Shown |
| Work hours | `work_hours` | Inactive | Shown | Shown |
| Payroll entries | `payroll_entries` | Active | Shown | Hidden |
| Users | `users` | Active | Hidden | Hidden |
| Roles | `roles` | Active | Hidden | Shown |
| Audit log | `audit_log` | Inactive | Hidden | Hidden |

Les champs de `employees` incluent `id`, `employee_number`, `first_name`, `last_name`,
`email`, `department_id`, `contract_id`, `internal_code`, `created_at` et `updated_at`.

## 13. Direction visuelle

La page suit strictement `DESIGN.md` :

- canvas `#f5f5f5`, surfaces principales `#ffffff`, surface alternative `#fafafa` ;
- texte principal `#0a0a0a`, texte secondaire `#737373`, bordures `#e5e5e5` ;
- Geist, avec Inter comme substitution ;
- corps 14 px, labels 12 à 14 px, sous-titres 18 px, titre de page 30 px ;
- carte principale avec radius 24 px, contrôles avec radius 18 px ;
- grille d’espacement de 4 px et densité compacte ;
- ombre de carte très légère et bordure hairline ;
- icônes PrimeIcons fines et fonctionnelles ;
- rouge `#e7000b` réservé aux erreurs et actions réellement destructives.

Aucun indicateur ne reçoit une couleur chromatique dédiée. Les différences entre `Active`,
`Main` et `Related` reposent sur le texte, l’icône, l’alignement et la hiérarchie, sans multiplier
les badges.

Aucun indigo, gradient, illustration, métrique décorative, carte surdimensionnée ou ombre
colorée n’est introduit.

## 14. Responsive

### À partir de 1200 px

Le master-detail est complet avec un panneau Tables de 340 px et deux zones scrollables.

### De 1024 à 1199 px

Le panneau Tables passe à 300 px. Les noms techniques peuvent être tronqués, mais les trois
indicateurs de table et les deux réglages du détail restent complets.

### Sous 1024 px

La page utilise une navigation séquentielle :

1. liste des tables en pleine largeur ;
2. sélection d’une table ;
3. détail en pleine largeur avec l’action `Retour aux tables`.

Le retour à la liste conserve la recherche, les filtres, la sélection et les modifications
locales. Aucun drawer ou sidebar n’est introduit.

## 15. Accessibilité visuelle

- contraste minimum WCAG AA pour le texte et les contrôles ;
- focus clavier visible avec un contraste d’au moins 3:1 ;
- cibles interactives d’au moins 44 × 44 px ;
- chaque état communiqué par texte, icône et traitement visuel ;
- labels visibles et explicites pour chaque contrôle ;
- ordre de navigation cohérent : recherche, filtres, liste, réglages, champs, sauvegarde ;
- disabled et lecture seule visuellement distincts ;
- aucune spécification technique d’attributs ARIA dans ce livrable UI/UX.

## 16. Écart technique observé

La découverte des relations utilise `displayRelated`, mais `ReportDefinitionResolver` et
`DataSetFieldRepository` vérifient actuellement `displayMain` lors de l’accès aux champs. Une
table configurée `displayMain=false` et `displayRelated=true` peut donc être découverte comme
table liée tout en voyant ses champs refusés par la validation actuelle.

La maquette représente la sémantique cible des trois booléens sans masquer cet écart. Sa
résolution, les règles de validation finales et les contrats d’administration appartiennent à
une phase technique séparée.

## 17. Hors périmètre

- sidebar ou refonte du shell global ;
- maquette ou fichier Figma ;
- code Angular, TypeScript, HTML ou SCSS ;
- composants PrimeNG exécutables ;
- appels HTTP, services, endpoints ou DTO ;
- base de données, migration ou persistance ;
- modification de `DataSetInitializer`, des repositories ou du resolver ;
- actions de masse ;
- suppression de tables ou de champs ;
- refactoring d’écrans existants.

## 18. Critères d’acceptation

- `active`, `displayMain` et `displayRelated` sont présentés comme trois dimensions distinctes.
- `active` est clairement technique et non modifiable pour les tables et les champs.
- Les quatre combinaisons `displayMain/displayRelated` sont compréhensibles sans documentation
  externe.
- La sélection courante est identifiable sans dépendre uniquement de la couleur.
- La page reste exploitable avec 100 tables et 200 champs.
- Une table inactive est lisible en lecture seule sans perte apparente de configuration.
- Une table masquée des deux contextes reste configurable lorsqu’elle est active.
- Les recherches, filtres, compteurs et empty states sont distincts et compréhensibles.
- Les changements locaux survivent à la navigation entre tables.
- La barre de sauvegarde ne compte que les propriétés administrables et ne masque aucune
  ligne.
- Les layouts à 1440, 1200, 1024 et sous 1024 px suivent le comportement défini.
- La palette, la typographie, les rayons et les surfaces proviennent de `DESIGN.md`.
- Aucun wording ne suggère une suppression SQL ou une persistance déjà disponible.

## 19. Interfaces publiques

Aucune interface publique n’est ajoutée ou modifiée. Cette spécification ne définit ni API,
ni modèle TypeScript, ni route, ni schéma de données. Une future phase d’implémentation devra
définir séparément ces contrats après validation du comportement UI.
