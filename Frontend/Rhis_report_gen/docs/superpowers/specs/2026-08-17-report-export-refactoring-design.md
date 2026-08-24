# Refactoring ciblé de la page Export

## Objectif

Améliorer la lisibilité, le naming et la maintenabilité de la page Angular d’export sans modifier son comportement métier, son rendu validé, ses contrats HTTP, ses modèles, ses routes, son stockage ou son workflow de génération et de téléchargement.

Le nombre de lignes n’est pas un critère d’extraction. Un fichier long mais cohérent est conservé lorsqu’un découpage obligerait à naviguer entre plusieurs fichiers sans isoler une responsabilité réellement autonome.

## Périmètre analysé

- `export.component.ts` ;
- `export.component.html` ;
- `export.component.scss` ;
- `export.component.spec.ts` ;
- les services et modèles directement utilisés par la page, uniquement pour vérifier les responsabilités et les contrats existants.

## Diagnostic

### Responsabilité du composant

Le composant Export orchestre une seule expérience utilisateur : suivre la génération du rapport, sélectionner indépendamment PDF et Excel, suivre leur création puis télécharger les fichiers prêts. Ses états synchrones, ses appels asynchrones, la reprise après rechargement et le nettoyage à la navigation appartiennent à ce workflow de page.

Malgré sa taille, le composant ne justifie donc pas une extraction en plusieurs composants, une façade, un store ou un nouveau service. Une telle séparation augmenterait le nombre de contrats et la navigation entre fichiers sans isoler une responsabilité métier autonome.

### Naming ciblé

Certains noms masquent leur intention ou le type de donnée représenté :

- `generation` et `generationId` ne précisent pas qu’il s’agit de la génération du rapport ;
- `availableFormatCards` et `downloadFormatCards` représentent des options d’export dérivées, et non des composants visuels ;
- `activateFormat()`, `download()`, `previous()` et `newReport()` ne décrivent pas précisément l’action utilisateur ;
- plusieurs variables locales nommées `current` nécessitent de relire leur contexte pour connaître le domaine représenté.

Les renommages seront limités à ces ambiguïtés concrètes. Les noms déjà précis et les méthodes publiques des services resteront inchangés afin d’éviter un diff transversal sans bénéfice direct.

### État synchrone

Les signaux `selectedFormats`, `creatingFormats` et `formatErrors` représentent trois réalités indépendantes : sélection immédiate, requête de création en cours et erreur propre au format. Ils ne peuvent pas être dérivés uniquement des exports PDF/XLSX et doivent être conservés.

Les signaux PDF et Excel séparés restent lisibles pour deux formats fixes. Les remplacer par un store ou une structure générique ajouterait une abstraction sans simplifier le workflow.

Les collections et résumés déjà exprimés avec `computed()` restent dérivés. Aucun `effect()` n’est nécessaire.

### RxJS et polling

Les méthodes de polling de la génération et des exports dupliquent la même décision de retry pour les erreurs réseau et serveur, ainsi que la valeur temporelle `2_000`.

La stratégie sera mutualisée dans une méthode privée du composant, avec des constantes locales distinguant la fréquence de polling du délai de retry. Aucun opérateur personnalisé ni service supplémentaire ne sera introduit.

Les flux longs utilisent déjà leur mécanisme de terminaison. La souscription directe du nettoyage à la navigation porte sur une requête HTTP finie et doit pouvoir survivre à la destruction du composant ; elle restera donc inchangée dans son comportement.

### Template

Le template appelle plusieurs fois les mêmes sélecteurs ou méthodes dans un même bloc : état d’une étape, export associé à un format, erreur, création en cours et collections d’options.

Des variables locales Angular `@let` et des alias métier seront utilisés pour éviter ces répétitions et alléger les conditions. Le template partagé PDF/Excel est déjà une mutualisation adaptée et restera dans la page.

L’extraction d’un composant de carte de format est rejetée : elle imposerait plusieurs entrées, sorties et états dérivés pour une vue utilisée uniquement dans cette page.

La classe dynamique `export-format-avatar` n’a aucune règle SCSS associée. Elle peut être supprimée sans effet visuel.

### SCSS

Le SCSS ne contient pas de duplication suffisamment importante pour justifier une réorganisation. Seules les déclarations structurelles strictement identiques pourront être regroupées si cela améliore effectivement la lecture.

Aucune valeur visuelle — largeur, espacement, couleur, breakpoint ou override PrimeNG — ne sera modifiée. Les sélecteurs `::ng-deep` nécessaires à la structure des composants PrimeNG resteront en place.

## Design validé

### Structure

- Conserver `ExportComponent` et ses fichiers HTML, SCSS et spec actuels.
- Ne créer aucun composant, service, façade, store, mapper, factory, strategy, resolver, handler, directive, pipe, interface ou classe abstraite.
- Ne déplacer aucune responsabilité vers les services existants.

### Renommages ciblés

Les renommages internes prévus sont :

- `generation` → `reportGeneration` ;
- `generationId` → `reportGenerationId` ;
- `availableFormatCards` → `availableExportOptions` ;
- `downloadFormatCards` → `selectedExportOptions` ;
- `activateFormat()` → `exportOrDownloadFormat()` ;
- `download()` → `downloadExportFile()` ;
- `previous()` → `returnToConfiguration()` ;
- `newReport()` → `startNewReport()` ;
- `pollGeneration()` → `pollReportGeneration()` ;
- les alias et variables `current` → un nom métier correspondant à la valeur manipulée.

Un renommage supplémentaire n’est autorisé que s’il corrige une ambiguïté équivalente rencontrée pendant l’implémentation. Les contrats publics des services, modèles et routes ne changent pas.

### Mutualisation RxJS

- Introduire une constante pour l’intervalle normal de polling.
- Introduire une constante distincte pour le délai de retry après une erreur transitoire.
- Extraire dans une méthode privée locale la décision commune : interrompre immédiatement pour une erreur HTTP cliente non transitoire, sinon signaler l’interruption réseau et réessayer après le délai existant.
- Réutiliser cette méthode dans les deux pollings sans modifier leur cadence, leur terminaison ou leur gestion d’état.

### Simplification du template

- Utiliser `reportGeneration` comme alias du signal principal.
- Utiliser `@let` pour les valeurs réemployées plusieurs fois dans un même bloc.
- Conserver les conditions, libellés, composants PrimeNG, attributs accessibles et `data-testid` existants.
- Conserver le template partagé des cartes PDF/Excel.
- Supprimer uniquement la classe dynamique sans définition CSS.

### Nettoyage SCSS

- Regrouper uniquement des déclarations parfaitement identiques lorsque le regroupement reste plus lisible que leur répétition.
- Ne supprimer aucun sélecteur utilisé.
- Ne modifier aucune valeur produisant un effet visuel ou responsive.

## Changements explicitement rejetés

- découpage motivé uniquement par les 533 lignes du TypeScript ;
- composant dédié à la carte PDF/Excel ;
- composant générique pour les étapes du workflow ;
- service de polling ou façade de page ;
- store global ou local ;
- remplacement des signaux PDF/XLSX par un dictionnaire générique ;
- modification des services, modèles, endpoints ou clés de session storage ;
- modification visuelle ou comportementale de la page.

## Ordre d’implémentation et risques

1. Nettoyage sans comportement : classe HTML morte et alias locaux — risque faible.
2. Renommages TypeScript ciblés et adaptation du template/spec — risque faible.
3. Mutualisation locale du retry et constantes temporelles — risque moyen limité aux flux de polling.
4. Réduction des calculs répétés dans le template avec `@let` — risque faible.
5. Nettoyage SCSS strictement structurel, seulement s’il améliore la lecture — risque faible.
6. Mise à jour et exécution des tests — risque faible.

## Validation

Les tests doivent continuer à couvrir :

- génération `PROCESSING` ;
- génération `FINALIZING` ;
- génération ou export `READY` ;
- génération ou export en erreur ;
- sélection et création de PDF uniquement ;
- sélection et création d’Excel uniquement ;
- sélection indépendante des deux formats ;
- restauration des exports sauvegardés après rechargement ;
- retry et affichage des erreurs pendant la création ou le polling ;
- téléchargement Blob et nettoyage à la navigation.

La validation finale comprend :

- la spec du composant Export ;
- toute la suite de tests Angular ;
- le build de production ;
- la vérification qu’aucun nouveau warning de budget propre au composant n’est introduit.

Le refactoring est accepté seulement si ces vérifications réussissent sans changement du DOM utile, du rendu, des appels HTTP, des transitions d’état ou des données persistées.
