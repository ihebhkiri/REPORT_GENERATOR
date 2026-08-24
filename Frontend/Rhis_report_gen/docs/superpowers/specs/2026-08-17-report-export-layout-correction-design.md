# Correction ciblée du layout de la page Export

## Objectif

Corriger uniquement les problèmes visibles de largeur, de densité, d’overflow et de responsive de la page Angular d’export. Le workflow, les appels API, les modèles, les états métier, le stepper et la timeline PrimeNG restent inchangés.

## Diagnostic

La timeline est limitée à `960px`, mais PrimeNG réserve une colonne `opposite` vide avec `flex: 1`. Le contenu du workflow ne reçoit donc qu’environ la moitié de cette largeur. La grille PDF/Excel divise ensuite cet espace en deux cartes.

Chaque carte utilise trois colonnes internes — icône, texte et bouton — alors que le libellé complet du bouton est incompressible. Cette combinaison comprime le texte et fait déborder les boutons. La progress bar est également affichée en état `READY`, alors que le tag, le message de succès et le bouton communiquent déjà cet état.

Le titre utilise la largeur de `.export-main`, tandis que la timeline possède son propre centrage et sa propre largeur. Cette différence crée deux axes visuels distincts.

Le marker signalé précédemment est explicitement hors périmètre et ne sera pas modifié.

## Design validé — option A

### Composition générale

- Conserver `.export-main` comme enveloppe de page.
- Aligner l’introduction, les messages, la timeline, le chargement et le footer sur un container commun fluide plafonné à `960px`.
- Conserver le stepper dans l’introduction et le rendre fluide dans ce même axe visuel.
- Neutraliser uniquement la colonne `opposite` vide générée par `p-timeline`, puis laisser le contenu occuper toute la largeur restante après le séparateur vertical.

### Cartes de format

- Conserver la grille PDF/Excel à deux colonnes sur desktop.
- Organiser chaque carte sur deux colonnes pour l’icône et le contenu, puis placer le bouton PrimeNG sur une ligne dédiée occupant toute la largeur.
- Appliquer `min-width: 0` aux zones flexibles et autoriser le retour à la ligne des messages longs.
- Utiliser l’option `fluid` du bouton PrimeNG ; aucun bouton HTML personnalisé n’est introduit.
- Conserver les labels complets « Télécharger PDF » et « Télécharger Excel ».

### Densité de l’état READY

- Afficher la progress bar uniquement pendant la création, `PENDING` ou `RUNNING`.
- En état `READY`, masquer la progress bar et le tag interne redondant « Prêt ».
- Conserver le message avec l’icône de succès « Fichier prêt » et le bouton de téléchargement.
- Conserver les tags utiles pour les états en attente, en cours et en erreur.

### Responsive

- Garder les deux cartes côte à côte tant que chaque colonne dispose d’une largeur exploitable.
- Passer la grille sur une colonne sous environ `760px`.
- Réduire les paddings latéraux sur mobile sans introduire de largeur fixe.
- Garantir l’absence d’overflow horizontal avec des colonnes `minmax(0, 1fr)` et des descendants flexibles réductibles.

## Périmètre d’implémentation

Les changements applicatifs sont limités à :

- `export.component.html` pour conditionner la progress bar, le tag READY et activer le bouton PrimeNG fluide ;
- `export.component.scss` pour le container commun, la largeur effective de la timeline, la structure des cartes et le responsive ;
- `export.component.spec.ts` pour vérifier la présentation READY/PROCESSING et les hooks structurels stables.

`export.component.ts`, les services, les modèles, les routes, le stockage, le polling et les téléchargements Blob ne sont pas modifiés.

## Validation

- Deux cartes tiennent côte à côte sur desktop sans overflow.
- Les cartes passent sur une colonne sous le breakpoint retenu.
- Les boutons restent entièrement contenus et conservent leur label complet.
- Aucun texte ne chevauche une action.
- La progress bar est visible pendant la génération et absente en état `READY`.
- Le message « Fichier prêt » et le bouton de téléchargement restent visibles en état `READY`.
- Le workflow et les appels métier restent identiques.
- Aucune dépendance n’est ajoutée.
- Les tests Export, la suite Angular complète et le build de production réussissent sans nouveau warning de budget du composant.
