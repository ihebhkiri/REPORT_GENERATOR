# Research: refresh frontend sur 403

Date: 2026-09-07
Status: Ready for planning
Related issue: N/A

## Question et scope
Identifier comment ajouter le refresh sur 403 au frontend, sans modifier le backend.

## Verified current behavior
- `Frontend/Rhis_report_gen/src/app/app.config.ts`: `provideHttpClient()` sans intercepteur.
- `Frontend/Rhis_report_gen/src/app/features/auth/services/auth.service.ts`: login et me avec cookies, aucune méthode refresh.
- `RHIS/src/main/java/RHIS/com/RHIS/auth/auth/AuthController.java`: POST `/api/v1/auth/refresh`, cookie refreshToken, réponse 204 et renouvellement des cookies.
- `RHIS/src/main/java/RHIS/com/RHIS/auth/auth/services/AuthServiceImpl.java`: vérifie expiration/validité, invalide l'ancien refresh token et génère les deux nouveaux tokens.
- `RHIS/src/main/java/RHIS/com/RHIS/auth/SecurityConfig.java`: refresh public, entry point non authentifié à 401 ; les endpoints admin exigent ROLE_ADMIN.
- Angular 20.3.26 et RxJS ~7.8.0 déclarés dans package.json.

## Data and control flow
Les services frontend appellent l'API avec withCredentials. Le refresh dépend d'un cookie HttpOnly et de sa validité en base ; le frontend ne doit pas lire les tokens.

## Invariants and constraints
Pas de boucle sur refresh/login/logout, pas de refresh pour des services tiers. La rotation impose de partager un refresh déjà en cours entre erreurs concurrentes.

## Existing tests and validation commands
AuthService possède des tests Jasmine avec HttpTestingController. Aucun test exécuté pendant cette recherche.

## Risks and unknowns
403 peut indiquer un refus de rôle ; 401 est configuré pour une absence d'authentification. Le déclencheur demandé reste 403. La configuration production apiBaseUrl est vide, contre http://localhost:8080/api/v1 en développement ; le chemin refresh demandé doit rester /api/v1/auth/refresh en production.

## Conclusions for planning et open questions
Un intercepteur fonctionnel et un refresh partagé dans AuthService suffisent. Aucun besoin de dépendance supplémentaire. Validation humaine du plan avant code de production selon AGENTS.md.

## Extension logout demandée le 2026-09-07
- `shared/page-layout/shared-page-layout.component.html` : avatar actuellement dans un span non interactif ; le fichier contient déjà du formatage utilisateur à préserver.
- `shared/page-layout/shared-page-layout.component.ts` : utilisateur chargé via AuthService.me(), aucun logout.
- `app.routes.ts` : connexion à /login ; la page administration possède un canDeactivate pour modifications non enregistrées.
- `AuthController.logout()` : POST /api/v1/auth/logout, 204 et suppression des cookies via maxAge=0. Pas de révocation du refresh token en base.
- PrimeNG 20 est déjà installé ; un menu popup évite une implémentation manuelle du dropdown. Les signatures installées seront vérifiées avant code.
