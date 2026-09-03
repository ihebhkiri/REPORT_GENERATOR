# Reconstruction visuelle de la page Configuration

## Objectif

Reproduire aussi fidèlement que possible le contenu principal de la capture de référence au viewport desktop 1664 × 970, tout en conservant le header, la sidebar et le comportement fonctionnel existants.

Le responsive mobile et tablette actuellement implémenté reste fonctionnel. La capture constitue la référence visuelle prioritaire pour le desktop.

## Périmètre

La modification concerne uniquement la page de configuration d’un rapport et ses composants visuels existants :

- `configuration.component.html` et `configuration.component.scss` ;
- `report-steps` ;
- `column-selector` ;
- `filter-editor` ;
- `sort-editor` ;
- `preview-panel`.

Le layout partagé reste inchangé. Aucun header, aucune sidebar et aucune navigation ne seront recréés ou dupliqués.

## Architecture retenue

La solution réutilise la composition Angular existante. Aucun nouveau composant, service, modèle, package ou niveau d’abstraction n’est nécessaire.

Les templates et styles des composants existants seront ajustés au minimum utile. La logique TypeScript, les formulaires, validations, événements, appels API, routes et règles métier restent inchangés, sauf adaptation strictement nécessaire à l’exposition d’un état visuel déjà existant.

## Composition visuelle desktop

Dans la zone principale fournie par le layout existant :

1. Un breadcrumb, un titre de page et un sous-titre introduisent le workflow.
2. Le stepper horizontal occupe toute la largeur disponible dans une card dédiée. L’étape 2 est active, les étapes 1 et 3 restent visuellement secondaires.
3. La zone de configuration utilise une grille à deux colonnes : environ deux tiers pour les colonnes du rapport, un tiers pour les filtres et l’ordre de tri.
4. La card Colonnes conserve ses deux panneaux internes : champs disponibles à gauche et colonnes sélectionnées à droite.
5. Les cards Filtres et Ordre de tri sont empilées avec un espacement régulier.
6. Une barre pleine largeur regroupe l’aide contextuelle à gauche et les actions Précédent/Suivant à droite.
7. L’aperçu en temps réel occupe toute la largeur sous les actions, avec statut, commande de réduction et état vide clairement présenté.

Les proportions, espacements, bordures, rayons, ombres, couleurs, tailles typographiques, badges, icônes et états actifs seront rapprochés de la capture en réutilisant en priorité les tokens et composants déjà installés.

## Responsive

Le comportement existant sous 768 px est conservé, notamment les tabs Configuration/Aperçu et l’accessibilité clavier associée. Aux largeurs intermédiaires, la grille passe en pile afin d’éviter le scroll horizontal. Aucun élément interactif essentiel ne doit devenir inaccessible à 200 % de zoom.

## États et accessibilité

Les états loading, empty, error, disabled, selected et focus existants restent disponibles. Les contrôles conservent leurs noms accessibles, leurs handlers et leur navigation clavier. La couleur ne devient pas l’unique moyen de communiquer un état.

## Hors périmètre

- modification du header ou de la sidebar partagés ;
- duplication du layout global ;
- changement des services, API, modèles, routing ou règles métier ;
- nouveau design system ou nouvelle dépendance UI ;
- refactoring fonctionnel sans rapport avec la fidélité visuelle.

## Validation

- vérifier que la route utilise toujours `SharedPageLayoutComponent` ;
- vérifier l’absence de modification structurelle du layout partagé ;
- exécuter les tests ciblés des composants concernés ;
- exécuter le build de production Angular ;
- contrôler le contenu principal à 1664 × 970 face à la capture ;
- contrôler au moins un viewport mobile et un viewport tablette ;
- vérifier focus visible, navigation clavier et états vide/chargement/erreur ;
- inspecter le diff final pour confirmer l’absence de changement fonctionnel ou hors périmètre.

## Critères d’acceptation

- le header et la sidebar existants sont réutilisés sans duplication ;
- seul le contenu principal est visuellement adapté ;
- la structure desktop correspond à la capture de référence ;
- les interactions, validations, appels API et routes conservent leur comportement ;
- le responsive existant reste utilisable ;
- aucun composant ou package inutile n’est ajouté.
