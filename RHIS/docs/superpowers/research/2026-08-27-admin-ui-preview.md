# Administration UI — analyse avant aperçu

Date : 2026-08-27. Task slug : `admin-ui-preview`.
Statut : direction approuvée le 2026-08-27 ; maquette créée, en attente de validation.
Aucune implémentation Angular autorisée. Voir la spec et la note de progression du même slug.

## Périmètre et sources

Header réservé à l'administration, `/administration/datasets`, panneau de détails,
barre des modifications, responsive, accessibilité et transitions. Aucun changement
backend, API, permissions, dépendances ou pages de rapports.

Sources lues : AGENTS.md backend/frontend et parent, `../DESIGN.md`,
`../docs/guidelines/frontend-ui.md`, flow 01, recherche master-detail précédente,
routes/racine/thème Angular, composant dataset-exposure (TS/HTML/SCSS/tests), modèle,
service et guard, headers des pages voisines, contrôleur/service/DTO admin backend.
Skill utilisé : brainstorming. Les étapes frontend-design et interactive-prototype
attendent l'accord sur la direction, conformément à la demande utilisateur.

## Faits observés

- Frontend situé dans `../Frontend/Rhis_report_gen`, hors arborescence backend.
  Particularité de ce checkout : `git rev-parse --show-toplevel --git-dir` depuis
  les deux dossiers résout `C:/Users/Surface Pro/Downloads/RHIS` et son `.git`.
  Branche `codex/dataset-exposure-master-detail`, seul commit récent `06259e0`
  (`mvp is ready`). Cela diffère de la séparation Git annoncée ; ne rien réparer
  ni déplacer. Les responsabilités frontend/backend restent distinctes.
- Nombreux changements préexistants : master-detail, brouillon global, toasts,
  guard de sortie et tests. Ils constituent la référence actuelle à préserver.
- `app.html` contient seulement un toast global et le router-outlet.
  `app.routes.ts` ne contient qu'une page admin : `administration/datasets`.
  Aucun layout d'administration n'existe. Les headers de rapports sont locaux
  et hors périmètre.
- Identité présente : PrimeNG Aura, palette primaire indigo, surfaces neutres,
  police Inter/system-ui, PrimeIcons, grille d'espacement de 4 px. Packages déclarés :
  Angular 20.3.26, PrimeNG ^20.4.0. Ne pas introduire d'autre design system.
- `dataset-exposure.component.html` : « Mode d’exposition » est associé à un
  `p-select`, pas une checkbox. Quatre états : NONE, MAIN_ONLY, RELATED_ONLY,
  MAIN_AND_RELATED, issus de displayMain/displayRelated. Les checkboxes existantes
  pilotent exclusivement `field.visible`. Une checkbox unique ne représente pas
  ces quatre possibilités sans une règle métier supplémentaire.
- La ligne demandée correspond au footer `.action-bar` (vers ligne 295) : résumé
  global, Annuler les modifications, Enregistrer. Toujours rendue actuellement,
  elle indique « Aucune modification » à l'état propre. Ce n'est pas un journal
  d'activité ni un toast de succès.
- Le formulaire utilise FormsModule/ngModel pour les contrôles, mais l'état de
  référence est constitué des Signals `baseline` et `draft`. `changes`, `dirty`,
  `changeCount`, `dirtyDatasetIds` sont calculés par comparaison des valeurs et IDs.
  Ne pas remplacer cette comparaison par l'indicateur dirty d'un formulaire.
- Le brouillon est global : changer de table conserve les modifications sans
  confirmation et réinitialise seulement la recherche de champs si l'ID change.
  Aucune table sélectionnée initialement. Annuler restaure toutes les tables.
- Succès du PUT : baseline et draft remplacés par la réponse complète, toast succès.
  Échec : draft conservé, toast fonctionnel d'erreur. Aucun timer ne remet dirty à faux.
  Contrôles d'édition/actions désactivés pendant saving. Sortie de route protégée par
  CanDeactivate, fermeture/rechargement par beforeunload.
- Table inactive : lecture seule ; mode NONE : champs désactivés, préférences
  conservées ; champ inactif : non modifiable.
- API inchangée : GET/PUT `/api/v1/admin/dataset-exposure`, cookies via withCredentials,
  accès ADMIN au contrôleur, validation puis mutation transactionnelle globale.
  Le DTO ne fournit pas de noms SQL ni types : ne pas en inventer dans le futur écran.
- Écart accessibilité/tests : le template référence `tables-title` et
  `dataset-detail-title` mais ne rend pas ces titres. `#detailHeading` n'est rendu
  que dans l'état vide. Les tests attendent le titre sélectionné et le focus mobile ;
  un test attend aussi « Indisponible », absent du template actuel. Observation
  statique seulement, pas un résultat d'exécution des tests.
- Responsive actuel : bascule liste/détail sous 1024 px, bouton Retour aux tables,
  champs présentés en cartes sous 768 px. Clause reduced-motion déjà présente,
  pas de transitions explicites du panneau ou de la barre dans le SCSS examiné.

## Direction proposée, à approuver

1. **Recommandé : layout d'administration commun.** Header unique, identité RHIS,
   contexte Administration, titre/description de la page et zone d'actions adaptée.
   Même largeur et mêmes alignements que le contenu ; pas d'actions factices dans
   la production. Éviter store global, framework de headers ou nouveau design system.
2. **Alternative : header partagé inclus dans chaque page.** Moins de changement
   de routing, mais chaque page doit respecter le placement et les espacements.
   Pertinent seulement si les pages ont des layouts incompatibles.

Dans les deux cas : conserver master-detail, améliorer sa densité et ses titres,
sélection indigo accompagnée d'un repère, focus visible, états inactifs explicites.
Libellé du mode à gauche, contrôle à droite ; recommander le select actuel pour
préserver les quatre modes, sous réserve de clarification sur « checkbox ».
Barre globale visible uniquement si dirty, conservée pendant saving/échec, retirée
après succès/annulation/retour aux valeurs enregistrées. Prévoir un retour d'erreur
près des actions sans remplacer le toast global existant. Gestion du focus lors de
la disparition des boutons à vérifier. Transitions CSS proposées : 140–180 ms,
fondu/déplacement minimal, sans temporiser l'action ; reduced-motion sans mouvement.

La maquette isolée utilisera des données fictives et un deuxième écran admin
« Utilisateurs — démonstration » pour montrer la réutilisation du header ; cet écran
n'existe pas dans le frontend et ne constitue pas une demande de nouvelle page.
Aucun appel API réel. Simulations prévues : sélection/recherches, édition de plusieurs
tables, retour à baseline, annulation globale, réussite, échec puis réessai, sortie
avec brouillon, lecture seule, viewport étroit et navigation clavier.

## Validation et limites

Lectures et inspection Git uniquement. `git status --short`, `git log -5 --oneline`,
`git diff --stat`, diff des fichiers racine et composant TS/HTML, `git rev-parse`
exécutés. Lecture Git initialement bloquée par dubious ownership dans le sandbox,
puis réussie via commande autorisée ; aucune configuration Git modifiée.
Tests et build non exécutés à ce stade d'analyse. Pas de rendu navigateur ni de
prototype créé. Les constats de styles/focus restent à vérifier visuellement.

## Validation humaine requise

Confirmer la direction, notamment la conservation du sélecteur à quatre modes
malgré le mot « checkbox ». En cas de demande binaire, préciser sa signification
et la conservation des combinaisons existantes avant de concevoir ce contrôle.
Après accord seulement : frontend-design + interactive-prototype, puis vérification
avec browser:control-in-app-browser. Arrêt devant la maquette jusqu'à la phrase
« Je valide cette maquette, passe à l’implémentation. » ; writing-plans ensuite.

## Révision après aperçu — 2026-08-28

L'utilisateur demande explicitement de remplacer Utilisateurs par Rapports, avec
navigation `/rapports`, et Données vers `/administration/datasets` visible seulement
pour le rôle admin. Il confirme le skeleton de chargement. Cela autorise le partage
du header avec Rapports, sans autoriser encore l'implémentation Angular.

Vérification complémentaire : CurrentUser expose `roles: readonly string[]` ;
`adminGuard` vérifie `ROLE_ADMIN`, redirige les autres profils vers `/rapports` et
les erreurs vers `/login`. La future visibilité du menu doit utiliser ce même rôle ;
elle ne remplace pas les autorisations de route et backend. Le prototype permet
de simuler les deux profils et navigue sur les chemins locaux correspondants.
Voir spec, progression et README pour la maquette désormais créée et sa validation.
