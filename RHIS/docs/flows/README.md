# Flows fonctionnels et techniques RHIS

Cette documentation suit le code courant de deux dépôts locaux :

- frontend Angular : `../Frontend/Rhis_report_gen` depuis la racine RHIS parente ;
- backend Spring Boot : le dépôt courant `RHIS`.

Les liens frontend partent de `docs/flows/` et remontent donc vers `../../../Frontend/Rhis_report_gen`. Les comportements décrits ont été établis à partir des appels réels et confrontés aux tests le 14 août 2026.

## Vue d'ensemble

| Ordre | Flow réel | Déclencheur | Endpoint(s) principal(aux) | Document |
| --- | --- | --- | --- | --- |
| 1 | Chargement des datasets, relations et fields | Ouverture de `/rapports`, puis clic sur « Suivant » | `GET /api/v1/datasets`, `/relations`, `/{id}/fields` | [01 — Sélection et chargement](01-dataset-selection-and-configuration-load.md) |
| 2 | Construction de la définition : columns, filters, sorts et brouillon local | Actions dans les éditeurs de configuration | Aucun appel lors de l'édition ; contrat commun envoyé ensuite | [02 — Définition du rapport](02-report-definition-fields-filters-sorts.md) |
| 3 | Preview synchrone | Clic sur « Aperçu » | `POST /api/v1/reports/preview` | [03 — Preview](03-report-preview.md) |
| 4 | Génération complète asynchrone | Clic sur « Générer » | `POST /api/v1/report-generations`, puis polling `GET /{id}` | [04 — Génération](04-report-generation.md) |
| 5 | Export PDF/XLSX et téléchargement | Clic sur « Exporter », puis « Télécharger » | `POST /report-generations/{id}/exports`, polling et `GET /report-exports/{id}/file` | [05 — Export et téléchargement](05-report-export-and-download.md) |
| 6 | Login par cookies ; refresh backend uniquement | Soumission du formulaire de connexion ; client externe pour le refresh | `POST /api/v1/auth/login`, `/refresh` | [06 — Authentification](06-authentication-and-refresh.md) |

## Frontière fonctionnelle observée

Flows demandés mais absents du code courant :

- aucune sauvegarde de report template en base ;
- aucun écran de liste ou de réouverture de templates ;
- aucun mapper métier dédié aux rapports ;
- aucun refresh token côté Angular, aucun HTTP interceptor et aucun route guard ;
- aucun appel Angular à `/auth/me` ou `/auth/logout` ;
- aucun broker ou worker externe : les jobs utilisent un `TaskExecutor` dans le même processus JVM.

Le `ReportDraftStorageService` sauvegarde bien une définition, mais uniquement dans le `sessionStorage` du navigateur. C'est un mécanisme de restauration locale et temporaire, pas un template partagé ou durable.

## Architecture transversale

```text
Angular
  RapportsComponent
    → ConfigurationComponent + éditeurs enfants
      → ReportPreviewService / ReportGenerationService
        → HTTP JSON + cookies
Spring Boot
  SecurityFilterChain + JwtCookieFilter
    → controllers REST
      → services d'orchestration
        → JPA pour métadonnées/états de jobs
        → JDBC pour les données de rapport
        → filesystem pour snapshots/exports
PostgreSQL + fichiers temporaires
    → réponses JSON ou fichier binaire
Angular
  Signals → templates PrimeNG → utilisateur
```

## Constats critiques confirmés

1. **Sécurité effective contradictoire.** Le [`SecurityConfig`](../../src/main/java/RHIS/com/RHIS/auth/SecurityConfig.java) courant déclare les quatre familles `/datasets/**`, `/reports/**`, `/report-generations/**` et `/report-exports/**` en `permitAll`. La preview et le catalogue sont donc anonymes. Les controllers de génération/export exigent pourtant un `@AuthenticationPrincipal` et le déréférencent : une requête anonyme peut atteindre le controller sans principal valide.
2. **Les tests expriment une autre intention.** `ReportControllerSecurityTest` attend `401` pour la preview et le polling anonymes. Sur le code courant, 51 tests ciblés passent et 2 échouent : preview anonyme observée à `200`, polling du slice observé à `404` au lieu de `401`.
3. **Le frontend n'assure pas le cycle de session.** Tous les services de reporting utilisent `withCredentials: true`, mais aucun refresh automatique n'existe. Le message `401` de la preview ne peut donc apparaître que si le backend recommence à protéger l'endpoint.
4. **Deux racines d'API frontend.** Les services de reporting importent `environment`, alors que `AuthService` importe directement `environment.development`. Une build de production peut donc conserver l'URL de développement pour le login selon la résolution TypeScript effective.
5. **Contrat frontend plus large que le backend.** Le type TypeScript `FilterOperator` contient `NOT_EQUALS`, `IS_NULL` et `IS_NOT_NULL`, absents de l'enum Java. L'éditeur les masque grâce à `VISIBLE_OPERATORS`, donc le flow UI courant ne les envoie pas.

## Vérification exécutée

- Frontend : `npm.cmd test -- --watch=false` — **84/84 tests réussis**.
- Backend ciblé : **53 tests**, dont **51 réussis et 2 échecs de sécurité** décrits ci-dessus.
- Les tests PostgreSQL Testcontainers ne sont actifs que lorsqu'un moteur Docker est disponible (`disabledWithoutDocker = true`).
