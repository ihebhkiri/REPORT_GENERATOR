# Navigation mobile et actions des rapports

Date : 2026-09-07  
Statut : validé en conversation, en attente de relecture du document

## Objectif

Rendre la navigation principale disponible sur mobile et harmoniser les actions mobiles des pages Configuration et Export avec la couleur primaire du produit.

## Comportement attendu

### Sidebar mobile

Sous 48 rem, l’icône hamburger du header devient un bouton. Elle ouvre la sidebar existante comme un panneau coulissant depuis la gauche, au-dessus du contenu, accompagné d’un fond assombri.

Le panneau se ferme par le même bouton, un clic sur le fond, la touche Échap ou l’activation d’un lien de navigation. Le focus clavier reste visible. Le bouton expose l’état ouvert avec `aria-expanded` et référence la sidebar avec `aria-controls`. Le comportement desktop plié/déplié reste inchangé.

### Page Configuration

Les onglets mobiles « Configuration » et « Aperçu » conservent leur structure et leur comportement clavier. Leur état actif et leur focus utilisent la couleur primaire PrimeNG du projet, Indigo 500 (`#6366f1`), à la place du bleu `#2563eb`.

La carte d’actions contient toujours une action secondaire « Précédent » et une action primaire « Génerer », avec leurs règles actuelles de disponibilité et d’erreur.

En mobile :

- lorsque « Configuration » est active, la carte apparaît après le formulaire de configuration ;
- lorsque « Aperçu » est actif, l’aperçu apparaît avant la carte d’actions.

L’ordre desktop reste inchangé. L’ordre mobile est obtenu par la mise en page CSS des blocs existants ; aucune duplication d’action ou de logique métier n’est introduite.

### Page Export

Le footer conserve ses actions et leur comportement : « Modifier la configuration » à gauche et « Créer un nouveau rapport » à droite.

Leur présentation reprend celle de Configuration : première action secondaire blanche avec bordure neutre, seconde action primaire indigo. En mobile, elles sont empilées et occupent la largeur disponible avec une hauteur tactile d’au moins 44 px. Le desktop conserve une disposition horizontale.

## Direction visuelle

La modification réutilise le système actuel : Indigo 500 `#6366f1` pour l’action primaire, Indigo 600 `#4f46e5` au survol, blanc `#ffffff`, texte principal `#10213f`, texte secondaire `#526683` et bordure `#dfe5ed`. Aucun changement de typographie, aucune nouvelle dépendance et aucun nouvel effet décoratif.

## Fichiers concernés

- `src/app/shared/page-layout/shared-page-layout.component.{ts,html,scss}` : état et rendu du panneau mobile.
- `src/app/shared/page-layout/shared-page-layout.component.spec.ts` ou test de routes voisin : ouverture, fermeture et accessibilité de la sidebar.
- `src/app/features/rapports/pages/configuration/configuration.component.{html,scss}` : ordre mobile et couleur des onglets.
- `src/app/features/rapports/pages/configuration/configuration.component.spec.ts` : ordre et styles contractuels pertinents.
- `src/app/features/rapports/pages/export/export.component.{html,scss}` : variantes et disposition des actions.
- `src/app/features/rapports/pages/export/export.component.spec.ts` : variantes et structure mobile.

## Validation

- Tests Angular ciblés des trois zones.
- Build de production.
- Vérification à une largeur inférieure à 48 rem : ouverture/fermeture de la sidebar, ordre aperçu/actions, couleurs et largeur des boutons.
- Vérification desktop afin de confirmer que l’ordre et la sidebar existants sont préservés.

## Hors périmètre

- Nouvelle navigation mobile distincte de la sidebar existante.
- Modification des routes ou des règles métier de génération/export.
- Refonte générale des pages ou des tokens globaux.

## Auto-revue

La spécification ne contient aucun placeholder. Les comportements de fermeture, l’ordre des blocs, les variantes des actions et le breakpoint sont explicites. Le périmètre forme une correction responsive cohérente et ne requiert aucune nouvelle abstraction.
