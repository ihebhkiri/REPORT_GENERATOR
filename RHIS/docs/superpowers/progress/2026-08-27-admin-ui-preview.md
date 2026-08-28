# Administration UI — progression

Mise à jour : 2026-08-28, Europe/Berlin. Task slug : `admin-ui-preview`.
Phase : implémentation livrée le 28 août 2026, validation avec limites documentées.
État final prioritaire : `2026-08-28-admin-ui-preview-validation.md` dans ce dossier.

## Phase 2 — état courant (prioritaire sur l'historique ci-dessous)

- Phrase explicite reçue. Plan : `docs/superpowers/plans/2026-08-28-admin-ui-preview.md`.
- Layout partagé créé dans frontend `src/app/shared/page-layout/` (TS/HTML/SCSS,
  specs composant et routes), utilisé sur la seule source `/rapports` et datasets.
- Routes frontend modifiées, titre/header supprimés du template source Rapports.
  Configuration/Export/login restent sans ce layout. Guards métier inchangés ;
  canDeactivate est sur le composant datasets enfant du layout.
- Datasets : barre conditionnelle avec animate.enter/leave, erreur persistante,
  titres restaurés, alignement horizontal du select, focus et styles de la maquette.
- Copies préimplémentation : `opendesign/mockups/admin-ui-preview/implementation-baseline/`.
  Aucun commit/push. Aucun backend/API/dépendance modifié. Ponytail full.
- Tests baseline datasets : 19 réussis, 2 échecs (titre/focus manquant et Indisponible).
  Avec les nouveaux tests avant correction : 19 réussis, 5 échecs.
  Après correction : 24/24 réussis. Header : 2/2 réussis.
- Suite complète finale : 135 réussis, 1 échec ExportComponent (Option B, attente d'une p-tag).
  Reproduit seul : 18 réussis, 1 échec ; fichiers Export identiques à HEAD.
- Plusieurs builds ont révélé le budget CSS 8 kB, ramené de 10.13 à 8.28 puis 8.08 kB.
  Styles simplifiés, build final réussi à 7.96 kB ; aucun seuil augmenté.
- Rendu à vérifier avec build réel et serveur de fixtures HTTP isolé :
  `node opendesign/mockups/admin-ui-preview/verify-angular.cjs` (session 39733),
  URL `http://127.0.0.1:4390/__test?reset=1` ; ne proxyfie aucun backend.
  Backend local /auth/me : délai de contrôle 3 s dépassé, aucune validation intégrée.
- Navigateurs : binding `iab`, onglet `angularTab` livré, viewport réinitialisé.
  Ancienne maquette 4387 distincte du build Angular 4390 avec fixtures.
- 32 tests ciblés réussis ; 22 contrôles navigateur enregistrés. Focus corrigé
  via afterNextRender après diagnostic de la course avec le rendu coalescé.
- Prochaine action utilisateur : vérifier le rendu et l’intégration avec un backend
  et une session réels. Aucun commit/push. Graphify refusé par le runtime, non contourné.

## Historique de phase 1

## Objectif et limites

Améliorer le header admin et l'écran datasets, avec un aperçu isolé validé avant
toute implémentation. Aucune modification des API, permissions, règles métier,
backend ou dépendances de production. Extension du header à `/rapports` demandée
explicitement par l'utilisateur après le premier aperçu. Aucun commit/push/refactoring
hors périmètre. Pas de subagents ni boucle autonome.

## État du checkout

Backend : `C:/Users/Surface Pro/Downloads/RHIS/RHIS`.
Frontend : `C:/Users/Surface Pro/Downloads/RHIS/Frontend/Rhis_report_gen`.
Les commandes Git depuis les deux dossiers résolvent la racine commune
`C:/Users/Surface Pro/Downloads/RHIS`, branche `codex/dataset-exposure-master-detail`,
baseline `06259e0`. Écart aux instructions décrivant deux dépôts indépendants :
constaté uniquement ; ne pas changer l'organisation Git.

Changements préexistants à préserver (chemins relatifs au frontend sauf indication) :

- `src/app/app.config.ts`, `app.html`, `app.routes.ts`, `app.spec.ts`, `app.ts` ;
- `src/app/features/administration/dataset-exposure/dataset-exposure.component`
  avec extensions `.html`, `.scss`, `.spec.ts`, `.ts` ;
- non suivis : `pending-dataset-exposure-changes.guard.ts` et `.spec.ts` dans ce dossier ;
- backend : `docs/flows/01-dataset-selection-and-configuration-load.md` modifié,
  `docs/plans/2026-08-24-dataset-exposure-master-detail.md` et
  `docs/research/2026-08-24-dataset-exposure-master-detail.md` non suivis ;
- racine parente : `CHANGELOG.md` non suivi.

Ajouts de cette tâche : cette note, la recherche, la spec
`docs/superpowers/specs/2026-08-27-admin-ui-preview-design.md` et le prototype isolé
`opendesign/mockups/admin-ui-preview/` (sources, assets locaux, serveur, captures,
README et résultats de vérification). Aucun fichier de production modifié.

## Travail effectué et décisions

- Skill brainstorming lu et appliqué ; contexte, instructions, flow, styles,
  composant, tests, modèles, service, guards et diff existant inspectés.
- Barre identifiée : `.action-bar`, actuellement visible même sans delta. `dirty`
  compare déjà le brouillon global aux valeurs enregistrées. Conserver cette règle.
- Sélection de table sans perte ni confirmation ; annulation globale ; succès
  remplace baseline ; échec conserve le brouillon ; guards de sortie à préserver.
- Contrôle de mode p-select à quatre choix : conservation proposée puis approuvée
  par l'utilisateur avec la direction. Checkbox actuelle = exposition d'un champ.
- Aucun layout admin ni seconde page admin existante dans les routes. La première
  proposition Utilisateurs fictive a été remplacée par Rapports à la demande utilisateur.
- Références de titres/focus non satisfaites dans le template ; tests correspondants
  lus mais non exécutés. À traiter seulement dans la future implémentation approuvée.
- Direction approuvée par « ok je valide » le 2026-08-27. Cet accord autorise l'aperçu,
  pas l'implémentation Angular. Maquette non encore validée.
- Skills frontend-design, interactive-prototype et browser:control-in-app-browser
  utilisés. systematic-debugging utilisé pour distinguer les transitions et limites
  d'automatisation des défauts de l'interface. Aucun skill manquant installé.
- Prototype React à deux pages avec SharedHeader unique ; données fictives, mode,
  champs, delta global, annulation, succès/échec/réessai, protection de sortie et mobile.
- React/ReactDOM 18.3.1 téléchargés avec autorisation dans le prototype uniquement.
  Logo et PrimeIcons copiés depuis le frontend. Aucun changement de dépendance Angular.
- Révision du header : Rapports vers `/rapports` visible pour les profils connectés,
  Données vers `/administration/datasets` uniquement pour `ROLE_ADMIN`, valeur vérifiée
  dans `adminGuard` et le modèle CurrentUser. Le guard et le backend restent inchangés.
- Profil simulé ADMIN/non-admin, chemins locaux navigables et protection de sortie
  conservée. La vue Rapports démontre le header ; ses sources ne sont pas interactives.
- Skeleton conservé conformément au retour utilisateur. Tous les fichiers modifiés
  dans cette révision restent dans l'artefact et les notes de cette tâche.

## État de validation

| Vérification | Résultat |
| --- | --- |
| `git status --short`, `git log -5 --oneline` depuis les deux dossiers | Réussis après autorisation ; baseline sale documentée |
| `git diff --stat`, `git diff --` racine Angular et composant TS/HTML | Changements existants inspectés |
| `git rev-parse --show-toplevel --git-dir --abbrev-ref HEAD` | Racine Git commune constatée |
| Sources, styles, tests, flow et DTO/service admin | Inspection statique uniquement |
| Tests Angular / build / Maven | Non exécutés ; aucune source modifiée |
| `node build.cjs` dans le dossier prototype | Premier essai bloqué par accès sandbox ; compilation réussie ensuite avec autorisation, puis recompilation réussie après correction des boutons mobile |
| `node --check app.js`, `node --check serve.cjs` | Contrôles syntaxiques ; premier app.js absent après le build refusé, vérification réussie après compilation |
| `node serve.cjs` | Serveur local redémarré sur 127.0.0.1:4387, session exec 15802 ; routes locales ajoutées |
| Navigateur intégré / simulations | 45 assertions enregistrées dans screenshots/verification.json, sans erreur console dans le dernier relevé ; scénarios principaux vérifiés |
| Responsive | Pas de débordement horizontal mesuré à 320, 390 et 768 px ; captures bureau à 1440 × 1040 et mobile à 390 × 844 |
| Révision du header | `node build.cjs`, `node --check app.js`, `node --check serve.cjs` réussis ; contrôles ciblés dans screenshots/header-verification.json |
| Préservation des changements existants | SHA256 des 15 fichiers préexistants comparés à la baseline : tous identiques |

## Précisions de validation

- Barre absente à l'état propre ; retour aux valeurs initiales, reset global, succès
  et nouvelle baseline vérifiés. Échec garde le brouillon ; fermeture du toast ne
  supprime pas le message d'erreur de la barre. Dernier réessai réussi.
- Sélection d'autres tables conserve le brouillon ; sortie de page confirmable.
- Focus mobile vers le titre, retour sur la table et restitution après annulation
  vérifiés. Focus visible vérifié. Activation des checkboxes par espace non confirmée
  par l'outil ; contrôle natif à vérifier manuellement, ne pas annoncer ce test réussi.
- Réduction via bouton de la maquette vérifiée ; règle prefers-reduced-motion présente,
  mais préférence OS non émulée. Lecteur d'écran, zoom 200 % et autres navigateurs non testés.
- L'outil pouvait garder un ancien nom accessible après modification du texte. Quelques
  contrôles ont demandé une lecture DOM fraîche ou dom_cua. Une assertion initiale
  attendait trop tôt le retrait du DOM pendant la transition de sortie ; attente corrigée.
- La maquette utilise des contrôles natifs stylés, pas le runtime PrimeNG ; aucune
  conclusion de validation Angular/production ne doit être tirée de ces tests.
- Les 45 assertions initiales n'ont pas toutes été rejouées après la révision du header.
  Navigation, rôles, brouillon avant sortie, skeleton et responsive ont été revérifiés.
  Les captures actuelles portent le suffixe v2 ; les anciennes captures sont historiques.

## Prochaine action sûre

Attendre la validation explicite de la maquette, ou adapter uniquement l'aperçu selon
les retours. URL http://127.0.0.1:4387 ; si le serveur n'est plus actif, relancer
`node serve.cjs` dans le dossier prototype. Le fichier app.js est déjà compilé.
Le deuxième écran est une démonstration de réutilisation, pas une nouvelle route métier.
Ne pas écrire dans le frontend hors workspace sans l'autorisation requise.

Après livraison de l'aperçu, attendre explicitement :
« Je valide cette maquette, passe à l’implémentation. »
Puis seulement utiliser writing-plans et préparer la phase Angular.
