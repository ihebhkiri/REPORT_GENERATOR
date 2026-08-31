# RHIS — Modernisation du composer et repli de l’aperçu

**Date :** 30 août 2026  
**Statut :** Design approuvé en session  
**Task slug :** `angular-composer-preview-polish`

## Objectif

Appliquer des retouches visuelles ciblées au frontend Angular : rendre le composer de
RHIS Bot compact et explicite pendant l’attente, afficher l’aperçu de configuration replié
par défaut avec une animation accessible, puis unifier les pages de configuration et
d’export sous le layout partagé.

## Contraintes

- aucune modification du backend, des endpoints, des modèles HTTP ou de la logique métier ;
- aucune nouvelle dépendance ;
- préserver `draftMessage`, `isSubmitting`, les clarifications et `submit()` ;
- conserver `format` dans le body HTTP pour compatibilité, mais fixer sa valeur technique à
  `XLSX` sans l’exposer dans l’interface ; le choix réel reste disponible sur la page export ;
- ne modifier que RHIS Bot, `preview-panel`, les routes rapports et le layout direct des
  pages configuration/export.

## RHIS Bot

Le formulaire devient une capsule pleine largeur contenant uniquement un `textarea` et le
bouton PrimeNG d’envoi circulaire. Le `textarea` commence avec `rows="1"`, utilise le
placeholder « Envoyer un message… », grandit avec son contenu jusqu’à environ 160 px, puis
défile verticalement. La poignée native est supprimée. `Enter` insère une ligne et
`Ctrl+Enter` appelle le `submit()` existant.

Le sélecteur de format, son label et toute mention du format disparaissent. Le composant ne
conserve pas de signal de sélection inutile : `buildRequest()` et les chemins d’erreur qui
doivent produire un `BotReportRequest` envoient directement `format: 'XLSX'` afin de ne pas
modifier le contrat backend.

Le bouton réutilise l’icône `pi pi-send`, possède un nom accessible, et couvre les états
hover, `focus-visible`, disabled et loading. Il reste désactivé si le texte nettoyé est vide
ou si `isSubmitting()` vaut `true`.

Pendant `isSubmitting()`, la liste affiche après les messages persistants une ligne assistant
temporaire « Réflexion » suivie de trois points animés. Cette ligne est dérivée directement
de `isSubmitting` : elle n’est pas ajoutée au signal `messages` et aucun second état de
chargement n’est créé. Les points ne représentent aucun raisonnement interne. Leur animation
est désactivée avec `prefers-reduced-motion`.

## Aperçu de configuration

`ConfigurationComponent.previewCollapsed` est initialisé à `true`. Le chargement automatique
de l’aperçu reste inchangé : replier ou déplier ne déclenche aucun appel HTTP et ne détruit
aucun résultat.

Le header du `PreviewPanelComponent` porte le titre « Aperçu du rapport », conserve les
informations déjà disponibles (statut et nombre de lignes) et reçoit un vrai `button` à
chevron. Le bouton expose `aria-expanded`, cible `report-preview-body` et appelle le
`togglePreview()` existant. Le chevron pivote lorsque le panneau est ouvert.

Le corps est enveloppé par une structure grid : `0fr` et opacité `0` au repos, `1fr` et
opacité `1` à l’ouverture, avec un enfant `min-height: 0; overflow: hidden`. Les transitions
durent 220 ms pour la hauteur et 180 ms pour l’opacité. Aucune valeur arbitraire de
`max-height` n’est utilisée. `prefers-reduced-motion: reduce` supprime les transitions.

## Layout partagé de configuration et d’export

Les routes `/rapports/configuration/:datasetId` et `/rapports/export/:generationId` sont
imbriquées dans `SharedPageLayoutComponent`, sans modifier leurs URLs, paramètres ou
composants lazy-loaded. `LayoutPage` et `PAGE_COPY` reçoivent deux variantes explicites :
`configuration` et `export`. La navigation « Rapports » reste active sur ces variantes.

Les headers locaux de `ConfigurationComponent` et `ExportComponent` sont supprimés. Le
layout partagé devient leur unique header et leur unique `<h1>` :

- configuration : titre et description associés à la définition des colonnes, filtres et
  tris ;
- export : titre « Préparer et exporter le rapport » et description existante du workflow.

Le bouton de fermeture local de la configuration disparaît avec son header ; le lien
« Rapports » du header partagé fournit le retour vers `/rapports`. Le bouton local
« ReportGen Pro » de l’export disparaît également ; les actions métier présentes dans le
contenu de la page export restent inchangées.

Les conteneurs et styles de hauteur sont ajustés au contenu projeté par le layout partagé :
aucune page enfant ne réserve une seconde hauteur de viewport ou une seconde topbar. La
configuration conserve son comportement desktop scrollable et ses onglets mobiles ;
l’export conserve son stepper, sa timeline et son responsive.

## Validation

- tests du composer : absence du select, valeur HTTP `XLSX`, raccourci clavier, indicateur
  temporaire et états du bouton ;
- tests de l’aperçu : état initial replié, bouton natif/ARIA, classes d’ouverture et maintien
  des mises à jour automatiques ;
- tests du routing/layout : un seul header et un seul `<h1>` sur configuration/export,
  paramètres de route préservés et navigation « Rapports » active ;
- tests ciblés Angular des deux composants, puis build production ;
- revue du diff pour confirmer l’absence de changement backend ou hors périmètre.

## Hors périmètre

- rendre `format` optionnel dans l’API ;
- choisir automatiquement PDF ou XLSX depuis le texte ;
- modifier le workflow de la page export ;
- modifier les URLs, guards ou paramètres des routes rapports ;
- streaming, pièces jointes, raisonnement simulé ou nouvel état global.
