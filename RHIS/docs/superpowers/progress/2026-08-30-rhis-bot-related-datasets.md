# RHIS Bot Related Datasets — Progress

**Date:** 2026-08-30  
**Task slug:** `rhis-bot-related-datasets`  
**Branch:** `rhis_bot`  
**Status:** implémentation terminée, validation ciblée verte, suite complète bloquée par des échecs hors périmètre

## Completed

- Catalogue LLM séparé en `rootDatasets`, `relatedDatasets` et `relations` sans noms SQL physiques.
- `BotReportPlan` étendu avec `relatedDatasetIds` et erreurs `FAILED`.
- Prompt multi-datasets avec ordre métier, connectivité bidirectionnelle, clarification et absence de SQL.
- Validation backend autoritative des rôles, IDs, fields déclarés et chemins avant génération.
- Resolver partagé capable de chemins directs, inverses et indirects avec refus des chemins ambigus.
- SQL builder utilisant l'alias source réel de chaque segment, sans `DISTINCT`.
- Flow 07, recherche, spec et plan mis à jour/créés.
- Angular non modifié par cette task; aucun commit, push, merge ou rebase.

## Validation

| Command | Result |
|---|---|
| `mvn -DskipTests compile` | PASS — `BUILD SUCCESS`. |
| `mvn '-Dtest=ReportCatalogProviderTest,BotReportPlannerTest,BotReportServiceTest,ReportPreviewServiceTest,ReportSqlBuilderTest' test` | PASS — 36 tests, 0 failure, 0 error, 0 skipped. |
| `mvn test` | FAIL — 94 tests exécutés, 1 failure et 7 errors hors périmètre. |
| `mvn -DskipTests package` | PASS — `target/RHIS-0.0.1-SNAPSHOT.jar` produit. |
| `git diff --check -- <fichiers de la task>` | PASS — aucune erreur; avertissements CRLF seulement. |

## Full-suite Failures

- `ReportExportWriterTest`: 4 cas échouent car `SXSSFWorkbook` reçoit `rowAccessWindowSize=0`; Apache POI exige une valeur `> 0` ou `-1`. La configuration `application.yaml` est préexistante et hors périmètre.
- `ReportPreviewPostgresIntegrationTest`: 4 cas ne chargent pas le contexte car `reportJobExecutor` échoue à l'instanciation. Docker et PostgreSQL 16 sont disponibles; le conteneur a démarré. Le blocage est une configuration d'executor préexistante, pas Testcontainers.
- Aucun de ces tests ou composants n'est modifié par cette task.

## Dirty Files Owned by This Task

- `docs/flows/07-bot-natural-language-report.md`
- `docs/superpowers/{research,specs,plans,progress}/2026-08-30-rhis-bot-related-datasets*`
- `src/main/java/RHIS/com/RHIS/bot/{BotReportPlanner,BotReportService}.java`
- `src/main/java/RHIS/com/RHIS/bot/catalog/{CatalogRelation,ReportCatalog,ReportCatalogProvider}.java`
- `src/main/java/RHIS/com/RHIS/bot/dto/BotReportPlan.java`
- `src/main/java/RHIS/com/RHIS/dataset/repository/DataSetRepository.java`
- `src/main/java/RHIS/com/RHIS/report/service/{ReportDefinitionResolver,ReportQueryModel,ReportSqlBuilder}.java`
- tests correspondants sous `src/test/java/RHIS/com/RHIS/bot` et `src/test/java/RHIS/com/RHIS/report/service`.

## Preserved Unrelated Changes

- Tous les changements Angular préexistants.
- Déplacement préexistant de `BotReportController` et son test.
- `src/main/resources/application.yaml`.
- Flows 02/03, autres plans/specs/progress et suppressions `javac.*` préexistants.

## Next Action

Corriger séparément la configuration XLSX et celle du report job executor, puis relancer `mvn test`. Aucune intégration ne doit être proposée avant une suite complète verte ou une décision humaine explicite d'accepter cette baseline.
