# Clone visuel du contenu principal de la page Export

## Objectif

Reproduire fidèlement le contenu principal visible dans l'image de référence sur la page d'export, tout en conservant intégralement le header, la sidebar et le layout partagé existants.

## Périmètre

- Page concernée : `src/app/features/rapports/pages/export/export.component.*`.
- Le header et la sidebar continuent d'être rendus par `SharedPageLayoutComponent`.
- Les services, appels API, modèles, règles métier, routing, validations et handlers d'export restent inchangés.
- Les actions « Modifier la configuration » et « Créer un nouveau rapport » sont conservées et rendues visuellement secondaires sous le workflow.
- Aucun nouveau composant, package, token global ou système de navigation n'est introduit.
- Aucun asset n'est copié ou dupliqué : les deux fichiers déjà présents sous `public/assets` sont réutilisés.

## Direction visuelle

Le contenu reprend la composition de la référence : fond très clair, largeur utile généreuse, stepper horizontal dans une card blanche, puis trois sections indépendantes avec bordure fine, rayon modéré et ombre discrète.

### Hiérarchie

1. Le titre, la description et le breadcrumb restent fournis par le layout partagé.
2. Le stepper principal affiche les trois étapes et met « Export » en évidence.
3. La préparation terminée utilise une coche blanche dans un carré violet.
4. La section Formats présente PDF et Excel sur deux colonnes égales.
5. Chaque format affiche son image existante (`/assets/pdf logo.avif` ou `/assets/ms-excel.jpg`) dans un cadre bleu-gris de 56 × 56 px, son titre, sa description et une action violette pleine largeur. L'image conserve ses proportions avec `object-fit: contain` et possède un texte alternatif descriptif.
6. La section Téléchargement conserve tous ses états fonctionnels et affiche son état vide dans un bandeau bleu-gris.
7. Les actions de navigation finales sont conservées dans une rangée discrète sous les sections.

### Tokens locaux

Les variables PrimeNG existantes restent prioritaires. Les valeurs locales nécessaires suivent la référence :

- accent : violet primaire existant ;
- texte principal : bleu nuit du thème ;
- texte secondaire : bleu-gris atténué ;
- surfaces : blanc et gris bleuté très clair ;
- bordures : gris bleu clair ;
- rayons : environ 10–12 px pour les sections et 8–10 px pour les cards internes ;
- ombres : faibles, uniquement pour détacher les surfaces du fond.

La typographie existante est conservée afin de ne pas introduire de police ni de rupture avec le produit.

## Responsive et accessibilité

- Les deux formats passent en une colonne lorsque la largeur devient insuffisante.
- Les boutons restent pleine largeur dans leur card et utilisables au clavier.
- Les focus visibles, rôles, libellés accessibles et contrastes existants sont préservés.
- Les états loading, pending, ready, error, expired et network interruption restent visibles et compréhensibles.

## Stratégie d'implémentation

Le changement privilégie le CSS scoped de `ExportComponent`. Le template ne sera ajusté que lorsqu'un wrapper ou une classe existante ne permet pas de reproduire la structure visuelle. Le TypeScript ne doit pas changer, sauf correction strictement nécessaire révélée par la vérification.

Le style partagé de `ReportStepsComponent` ne sera modifié que si le rendu de l'étape active ne peut pas être obtenu proprement depuis le composant Export sans affecter les autres pages.

## Vérification

- Test ciblé de `ExportComponent`.
- Build Angular de production.
- Inspection du diff pour confirmer l'absence de modification du header, de la sidebar et de la logique métier.
- Vérification visuelle desktop et mobile du stepper, des trois sections, des deux cards de format et des états dynamiques.

## Critères d'acceptation

- Le header et la sidebar existants sont toujours utilisés sans duplication.
- Le contenu principal ressemble étroitement à la référence par ses proportions, espacements, cards, couleurs, bordures, ombres, boutons et densité.
- Les actions PDF et Excel, le téléchargement, les erreurs, le retour à la configuration et la création d'un nouveau rapport conservent leur comportement.
- Le responsive ne produit pas de défilement horizontal évitable.
- Aucun changement non lié n'est inclus.
