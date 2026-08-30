# rhis_bot — progression

**Updated:** 2026-08-30 Europe/Berlin
**Task slug:** `rhis-bot-natural-language-report`
**Current phase:** Validation / Review

## Intended Outcome

`POST /api/v1/bot/reports` transforme une phrase française en définition de rapport
validée, puis crée une génération asynchrone existante et retourne `202` avec un
`generationId`. Une phrase ambiguë produit une clarification et une validation rejetée
bénéficie d'une seule auto-correction LLM.

## Repository State

| Repository | Branch and checkout | Baseline | État courant avant commit documentaire |
| --- | --- | --- | --- |
| Monorepo RHIS | `rhis_bot`, `C:/Users/Surface Pro/Downloads/RHIS` | `394ad29` (`main`) | `43f3a94`; changements Angular/docs préexistants et suppressions `javac.*.args` préservés. |

La baseline Ponytail des fichiers préexistants est enregistrée dans
`target/ponytail-rhis-bot/baseline-dirty-files.json`. Les changements hors bot appartiennent
à d'autres travaux et ne doivent pas être intégrés aux commits de cette fonctionnalité.

## Completed

- Spring AI/Mistral et propriétés `rhis.bot` configurés sans secret dans le dépôt.
- Catalogue compact des datasets principaux actifs et champs visibles supportés.
- Structured output `BotReportPlan`, timeout et erreurs LLM typées.
- Orchestration complète avec `ReportDefinitionResolver`, pipeline de génération existant
  et une seule auto-correction.
- Endpoint authentifié `POST /api/v1/bot/reports` avec réponses `200/202/422` et
  `ProblemDetail` `400/502`.
- Profil de test configuré avec une clé factice afin que les contextes Spring n'exigent
  jamais de secret Mistral réel.
- Flow fonctionnel et technique documenté dans `docs/flows/07-bot-natural-language-report.md`.

Commits de la fonctionnalité avant documentation :

- `ea20947`, `d265989` — dépendances et propriétés ;
- `8f7147c` — catalogue ;
- `cf97a87` — planneur ;
- `157a837` — orchestration ;
- `6f8da3c` — endpoint, sécurité et configuration Mistral ;
- `43f3a94` — isolation de la clé dans le profil de test.

## Decisions

- Le LLM ne reçoit que la date, la phrase et les métadonnées du catalogue ; aucune ligne
  métier et aucun SQL.
- `ReportDefinitionResolver` reste la validation autoritative avant création du job.
- Maximum deux appels LLM : proposition initiale puis une correction après erreur backend.
- Le format effectif vient du body (`PDF`/`XLSX`) et vaut `XLSX` par défaut ; il n'est pas
  actuellement extrait de la phrase.
- La clé auparavant présente en clair dans le working tree a été retirée. Si elle était
  réelle, elle doit être révoquée côté Mistral.

## Validation State

| Command or check | Result | Meaning |
| --- | --- | --- |
| `mvn '-Dtest=BotReportControllerSecurityTest' test` | PASS, 4/4 | `401`, `202`, `422` et validation `400`. |
| `mvn '-Dtest=BotAiPropertiesTest,ReportCatalogProviderTest,BotReportPlannerTest,BotReportServiceTest,BotReportControllerSecurityTest' test` | PASS, 17/17 | Toute la fonctionnalité bot est verte, aucun skip. |
| `mvn '-Dtest=ReportPreviewPostgresIntegrationTest' test` | FAIL, 4 errors | Docker/PostgreSQL démarrent ; échec baseline du pool `ReportJobConfiguration`, plus d'échec Mistral. |
| `mvn test` | FAIL, 87 tests : 1 failure, 7 errors, 0 skip | Exactement la baseline : 4 anomalies XLSX (`xlsxRowWindow=0`) et 4 erreurs de contexte PostgreSQL (pool absent). Aucun nouvel échec bot. |
| `mvn package -DskipTests` | PASS | JAR Spring Boot construit ; les tests n'ont pas été rejoués par cette commande. |
| Recherche de clé Mistral en clair hors `target/` | PASS | Aucune valeur d'API key suivie ou non suivie dans le backend. |
| Empreintes Ponytail | 11/12 identiques | `docs/plans/2026-08-24-configuration-live-preview.md`, non suivi et hors périmètre bot, a changé depuis la baseline ; il a été préservé. Résultat dans `target/ponytail-rhis-bot/after-dirty-files.json`. |
| E2E login → bot → génération → export → download | SKIPPED | Nécessite une nouvelle clé Mistral sûre et les identifiants d'un utilisateur local existant. |

## Remaining Work

1. Exécuter l'E2E manuel avec `MISTRAL_API_KEY` fourni uniquement par l'environnement et
   un compte local de test, puis enregistrer les réponses `200/202/401` et le téléchargement.
2. Réaliser la phase 2 Angular (interface chat) dans un plan séparé après validation de
   l'API backend.
3. Traiter les anomalies XLSX et les propriétés de pool de test dans des tâches séparées.

## Blockers and Risks

- L'E2E réel n'est pas autorisable sans clé Mistral sûre et identifiants locaux ; aucun
  appel externe payant n'a été effectué pendant cette reprise.
- Une empreinte préexistante diffère pour le plan live-preview non suivi. Ce fichier est
  hors périmètre bot et n'a été ni restauré ni ajouté à un commit.
- Le backend exige une clé Mistral non vide au démarrage hors tests. Le déploiement doit
  fournir `MISTRAL_API_KEY`.
- `CompletableFuture` applique le timeout côté appelant, mais l'annulation du travail LLM
  sous-jacent n'est pas démontrée par les tests actuels.
- L'API ne déduit pas le format depuis la phrase, malgré la formulation indicative de la spec.

## Next Safe Action

Fournir une clé Mistral renouvelée via variable d'environnement et les identifiants d'un
compte local de test, puis exécuter exactement le scénario E2E de la Task 6 sans écrire les
secrets dans un fichier du dépôt.
