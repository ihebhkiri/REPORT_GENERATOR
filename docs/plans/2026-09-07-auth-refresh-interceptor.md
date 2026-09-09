# Ajouter le refresh frontend sur 403 et le menu de déconnexion

Date: 2026-09-07
Status: Completed (targeted checks passed; full-suite failures documented)
Research: docs/research/2026-09-07-auth-refresh-interceptor.md

Plan vivant régi par `.agent/PLANS.md`.

## Purpose and observable outcome
Après un 403 provenant de l'API RHIS, appeler POST /api/v1/auth/refresh avec cookies et rejouer une seule fois la requête initiale après succès.
Au clic sur l'avatar du header, afficher un menu Déconnexion ; son action appelle POST /api/v1/auth/logout avec cookies puis redirige vers /login après succès.

## Scope and non-goals
Frontend uniquement. Conserver le déclencheur 403 demandé ; aucune modification du backend ou des rôles, ni redirection automatique après un échec de refresh. Le logout réussi redirige vers /login.

## Current behavior
Voir la recherche : aucun intercepteur, AuthService expose login/me, le backend effectue une rotation des refresh tokens. Le backend utilise 401 pour l'entry point non authentifié.

## Proposed approach
Intercepteur fonctionnel limité à l'API configurée, excluant login/refresh/logout. Ajouter AuthService.refresh() utilisant un Observable partagé uniquement pendant le refresh en cours ; remise à zéro après succès/échec. Cela évite plusieurs rotations concurrentes du même token. Rejouer via next après succès, propager l'erreur si refresh échoue ou si la nouvelle tentative échoue, sans nouvelle boucle. Construire l'URL depuis environment.apiBaseUrl, avec /api/v1 comme fallback quand elle est vide. Ne pas envoyer de cookies à des origines tierces.

Pour le logout, réutiliser un menu popup PrimeNG dans le layout existant et remplacer le span profil par un bouton accessible. Ajouter AuthService.logout(). Désactiver les doubles clics pendant la requête ; en cas d'échec afficher une erreur et permettre une nouvelle tentative, sans prétendre que la session est fermée. Terminer un refresh en cours avant le logout pour éviter qu'une réponse tardive réinstalle les cookies. Préserver le garde de modifications non enregistrées : vérifier le comportement de navigation avant fermeture effective de session et garantir qu'une annulation ne déconnecte pas l'utilisateur. Ne pas ajouter de composant ou service générique.

## Affected files and symbols
Sous `Frontend/Rhis_report_gen/` :
- `src/app/features/auth/services/auth.service.ts` : AuthService.refresh et état temporaire du refresh.
- `src/app/features/auth/auth.interceptor.spec.ts` couvre aussi les contrats HTTP refresh/logout d'AuthService ; les tests login/me existants sont conservés.
- `src/app/features/auth/auth.interceptor.ts` : nouvel intercepteur.
- `src/app/features/auth/auth.interceptor.spec.ts` : scénarios HTTP.
- `src/app/app.config.ts` : enregistrement avec withInterceptors.
- `src/app/shared/page-layout/shared-page-layout.component.ts` : menu, action logout, chargement et erreur.
- `src/app/shared/page-layout/shared-page-layout.component.html` : bouton avatar, menu et retour d'erreur accessible ; préserver le formatage déjà modifié.
- `src/app/shared/page-layout/shared-page-layout.component.scss` : bouton et focus cohérents avec le header.
- `src/app/shared/page-layout/shared-page-layout.routes.spec.ts` : interactions et navigation logout, y compris garde dirty.
- `src/app/features/administration/dataset-exposure/pending-dataset-exposure-changes.guard.ts` : réutilisation de la confirmation avant logout, sans second dialogue après succès.
- `src/app/features/administration/dataset-exposure/pending-dataset-exposure-changes.guard.spec.ts` : contexte d'injection Angular pour le guard.

## Milestone 1: Implémenter et vérifier le comportement
Après approbation, créer une branche dédiée codex/auth-refresh-interceptor sans toucher aux modifications existantes. Ajouter méthode, intercepteur et enregistrement. Tester succès 403 → refresh → retry, échec du refresh, second 403 sans boucle, exclusions, autres statuts et URLs tierces, deux 403 simultanés donnant un seul refresh, nettoyage après échec.

## Milestone 2: Vérification finale
Implémenter le menu et le logout. Tester ouverture au clic/clavier, POST avec cookies, redirection après succès, échec sans fausse déconnexion, double clic et interaction avec refresh/guard. Exécuter le build, relire le diff pour les cookies, les exclusions, les boucles et les changements étrangers. Mettre à jour Graphify si disponible.

## Validation and acceptance
- `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/auth/**/*.spec.ts"` depuis le frontend.
- `npm.cmd run build` depuis le frontend.
- `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/shared/page-layout/*.spec.ts"` depuis le frontend.
- Une requête initiale rejouée au maximum une fois ; un seul refresh pour les 403 concurrents ; erreurs finales visibles aux appelants.
- Un test navigateur réel exige une session backend avec refresh token valide ; déclarer explicitement si non exécuté.

## Risks and rollback
Un 403 de droits produira un refresh inutile puis restera refusé. Les 401 ne déclencheront pas ce mécanisme. Supprimer l'enregistrement de l'intercepteur pour désactiver la fonctionnalité. Préserver les fichiers déjà modifiés par l'utilisateur.

## Progress
- [x] 2026-09-07 — Code et instructions examinés, recherche et plan rédigés.
- [x] Approbation du plan : utilisateur « Je valide le plan, implémente le refresh et le logout ».
- [x] Implémentation refresh/logout terminée.
- [x] Tests ciblés : 31/31 réussis, code de sortie 0 après corrections des tests.
- [x] Build final et revue finale : réussis ; avertissements de budgets conservés.
- [x] Suite complète : exécutée deux fois, 175 réussis et 7 échecs hors tests ciblés, détaillés ci-dessous.
- [x] Graphify : graphify update . réussi, 3025 nœuds / 6162 liens / 197 communautés.

## Surprises & Discoveries
- 2026-09-07 — Le guard dirty est réutilisé avant le POST logout ; navigation confirmée via state logoutConfirmed pour éviter une seconde confirmation après suppression des cookies. Le fichier pending-dataset-exposure-changes.guard.ts rejoint les fichiers modifiés.
- 2026-09-07 — En production les services existants utilisent des chemins relatifs à la racine puisque apiBaseUrl est vide. L'intercepteur couvre ces requêtes locales (exclut // et URLs tierces), et les nouveaux endpoints auth utilisent le fallback /api/v1.
- 2026-09-07 — Le git status depuis le frontend inclut également les modifications backend : vérifier la racine Git avant création de branche.
- 2026-09-07 — apiBaseUrl vide en production ; préserver le chemin explicite du refresh.

## Decision Log
- 2026-09-07 — Retenir 403 conformément à la demande. Un déclencheur 401 serait plus approprié pour l'entry point actuel, mais élargir les statuts changerait le périmètre demandé.
- 2026-09-07 — Préférer un refresh partagé aux refresh indépendants, incompatibles avec la rotation lors d'erreurs simultanées.
- 2026-09-07 — Extension explicite utilisateur : inclure le menu avatar et POST logout suivi de /login. Réutiliser PrimeNG plutôt qu'un dropdown personnalisé ; préserver les erreurs et les protections de modifications non enregistrées.

## Outcomes & Retrospective
Implémentation livrée sur `codex/auth-refresh-interceptor`, créée depuis `bot`. Aucun commit/push/merge. Modifications utilisateur préservées, notamment le formatage du layout et les changements chatbot/backend.

### Vérifications effectuées
Depuis `Frontend/Rhis_report_gen` :
- `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/auth/**/*.spec.ts" --include="src/app/shared/page-layout/*.spec.ts" --include="src/app/features/administration/dataset-exposure/pending-dataset-exposure-changes.guard.spec.ts"` : 31 réussis, exit 0. Le premier essai sandbox ne compilait pas à cause d'un refus d'accès ; l'exécution autorisée a permis de tester. Un premier run effectif donnait 28/31 avant adaptation du contexte d'injection des tests du guard et du clic du test menu.
- `npm.cmd run build` : deux builds réussis, exit 0, dernier bundle initial 622,16 kB. Avertissements : initial >500 kB, styles layout 5,08 kB, datasets 7,97 kB, rapports 4,59 kB (>4 kB).
- `npm.cmd test -- --watch=false --browsers=ChromeHeadless` : 175 réussis / 7 échecs, exit 1. Relance diagnostique avec `--reporters=dots --progress=false` : mêmes 7 échecs.
- `git diff --check -- Frontend/Rhis_report_gen/src/app` depuis la racine : exit 0. Un essai avec core.autocrlf=false a produit des faux positifs CRLF ; aucune réécriture des fins de ligne effectuée.
- `graphify update .` depuis la racine : exit 0. Avertissements concernant huit fichiers sans nœuds AST et labels de communautés à rafraîchir ; graphe généré mis à jour sans appel LLM.

### Échecs de la suite complète laissés hors périmètre
Ces tests ciblent des composants non modifiés et ne chargent pas l'intercepteur global. Aucun run du checkout antérieur n'a été exécuté : ne pas présenter la baseline comme verte ou ces échecs comme expérimentalement prouvés antérieurs.
- `dataset-exposure.component.spec.ts` : 4 échecs (titre sélectionné, textes de recherche vide, focus mobile, focus après sauvegarde).
- `configuration.component.spec.ts` : 1 échec (bouton attendu « Suivant », rendu « Génerer »).
- `preview-panel.component.spec.ts` : 2 échecs (compteurs « 0 ligne » et « 2 lignes » absents).

### Limites et reprise
Pas de test avec une session backend réelle ni de validation manuelle du clavier/mobile. Les tests ChromeHeadless couvrent le menu rendu, son lien, la redirection après succès, les erreurs, les doubles clics, la confirmation dirty, la rotation concurrente et l'absence de boucle. Karma signale également des ressources d'icônes introuvables et un arrêt lent de Chrome ; les résultats sont rapportés tels quels.

Le déclencheur reste uniquement 403 ; le backend utilise aussi 401 pour absence d'authentification. Le logout backend efface les cookies sans révoquer le refresh token en base, comportement existant inchangé. Les nouveaux endpoints utilisent /api/v1 quand apiBaseUrl est vide ; login/me et les autres URLs existantes sont préservés.

Prochaine action éventuelle : vérifier manuellement avec le backend démarré ; les sept échecs de tests existants constituent un travail distinct. Aucun travail d'implémentation refresh/logout restant.
