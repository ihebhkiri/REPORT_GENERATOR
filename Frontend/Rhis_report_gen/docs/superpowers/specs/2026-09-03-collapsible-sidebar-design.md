# Sidebar repliable

## Objectif et état de validation

Ajouter les états collapsed et expanded à la sidebar existante, sans recréer la navigation. Approche et spécification écrite approuvées dans la conversation le 3 septembre 2026. Implémentation et résultats documentés dans `../plans/2026-09-03-collapsible-sidebar.md`.

## Existant vérifié

- `src/app/shared/page-layout/shared-page-layout.component.{ts,html,scss}` porte la sidebar et le layout partagé.
- Le template contient directement les entrées : aucun modèle de menu séparé à réutiliser.
- Les liens sont `/rapports`, `/administration/datasets` (administrateur uniquement) et `/assistant`. Les autres icônes sont des éléments décoratifs non interactifs.
- `public/rhis-solutions-logo.png` contient déjà le symbole et le nom RHIS SOLUTIONS ; le conteneur actuel masque le nom.
- La largeur actuelle est de 5.25rem, puis 4.5rem sous 64rem. La sidebar est masquée sous 48rem.
- La flèche en bas est actuellement un span décoratif.

## Solution retenue

Un signal local `isCollapsed`, initialisé à `true`, pilote une classe sur le layout existant. Pas de deuxième sidebar, de duplication des entrées, de service, de dépendance ou de persistance. L'état reste attaché à l'instance du composant et revient à collapsed lorsqu'elle est recréée.

En collapsed, conserver les dimensions, le rognage du logo, les icônes et les styles actuels ; aucun label visible. En expanded, utiliser une largeur de 15rem (240px à la taille racine habituelle), révéler le logo complet et afficher les labels à côté des mêmes icônes. La grille existante réserve la largeur nécessaire sans recouvrir le contenu. Conserver le masquage mobile actuel.

Labels des liens : Rapports, Données, Assistant. Labels des éléments décoratifs, déduits des icônes : Applications, Calendrier, Notifications, Utilisateurs, Paramètres et Aide ; l'icône Données reste décorative pour les non-administrateurs. Ces éléments ne deviennent pas des actions et ne reçoivent aucune nouvelle route.

Remplacer uniquement le span de toggle par un bouton natif avec nom accessible, `aria-expanded` et focus visible. Garder l'apparence collapsed de la flèche ; adapter son orientation en expanded. Retirer l'exclusion ARIA du conteneur du footer pour rendre le bouton accessible, tout en conservant l'aide décorative hors de l'arbre d'accessibilité.

Conserver les conditions de rôle, les routes, les icônes et le calcul des états actifs. Ne pas modifier l'authentification, les pages métier ni le header.

## Vérification attendue

- Test ciblé : collapsed initial, clic vers expanded, second clic vers collapsed, un seul aside et aucun lien dupliqué.
- Vérifier les labels et le logo dans les deux états, les routes et les états actifs inchangés, et l'accès Données selon le rôle.
- Vérifier le bouton au clavier, son nom accessible et `aria-expanded`.
- Vérification visuelle aux largeurs desktop, tablette et mobile ; absence de recouvrement et rendu collapsed conservé.
- Exécuter les tests du layout puis le build Angular. Les tests actuels contiennent des attentes potentiellement obsolètes (`.primary-nav` absent du template et ancien titre de configuration) : établir le résultat de référence et distinguer ces problèmes de toute régression.

## Périmètre des fichiers

Implémentation limitée aux fichiers TS, HTML, SCSS et spec du composant partagé, plus le court plan de travail. Préserver les modifications préexistantes dans `export.component.html` et `export.component.spec.ts`. Aucun commit sans demande explicite, conformément aux instructions du frontend.

## Relecture

Périmètre et critères relus : aucun placeholder ; aucun changement de navigation ou de permission ; les libellés des icônes décoratives sont une hypothèse explicite. Ajustement découvert pendant la vérification : défilement vertical de la sidebar sur les fenêtres peu hautes pour garder le toggle sur son fond bleu et accessible ; dimensions inchangées.
