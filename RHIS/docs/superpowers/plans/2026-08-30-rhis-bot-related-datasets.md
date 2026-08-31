# RHIS Bot Related Datasets Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permettre à RHIS Bot de valider et générer des rapports utilisant plusieurs datasets reliés par des chemins directs, inverses ou indirects autorisés.

**Architecture:** Le catalogue LLM expose séparément racines, associés et relations sans métadonnées SQL. `BotReportService` valide le contrat spécifique du structured output, puis le resolver partagé construit un graphe non orienté et produit une chaîne de `ResolvedJoin` exécutable par le SQL builder existant.

**Tech Stack:** Java 17, Spring Boot 4.1, Spring Data JPA, PostgreSQL, JUnit 5, Mockito, Maven.

## Global Constraints

- Préserver `ReportPreviewRequest`, `BotReportResponse` et tous les contrats HTTP existants.
- Réutiliser `DataSetRepository.findVisibleTableRelations()` et le pipeline report existant.
- Ne jamais exposer au LLM les noms physiques des tables, colonnes ou contraintes.
- Ne jamais ajouter `DISTINCT` pour masquer une multiplication de lignes.
- Ne pas modifier Angular, ajouter de dépendance, migration, facade, store ou abstraction spéculative.
- Préserver les changements utilisateur préexistants et ne créer aucun commit.
- Deux appels LLM maximum restent autorisés.

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `src/main/java/RHIS/com/RHIS/dataset/repository/DataSetRepository.java` | Modify | Charger les datasets associés actifs exposés. |
| `src/main/java/RHIS/com/RHIS/bot/catalog/ReportCatalog.java` | Create | Conteneur compact du catalogue LLM. |
| `src/main/java/RHIS/com/RHIS/bot/catalog/CatalogRelation.java` | Create | Arête de graphe sans métadonnées SQL. |
| `src/main/java/RHIS/com/RHIS/bot/catalog/ReportCatalogProvider.java` | Modify | Construire racines, associés et relations. |
| `src/main/java/RHIS/com/RHIS/bot/dto/BotReportPlan.java` | Modify | Porter `relatedDatasetIds` et le statut `FAILED`. |
| `src/main/java/RHIS/com/RHIS/bot/BotReportPlanner.java` | Modify | Imposer les règles multi-datasets au LLM. |
| `src/main/java/RHIS/com/RHIS/bot/BotReportService.java` | Modify | Valider le plan contre le catalogue avant résolution. |
| `src/main/java/RHIS/com/RHIS/report/service/ReportQueryModel.java` | Modify | Représenter la source réelle de chaque jointure. |
| `src/main/java/RHIS/com/RHIS/report/service/ReportDefinitionResolver.java` | Modify | Résoudre les chemins directs, inverses et indirects. |
| `src/main/java/RHIS/com/RHIS/report/service/ReportSqlBuilder.java` | Modify | Joindre depuis l'alias source validé. |
| `src/test/java/RHIS/com/RHIS/bot/catalog/ReportCatalogProviderTest.java` | Modify | Vérifier le catalogue compact et les exclusions. |
| `src/test/java/RHIS/com/RHIS/bot/BotReportPlannerTest.java` | Modify | Vérifier le prompt et les statuts structurés. |
| `src/test/java/RHIS/com/RHIS/bot/BotReportServiceTest.java` | Modify | Vérifier toutes les validations backend du plan. |
| `src/test/java/RHIS/com/RHIS/report/service/ReportPreviewServiceTest.java` | Modify | Vérifier les chemins du resolver partagé. |
| `src/test/java/RHIS/com/RHIS/report/service/ReportSqlBuilderTest.java` | Modify | Vérifier le SQL direct, inverse et indirect. |
| `docs/flows/07-bot-natural-language-report.md` | Modify | Documenter le nouveau catalogue et les validations. |

### Task 1: Catalogue multi-datasets compact

**Interfaces:**
- Consumes: `findByActiveTrueAndDisplayMainTrue()`, `findVisibleTableRelations()`, `findVisibleFieldsByDatasetId(Long)`.
- Produces: `ReportCatalog buildCatalog()`, `findByActiveTrueAndDisplayRelatedTrue()`, `ReportCatalog`, `CatalogRelation`.

- [ ] Ajouter à `ReportCatalogProviderTest` un test qui construit une racine, un associé, un field supporté et plusieurs lignes d'une même relation composite; attendre une seule `CatalogRelation(1L, 2L)` et aucune donnée SQL dans le DTO.
- [ ] Ajouter un test d'exclusion des fields invisibles/non supportés en conservant le test principal seul existant.
- [ ] Exécuter `mvn '-Dtest=ReportCatalogProviderTest' test`; attendre un échec de compilation car `ReportCatalog` et la requête related n'existent pas.
- [ ] Ajouter `findByActiveTrueAndDisplayRelatedTrue()` avec `@EntityGraph(attributePaths = "dataSetFieldSet")`.
- [ ] Créer les deux records minimaux et faire retourner à `buildCatalog()` des listes immuables, les relations étant dédupliquées par couple d'IDs dans l'ordre du repository.
- [ ] Réexécuter `mvn '-Dtest=ReportCatalogProviderTest' test`; attendre tous les tests verts.
- [ ] Relire le diff de la task; vérifier l'absence de noms physiques dans les records sérialisés.

### Task 2: Structured output et validation autoritative du Bot

**Interfaces:**
- Consumes: `ReportCatalog`, `CatalogDataset`, `CatalogRelation`, `ReportDefinitionResolver.resolve(ReportPreviewRequest)`.
- Produces: `BotReportPlan.relatedDatasetIds()`, `BotReportPlan.isFailed()`, validation privée du plan avant génération.

- [ ] Adapter les constructeurs des tests existants et ajouter dans `BotReportPlannerTest` une assertion que le system prompt impose ordre, `relatedDatasetIds`, connectivité bidirectionnelle, IDs catalogués, aucun SQL, clarification et échec non relié.
- [ ] Ajouter dans `BotReportServiceTest` les cas: racine seule valide, direct valide, non relié, associé non exposé, field associé absent du catalogue, ID dataset/field inventé, demande ambiguë et modèle `READY` rejeté avant `generationService.create()`.
- [ ] Exécuter `mvn '-Dtest=BotReportPlannerTest,BotReportServiceTest' test`; attendre les échecs liés au nouveau contrat.
- [ ] Étendre `BotReportPlan` avec `relatedDatasetIds` et `isFailed()` sans changer `BotReportResponse`.
- [ ] Mettre à jour le prompt avec les règles exactes et le message métier demandé.
- [ ] Dans `BotReportService`, construire le catalogue une fois, sérialiser ce même objet, traiter `FAILED`, puis valider racine, associés, fields et connectivité par un BFS non orienté avant le resolver.
- [ ] Faire participer les erreurs de validation à l'auto-correction existante; après le second échec retourner `FAILED` et ne jamais créer de génération.
- [ ] Réexécuter `mvn '-Dtest=BotReportPlannerTest,BotReportServiceTest' test`; attendre tous les tests verts et au plus deux appels planner.
- [ ] Relire le diff; vérifier nullabilité, doublons, ordre métier et absence de confiance dans `READY`.

### Task 3: Chemins de relations exécutables dans le resolver partagé

**Interfaces:**
- Consumes: lignes `TableRelationProjection` de `findVisibleTableRelations()` et datasets référencés par les fields validés.
- Produces: `ResolvedJoin(sourceDataset, targetDataset, columns)` ordonné de la racine vers les cibles.

- [ ] Ajouter à `ReportPreviewServiceTest` les scénarios relation directe existante, relation inverse, chemin indirect via un intermédiaire et datasets non reliés.
- [ ] Ajouter un cas où deux contraintes entre les mêmes nœuds ou deux plus courts chemins rendent la résolution ambiguë.
- [ ] Exécuter `mvn '-Dtest=ReportPreviewServiceTest' test`; attendre l'échec des nouveaux scénarios inverse/indirect.
- [ ] Modifier `ResolvedJoin` pour porter `sourceDataset` et `targetDataset`.
- [ ] Dans `ReportDefinitionResolver`, grouper les projections par identité de contrainte et extrémités, préserver l'ordre des colonnes composites, construire une adjacency list bidirectionnelle et rechercher les chemins par BFS.
- [ ] Orienter les paires de colonnes selon le sens parcouru, réutiliser les segments déjà joints et refuser les ambiguïtés au lieu de choisir arbitrairement.
- [ ] Valider que chaque dataset référencé apparaît dans la racine ou les cibles jointes.
- [ ] Réexécuter `mvn '-Dtest=ReportPreviewServiceTest' test`; attendre tous les tests verts.
- [ ] Relire cardinalité et sens des colonnes; confirmer qu'aucun `DISTINCT` n'a été ajouté.

### Task 4: SQL multi-hop et sens inverse

**Interfaces:**
- Consumes: `ResolvedJoin.sourceDataset()`, `targetDataset()`, `columns()`.
- Produces: SQL utilisant l'alias réel de la source de chaque segment.

- [ ] Adapter les fixtures `ResolvedJoin` dans `ReportSqlBuilderTest` et ajouter un chemin `t0 -> t1 -> t2` ainsi qu'une relation inverse; vérifier chaque condition `ON` et l'absence de `DISTINCT`.
- [ ] Exécuter `mvn '-Dtest=ReportSqlBuilderTest' test`; attendre un échec avant adaptation du builder.
- [ ] Remplacer l'alias source fixe `t0` par `aliases.get(join.sourceDataset().getId())`; conserver les alias cibles déterministes et les `LEFT JOIN`.
- [ ] Réexécuter `mvn '-Dtest=ReportSqlBuilderTest' test`; attendre tous les tests verts.
- [ ] Relire le SQL généré pour les clés composites et les identifiants quotés.

### Task 5: Documentation et validation intégrée

**Interfaces:**
- Consumes: comportement final des tasks 1 à 4.
- Produces: flow exact et état de validation reproductible.

- [ ] Mettre à jour `docs/flows/07-bot-natural-language-report.md`: forme du catalogue, `relatedDatasetIds`, statuts, validation backend et chemins bidirectionnels/indirects.
- [ ] Exécuter `mvn '-Dtest=ReportCatalogProviderTest,BotReportPlannerTest,BotReportServiceTest,ReportPreviewServiceTest,ReportSqlBuilderTest' test`; attendre toute la sélection verte.
- [ ] Exécuter `mvn test`; rapporter exactement succès, échecs et tests Testcontainers skipped.
- [ ] Exécuter `mvn package`; attendre `BUILD SUCCESS` ou documenter précisément le blocage.
- [ ] Exécuter `git -c safe.directory='C:/Users/Surface Pro/Downloads/RHIS' diff --check` et relire le diff complet pour modifications involontaires.
- [ ] Mettre à jour `docs/superpowers/progress/2026-08-30-rhis-bot-related-datasets.md` avec commandes, résultats, fichiers sales, risques et prochaine action.

## Integrated Validation

- [ ] Les dix cas obligatoires sont couverts par des tests nommés et verts.
- [ ] Le catalogue JSON ne contient aucun nom physique SQL.
- [ ] Un plan `READY` falsifié ne peut pas atteindre `generationService.create()`.
- [ ] Les chemins direct, inverse et indirect produisent des jointures exécutables sans `DISTINCT`.
- [ ] Le contrat HTTP et Angular restent inchangés.
- [ ] Les changements utilisateur préexistants restent intacts.

## Rollback

Restaurer uniquement les fichiers listés dans la File Map. Aucun rollback de base ni migration n'est nécessaire. Les records du catalogue et `relatedDatasetIds` sont internes au Bot; le contrat HTTP public ne change pas.

## Completion Criteria

- Les demandes multi-datasets reliées deviennent `READY` et créent une génération.
- Les demandes ambiguës retournent `NEEDS_CLARIFICATION` sans génération.
- Les datasets non reliés, non exposés ou inventés et les fields indisponibles sont refusés par le backend même si le modèle retourne `READY`.
- Les validations Maven proportionnelles sont rapportées exactement et aucun commit n'est créé.
