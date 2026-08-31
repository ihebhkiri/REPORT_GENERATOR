# RHIS Bot — Rapports multi-datasets reliés

**Date :** 30 août 2026  
**Statut :** design approuvé en session  
**Task slug :** `rhis-bot-related-datasets`

## Objectif

RHIS Bot peut proposer et générer un rapport utilisant un dataset principal et plusieurs datasets associés lorsque chacun est accessible depuis la racine par des relations visibles autorisées. Les chemins directs, indirects et parcourus dans le sens inverse d'une clé étrangère sont supportés.

## Règles métier

- Le premier dataset explicitement mentionné devient `rootDatasetId`; l'ordre utilisateur n'est jamais réécrit.
- Les suivants deviennent `relatedDatasetIds`, dans leur ordre d'apparition et sans doublon.
- La racine doit être active et `displayMain=true`.
- Chaque dataset associé doit être actif et `displayRelated=true`.
- Tout field sélectionné, filtré ou trié doit exister, être actif, visible, de type supporté et appartenir à la racine ou à un dataset associé déclaré.
- Un chemin autorisé doit relier la racine à chaque associé. Pour la connectivité, chaque relation est parcourable dans les deux sens.
- Le modèle n'utilise que les IDs et relations du catalogue et ne produit aucun SQL.
- Le backend revalide toutes ces règles même si le modèle retourne `READY`.
- Une absence de chemin produit une erreur métier nommant les deux datasets.
- Aucune jointure n'utilise `DISTINCT` pour masquer une multiplication de lignes.

## Catalogue LLM

`ReportCatalogProvider.buildCatalog()` retourne :

```java
public record ReportCatalog(
        List<CatalogDataset> rootDatasets,
        List<CatalogDataset> relatedDatasets,
        List<CatalogRelation> relations
) {}

public record CatalogRelation(Long sourceDatasetId, Long targetDatasetId) {}
```

`rootDatasets` vient de `findByActiveTrueAndDisplayMainTrue()`. `relatedDatasets` vient d'une requête Spring Data minimale pour `active=true AND displayRelated=true`. Les deux listes contiennent uniquement les fields actifs, visibles et de type supporté. `relations` déduplique les couples issus de `findVisibleTableRelations()` sans exposer noms de tables, colonnes ou contraintes SQL.

## Plan LLM et prompt

`BotReportPlan` reçoit `List<Long> relatedDatasetIds` après `rootDatasetId`. Le structured output accepte `READY`, `NEEDS_CLARIFICATION` et `FAILED`.

Le prompt impose : détection dans l'ordre d'apparition, usage exclusif des IDs catalogués, vérification de connectivité non orientée, absence de SQL, conservation de l'ordre métier, clarification en cas d'ambiguïté, et `FAILED` avec un message compréhensible lorsque les datasets existent mais ne sont pas reliés.

Un statut `FAILED` du modèle est retourné sans tenter de génération. La validation backend reste autoritative et participe à l'unique passe d'auto-correction existante pour un plan `READY` invalide.

## Validation backend

`BotReportService` conserve l'objet `ReportCatalog` utilisé pour la sérialisation et valide chaque plan avant de construire le `ReportPreviewRequest` :

1. structure minimale du plan `READY`;
2. racine présente dans `rootDatasets`;
3. associés présents dans `relatedDatasets`;
4. IDs sans doublon et racine absente des associés;
5. tous les fields référencés appartiennent exactement aux datasets déclarés et sont présents dans leur catalogue;
6. parcours BFS non orienté du graphe compact entre la racine et chaque associé.

Cette validation ne remplace pas `ReportDefinitionResolver`; elle protège le contrat spécifique du plan LLM, puis le resolver recharge les métadonnées courantes avant génération.

## Résolution des jointures

`ReportDefinitionResolver` reste l'autorité qui transforme les fields demandés en jointures exécutables. Il groupe les lignes de `findVisibleTableRelations()` par contrainte, construit un graphe non orienté, puis recherche un plus court chemin depuis la racine vers chaque dataset référencé.

Chaque arête retenue devient un `ResolvedJoin` contenant le dataset déjà joint (`sourceDataset`), le nouveau dataset (`targetDataset`) et les colonnes orientées selon le sens du parcours. Les segments communs entre chemins sont réutilisés. Un dataset n'est joint qu'une fois. L'ordre est déterministe.

Si plusieurs relations physiques relient les mêmes deux nœuds sur un segment requis, ou si plusieurs plus courts chemins distincts sont possibles, le resolver refuse la définition comme ambiguë; il ne choisit pas silencieusement une sémantique métier.

`ReportSqlBuilder` utilise l'alias de `sourceDataset` au lieu de supposer `t0`. Il conserve les `LEFT JOIN`, le paramétrage et les protections d'identifiants existantes. Aucun `DISTINCT` n'est ajouté.

## Contrats préservés

- `ReportPreviewRequest` et tous les endpoints HTTP restent inchangés.
- `BotReportResponse` reste inchangé; Angular n'est pas modifié.
- Le mécanisme d'auto-correction reste limité à deux appels LLM maximum.
- `ReportGenerationService` et le pipeline asynchrone restent inchangés.
- Aucune dépendance, migration, facade, store ou nouvelle couche n'est ajoutée.

## Erreurs

- Dataset inconnu, non exposé, field inconnu/invisible ou ID inventé : plan rejeté par le backend avant génération.
- Dataset non relié : `Les datasets <Racine> et <Associé> ne sont pas reliés et ne peuvent pas être utilisés dans le même rapport.`
- Demande ambiguë : `NEEDS_CLARIFICATION` avec une seule question.
- Plan `READY` invalide : une correction LLM maximum, puis `FAILED` si la seconde proposition échoue.

## Tests d'acceptation

- dataset principal seul;
- relation directe;
- relation parcourue dans le sens inverse de la clé étrangère;
- chemin indirect par un dataset intermédiaire autorisé;
- datasets non reliés;
- associé non exposé;
- field associé invisible;
- ID inventé;
- demande ambiguë;
- validation backend d'un plan `READY` mensonger;
- SQL des chemins direct, inverse et indirect sans `DISTINCT`;
- non-régression des tests report et Bot existants.

## Hors périmètre

- sélection interactive du chemin lorsqu'il est ambigu;
- modification Angular;
- changement du schéma de données;
- relation inventée ou relation non retournée par `findVisibleTableRelations()`;
- stratégie automatique contre la multiplication métier légitime des lignes.

## Validation et livraison

Les tests focalisés sont exécutés avant `mvn test`, puis `mvn package`. Les tests PostgreSQL Testcontainers nécessitant Docker sont signalés comme skipped si Docker est indisponible. Le diff final est relu et aucun commit n'est créé.
