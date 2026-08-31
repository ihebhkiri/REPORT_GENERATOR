# RHIS Bot Multi-Dataset Reports Research

**Date:** 2026-08-30
**Task slug:** `rhis-bot-related-datasets`
**Repositories and branches:** monorepo RHIS, branche courante non modifiée

## Question

Comment permettre au RHIS Bot de proposer puis générer un rapport utilisant plusieurs datasets reliés, y compris par un chemin indirect ou parcouru dans le sens inverse d'une clé étrangère, sans dupliquer le moteur de rapport ?

## Scope

- In: catalogue envoyé au LLM, structured output, validation backend, résolution des chemins, construction des jointures et tests backend.
- Out: contrat HTTP public, Angular, SQL produit par le LLM, nouvelle dépendance, migration de schéma.

## Facts

- `ReportCatalogProvider.buildCatalog()` retourne actuellement uniquement les datasets `active=true` et `displayMain=true`.
- `BotReportPlan` ne représente pas les datasets associés.
- `BotReportService` sérialise le catalogue, convertit le plan en `ReportPreviewRequest`, puis appelle `ReportDefinitionResolver.resolve()` avant `ReportGenerationService.create()`.
- `ReportDefinitionResolver.resolveJoin()` accepte uniquement une relation directe `root -> target`.
- `ReportSqlBuilder.appendJoins()` joint chaque cible depuis `t0`; il ne peut donc pas exprimer un chemin indirect ni une relation parcourue en sens inverse.
- `DataSetRepository.findVisibleTableRelations()` fournit les identifiants, noms d'affichage et colonnes nécessaires aux jointures autorisées. Sa requête exige des datasets actifs et une cible `displayRelated=true`.
- Le worktree contient des changements utilisateur préexistants, notamment dans le frontend et autour du déplacement de `BotReportController`; ils sont hors périmètre.

## Relevant Execution Path

1. `BotReportService.generate()` obtient et sérialise le catalogue.
2. `BotReportPlanner.plan()` produit un `BotReportPlan` structuré.
3. `BotReportService` valide le plan et construit un `ReportPreviewRequest`.
4. `ReportDefinitionResolver.resolve()` valide les champs et construit les jointures autorisées.
5. `ReportSqlBuilder` produit le SQL paramétré; `ReportGenerationService` crée le job.

## Existing Contracts

- API: `POST /api/v1/bot/reports` et `BotReportResponse` restent inchangés.
- Data: aucune migration; les permissions viennent de `active`, `displayMain`, `displayRelated`, de la visibilité des fields et de `findVisibleTableRelations()`.
- Frontend: aucun changement requis car `relatedDatasetIds` reste interne au structured output LLM.

## Validation Evidence

| Check | Command or inspection | Result |
|---|---|---|
| État du worktree | `git -c safe.directory=... status --short` | Changements utilisateur préexistants identifiés; aucun fichier de cette feature encore modifié. |
| Resolver actuel | inspection de `ReportDefinitionResolver.resolveJoins()` | Relations directes sortantes uniquement. |
| SQL actuel | inspection de `ReportSqlBuilder.appendJoins()` | Chaque jointure part de `t0`. |
| Catalogue actuel | inspection de `ReportCatalogProvider.buildCatalog()` | Principaux uniquement, sans relations. |

## Risks and Unknowns

- Risk: une validation uniquement dans le Bot accepterait un plan que le moteur SQL ne sait pas matérialiser.
- Risk: plusieurs chemins autorisés de même longueur peuvent rendre la jointure ambiguë; le resolver doit refuser l'ambiguïté plutôt que choisir arbitrairement.
- Risk: les relations composites sont retournées ligne par ligne; elles doivent rester groupées par contrainte et ordonnées par position.
- Unknown: aucun point métier bloquant après validation utilisateur du design le 2026-08-30.

## Options

1. **Étendre le resolver partagé** — un seul mécanisme autoritatif pour preview, Bot et génération; changement nécessaire du modèle de jointure interne.
2. **Ajouter un validator propre au Bot** — diff local plus court mais génération indirecte impossible et duplication des règles.
3. **Étendre `ReportPreviewRequest`** — rend les datasets associés publics mais modifie inutilement le contrat HTTP.

## Recommendation

Étendre le resolver partagé avec une recherche de chemin non orientée et faire porter à chaque `ResolvedJoin` son dataset source, son dataset cible et ses colonnes orientées selon le parcours. C'est le plus petit changement qui rende réellement les plans multi-datasets exécutables tout en conservant `findVisibleTableRelations()` et le pipeline existant.

## Human Review Gate

Design approuvé par l'utilisateur le 2026-08-30. Aucun commit n'est autorisé.
