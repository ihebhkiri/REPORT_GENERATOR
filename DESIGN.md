## Principes fondamentaux

### Les 4 piliers CRAP

| Principe | Signification | Exemple |
|---|---|---|
| **Contraste** | Différencier clairement les éléments selon leur importance ou leur rôle | Bouton primaire plus visible qu’un bouton secondaire |
| **Répétition** | Réutiliser les mêmes patterns visuels pour créer de la cohérence | Même style de boutons, titres, cards et formulaires |
| **Alignement** | Chaque élément doit suivre une structure visuelle claire | Alignement sur une grille, marges et colonnes cohérentes |
| **Proximité** | Regrouper visuellement les éléments qui appartiennent au même concept | Label proche de son champ, actions regroupées |

## Hiérarchie visuelle

- Chaque écran doit avoir un point d’entrée visuel clair.
- La priorité doit être visible via :
  - taille
  - poids typographique
  - contraste
  - position
  - espacement
- Éviter d’avoir plusieurs éléments qui se disputent l’attention.
- Une page doit généralement avoir une seule action primaire évidente.
- Utiliser l’espacement avant d’ajouter des bordures ou des couleurs.
- Les informations secondaires doivent avoir moins de poids visuel que le contenu principal.

## Espacement

- Utiliser une échelle cohérente basée sur **8px**.
- Valeurs principales recommandées :
  - `4px` pour les micro-espacements
  - `8px`
  - `16px`
  - `24px`
  - `32px`
  - `48px`
  - `64px`
- Éviter les valeurs arbitraires comme `13px`, `27px`, `37px` sans justification.
- Les éléments appartenant au même groupe doivent être plus proches entre eux que des groupes voisins.
- Le whitespace est un outil de hiérarchie, pas de l’espace perdu.

## Grille et layout

- Desktop : grille de référence de **12 colonnes**.
- Mobile : grille de référence de **4 colonnes**.
- Ne pas obliger chaque composant à utiliser physiquement une grille 12 colonnes.
- Utiliser la grille comme principe d’alignement global.
- Préférer des layouts simples :
  - stack
  - flex
  - grid
- Éviter les layouts trop imbriqués.
- Garder des largeurs de contenu raisonnables.
- Éviter les containers plein écran pour du texte ou des formulaires courts.
- Les éléments liés doivent partager les mêmes axes d’alignement.

## Densité

- Ne pas maximiser la quantité d’information visible par défaut.
- Préserver une densité adaptée au contexte :
  - formulaire : aéré
  - dashboard : modéré
  - data table : plus dense
- Ne pas utiliser la même densité partout.
- L’espace vertical peut être réduit dans les écrans data-heavy, mais sans nuire à la lisibilité.
- Les zones interactives doivent rester suffisamment grandes.

## Typographie

- Utiliser une échelle typographique limitée.
- Éviter trop de tailles ou de weights différents.
- Exemple de hiérarchie :
  - page title
  - section title
  - body
  - helper text
  - caption
- Utiliser le `font-weight` avec modération.
- Ne pas utiliser le gras partout pour créer artificiellement de la hiérarchie.
- Favoriser une longueur de ligne confortable pour les textes.
- Garder une `line-height` suffisante.
- Éviter le texte entièrement en majuscules sauf pour de très petits labels spécifiques.

## Couleurs

- Utiliser les couleurs du design system ou des tokens.
- Éviter les couleurs codées directement dans les composants.
- Chaque couleur doit avoir une fonction claire :
  - primary
  - secondary
  - success
  - warning
  - error
  - neutral
- Ne pas utiliser la couleur seule pour transmettre une information.
- Conserver un contraste suffisant entre texte et arrière-plan.
- Réserver les couleurs fortes aux éléments importants.
- Une interface cohérente utilise généralement beaucoup plus de couleurs neutres que de couleurs d’accent.

## Formes, bordures et élévation

- Utiliser un nombre limité de `border-radius`.
- Éviter de mélanger des composants très arrondis et très carrés sans logique.
- Les bordures doivent servir à structurer, pas décorer.
- Éviter les nested cards inutiles :
  - card dans card
  - panel dans card
  - container bordé dans container bordé
- Utiliser les ombres avec parcimonie.
- Préférer contraste, whitespace et background subtil avant les grosses shadows.

## Actions

- Une action primaire doit être immédiatement identifiable.
- Les actions secondaires doivent être moins mises en avant.
- Les actions destructives doivent être visuellement distinctes.
- Utiliser des labels explicites :
  - `Enregistrer`
  - `Créer`
  - `Supprimer`
  - `Annuler`
- Éviter les labels ambigus :
  - `OK`
  - `Oui`
  - `Continuer`
  lorsqu’un verbe plus précis est possible.
- Les actions fréquemment utilisées doivent être faciles à atteindre.
- Les actions rares ou avancées peuvent être placées dans un menu secondaire.

## Affordance

- Un élément cliquable doit paraître cliquable.
- Un élément non interactif ne doit pas ressembler à un bouton.
- Les liens doivent être reconnaissables comme liens.
- Les contrôles désactivés doivent être clairement distingués.
- Ne pas dépendre uniquement du hover pour indiquer qu’un élément est interactif.
- Les icônes seules doivent être réservées aux actions très évidentes.

## États d’un composant

Tout composant interactif important doit considérer :

- default
- hover
- focus
- active
- selected
- disabled
- loading
- empty
- error
- success

Ne pas concevoir uniquement l’état nominal.

## Feedback

- Toute action utilisateur doit produire un feedback perceptible.
- Les actions asynchrones doivent indiquer leur état.
- Préférer un loading local au composant concerné plutôt qu’un blocage complet de la page.
- Prévenir les doubles soumissions.
- Afficher les erreurs près de l’endroit où elles se produisent.
- Ne pas afficher directement les erreurs techniques du backend.
- Ne pas afficher systématiquement un toast lorsque le résultat est déjà évident visuellement.

## Formulaires

- Toujours utiliser un label visible.
- Le placeholder ne remplace pas le label.
- Regrouper les champs selon le métier.
- Respecter un ordre logique.
- Réduire le nombre de champs visibles si certaines informations peuvent être demandées plus tard.
- Afficher les erreurs près du champ.
- Les messages d’erreur doivent indiquer comment corriger le problème.
- Ne pas vider le formulaire après une erreur récupérable.
- Utiliser le contrôle correspondant à la donnée :
  - date → date picker
  - booléen → switch/checkbox selon contexte
  - valeur unique parmi plusieurs → select/radio
- Ne pas utiliser un dropdown lorsque 2 ou 3 choix visibles seraient plus simples.

## Tables

- Montrer uniquement les colonnes utiles.
- Les colonnes importantes doivent apparaître en premier.
- Utiliser des labels métier, pas les noms de colonnes DB.
- Aligner les nombres de manière cohérente.
- Formater les dates, montants, pourcentages et unités de manière uniforme.
- Garder les filtres et tris actifs visibles.
- Éviter l’horizontal scroll quand une priorisation des colonnes suffit.
- Ne pas utiliser `DISTINCT` ou une suppression visuelle de doublons pour masquer un problème de cardinalité métier.

## Navigation

- L’utilisateur doit toujours comprendre :
  - où il se trouve
  - d’où il vient
  - quelle est l’étape suivante
- Éviter une profondeur excessive.
- Garder des labels de navigation stables.
- Ne pas utiliser des modals pour des workflows complets.
- Préserver l’état utile lorsqu’un utilisateur revient sur un écran, si pertinent.

## Responsive design

- Ne pas simplement réduire la largeur du desktop.
- Repenser la disposition lorsque l’espace diminue.
- Passer de :
  - plusieurs colonnes
  - à une disposition stack
  si nécessaire.
- Garder les actions importantes accessibles.
- Éviter le scroll horizontal sauf pour les contenus intrinsèquement larges.
- Vérifier :
  - mobile
  - tablette
  - desktop
  - très grand écran
- Les overlays, dropdowns et tooltips doivent rester dans le viewport.

## Accessibilité

- Utiliser du HTML sémantique.
- Tous les éléments interactifs doivent être accessibles au clavier.
- Le focus doit être visible.
- Utiliser ARIA uniquement lorsque le HTML natif ne suffit pas.
- Associer les labels aux inputs.
- Ne jamais communiquer une information uniquement par couleur.
- Respecter les contrastes.
- Les icônes interactives doivent avoir un nom accessible.
- Respecter `prefers-reduced-motion`.

## Motion

- Une animation doit avoir une fonction :
  - montrer une transition
  - expliquer une relation spatiale
  - confirmer un changement d’état
- Éviter les animations décoratives inutiles.
- Garder les animations courtes.
- Ne pas ralentir une action pour rendre l’animation visible.
- Éviter les animations qui provoquent des changements de layout importants.

## Progressive disclosure

- Montrer d’abord ce qui est nécessaire à la tâche principale.
- Cacher les options avancées tant qu’elles ne sont pas nécessaires.
- Ne pas afficher 15 paramètres si 3 suffisent pour la majorité des utilisateurs.
- Utiliser :
  - sections avancées
  - menus secondaires
  - accordions
  lorsque cela réduit la charge cognitive sans masquer une information importante.

## Charge cognitive

- Une interface doit demander le minimum d’effort mental.
- Éviter de demander à l’utilisateur de mémoriser des informations entre deux écrans.
- Préférer reconnaître plutôt que se souvenir.
- Garder les choix simples et contextualisés.
- Éviter trop d’actions concurrentes.
- Montrer uniquement l’information nécessaire à la décision actuelle.

## Prévention des erreurs

- Prévenir une erreur lorsque c’est possible au lieu de l’expliquer après.
- Désactiver ou masquer les options impossibles lorsque la raison est évidente.
- Préférer une contrainte dans le contrôle UI à une validation tardive.
- Confirmer uniquement les actions difficiles à annuler.
- Préférer `Undo` à une confirmation systématique lorsqu’un rollback est possible.

## Loi de Hick

Plus le nombre de choix augmente, plus la décision devient lente.

- Réduire les choix visibles.
- Regrouper les choix.
- Prioriser les options fréquentes.
- Ne pas mettre 15 actions au même niveau.

## Loi de Fitts

Une action est plus facile lorsqu’elle est grande et proche.

- Donner une taille suffisante aux targets interactifs.
- Ne pas créer de petites icônes difficiles à cliquer.
- Positionner les actions fréquentes près de leur contexte.
- Garder une distance suffisante entre deux actions dangereusement différentes.

## Gestalt

Utiliser notamment :

- proximité
- similarité
- continuité
- région commune

pour exprimer les relations visuelles.

Exemple :

```text
Nom
[____________]

Email
[____________]


        [Annuler] [Enregistrer]