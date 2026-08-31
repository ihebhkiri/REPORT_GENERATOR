# Bot Report — Refactoring Ponytail chirurgical

**Date :** 31 août 2026  
**Statut :** option approuvée oralement, revue du document requise  
**Task slug :** `bot-report-ponytail-refactor`

## Objectif

Retirer uniquement trois complexités démontrées dans Bot Report sans modifier le comportement observable, les contrats HTTP/DTO, la sécurité, les validations ou le pipeline de génération.

## Modifications retenues

### Backend

Dans `BotReportService`, `createGeneration()` conserve l'appel autoritatif à `validatePlan(plan, catalog)`. Le guard identique dans `toPreviewRequest()` est supprimé, car cette méthode est privée, possède un seul appelant et n'est appelée qu'après `validatePlan()`.

La validation d'exposition des datasets et champs, la connectivité des relations, la conversion des filtres et tris, puis `ReportDefinitionResolver.resolve()` restent inchangées.

### Frontend

Dans `ReportAssistantComponent.handleHttpError()`, la branche `422 FAILED` traite directement les erreurs structurées : elle efface le contexte de clarification et ajoute le même message assistant. Elle ne construit plus une fausse réponse complète, une requête vide ni un cast forcé pour appeler `handleResponse()`.

Les autres erreurs HTTP, la restauration du brouillon, les états Signals, les messages visibles et l'accessibilité restent inchangés.

### Test backend

L'assertion de `BotReportPlannerTest.promptDefinesMultiDatasetRules()` est alignée sur l'apostrophe typographique réellement présente dans le prompt. Le prompt et son comportement ne changent pas.

## Architecture et flux préservés

```text
Angular submit
  → POST + Idempotency-Key
  → BotReportController (200/202/422 + Location)
  → BotReportService
  → authorized catalog
  → planner (maximum two attempts)
  → backend plan validation
  → ReportDefinitionResolver
  → ReportGenerationService
  → unchanged response
```

Une clarification ne déclenche toujours ni resolver, ni SQL, ni génération. Toute proposition READY reste validée côté backend avant utilisation.

## Tests et vérification

- Exécuter les deux specs Angular de `report-assistant`.
- Exécuter les cinq classes de tests backend directement associées au bot.
- Exécuter le build Angular.
- Exécuter la compilation ou le package backend pertinent.
- Exécuter `graphify update .` si disponible.
- Examiner le diff limité aux trois fichiers de code/test retenus et aux documents de workflow.

## Hors périmètre

- réécriture du retry en boucle ;
- modification des records du catalogue ou des DTO ;
- changement de `Location`, des statuts ou bodies HTTP ;
- modification du resolver, de la génération ou du SQL ;
- nouvelle dépendance, couche, interface, helper partagé ou composant ;
- nettoyage de formatage ou de fichiers non concernés.

## Risques

Le risque fonctionnel est faible : les deux chemins simplifiés sont privés et couverts par les tests existants. Le risque principal est de chevaucher les modifications utilisateur présentes ; les patches devront rester limités aux lignes auditées et le diff final devra être revu contre l'état actuel du working tree.

## Contraintes de livraison

Aucun commit sans autorisation explicite. Toutes les modifications utilisateur existantes doivent être conservées.
