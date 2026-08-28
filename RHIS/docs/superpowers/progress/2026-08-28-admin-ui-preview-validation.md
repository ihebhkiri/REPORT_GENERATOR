# RHIS — validation Angular, 28 août 2026

Implémentation de la maquette explicitement validée, en mode Ponytail full.

## Livré

Un `SharedPageLayoutComponent` porte le header de `/rapports` (source seulement) et
`/administration/datasets`. Données est visible uniquement pour ROLE_ADMIN renvoyé
par AuthService.me ; le guard et le backend restent inchangés. Aide/Paramètres déjà
désactivés sur Rapports sont conservés. Configuration/Export/login restent hors layout.

Datasets : titres accessibles, compteurs, sélection, select à quatre modes horizontal,
label via inputId/ariaLabelledBy, skeleton, tables et responsive. La barre est rendue
uniquement sur delta, avec annulation globale, succès/nouvelle baseline, erreur
persistante et réessai. Aucun timer ne supprime un brouillon. CSS animate.enter/leave
et prefers-reduced-motion ; focus après rendu effectif via afterNextRender.

Fichiers frontend modifiés : `src/app/app.routes.ts`,
`src/app/features/rapports/rapports.routes.ts`,
`src/app/features/rapports/pages/source_de_donnes/rapports.component.html`,
`src/app/features/administration/dataset-exposure/dataset-exposure.component.{ts,html,scss,spec.ts}`.
Ajouts : `src/app/shared/page-layout/shared-page-layout.component.{ts,html,scss,spec.ts}`
et `shared-page-layout.routes.spec.ts`.

## Commandes et résultats

Toutes les commandes npm sont exécutées dans `../Frontend/Rhis_report_gen`.
Journaux dans le backend `opendesign/mockups/admin-ui-preview/`.

| Commande | Résultat |
| --- | --- |
| `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/administration/dataset-exposure/*.spec.ts"` | Baseline 19 succès/2 échecs préexistants (titre/focus et libellé). Nouveaux tests avant correction : 19 succès/5 échecs. Après correction : 24/24 succès |
| `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/shared/page-layout/*.spec.ts"` | 2/2 succès avant ajout des tests de routes |
| `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/administration/dataset-exposure/*.spec.ts" --include="src/app/shared/page-layout/*.spec.ts"` | **32/32 succès** après correction du focus ; focused-final.log |
| `npm.cmd test -- --watch=false --browsers=ChromeHeadless` | **135 succès, 1 échec sur 136**, aucun skip ; angular-tests-final.log |
| `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/export/export.component.spec.ts"` | 18 succès, même échec Export reproduit seul ; export-tests.log |
| `npm.cmd run build` | **Succès**, angular-build.log. Avertissements : initial 604.67 kB (seuil 500), CSS datasets 7.96 kB (avertissement 4, erreur 8) |
| `git diff --exit-code -- ../Frontend/Rhis_report_gen/src/app/features/rapports/pages/export` | Succès : Export identique à HEAD |
| `git diff --check` | Succès ; avertissements LF/CRLF uniquement |
| `node --check opendesign/mockups/admin-ui-preview/verify-angular.cjs` | Succès |
| `node opendesign/mockups/admin-ui-preview/verify-angular.cjs` | Build Angular réel + fixtures HTTP, loopback 4390, session 39733 |
| `graphify update .` | Non exécuté : refus de sécurité du runtime ; aucun contournement |

Les premiers builds ont dépassé le budget CSS (10.13, 8.28 puis 8.08 kB).
Règles redondantes simplifiées ; aucun seuil relevé. Karma signale des 404 sur les
polices PrimeIcons `/base/media/`, déjà présents avant modification. Les icônes du
build de production sont visibles dans les captures.

Échec restant : `ExportComponent keeps the Option B action disabled until the selected
file is ready`, `export.component.spec.ts:250`, attend une `.p-tag` absente. Fichiers
Export inchangés et échec reproduit dans sa suite isolée ; laissé hors périmètre.

## Navigateur et limites

22 contrôles dans `screenshots/angular-verification.json` : états propre/modifié,
retour initial, changement de table, annulation, échec persistant après toast et
réessai, header Rapports, non-admin, skeleton, erreur/vide, quatre modes, NONE,
alignement horizontal, 320/390/768 px sans débordement et focus mobile aller/retour.
Captures : `angular-desktop.png`, `angular-mobile.png`, `angular-reports.png`,
`angular-reports-non-admin.png`, `angular-skeleton.png`.

Le navigateur a révélé une course setTimeout/rendu coalescé : le titre précédent
recevait le focus avant d’être retiré. afterNextRender corrige la cause, vérifiée
dans le navigateur puis les tests. Les réponses de `http://127.0.0.1:4390` restent
fictives, sans proxy backend ni persistance. `/__test` n’existe pas en production.

- Backend local /auth/me : contrôle expiré après 3 secondes. Aucun PUT réel, session
  réelle ou test Maven validé. Les tests de guard ne prouvent pas les permissions serveur.
- Préférence OS reduced-motion non émulée ; CSS présent. Lecteur d’écran, zoom 200 %,
  autres navigateurs et clavier exhaustif restent manuels. Pas de relevé console exhaustif.
- Un GET /auth/me de présentation peut s’ajouter à celui du guard ; aucun cache global
  d’authentification ajouté. Le CSS reste proche du plafond 8 kB.
- Les 10 fichiers préexistants hors périmètre sont identiques aux hashes initiaux.
  Les 5 fichiers préexistants concernés ont été sauvegardés puis comparés dans
  `implementation-baseline/` ; services, guards et calcul du delta sont conservés.

Aucun backend/API/dépendance modifié. Aucun commit/push ni refactoring hors périmètre.
