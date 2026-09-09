# Sécuriser et simplifier les réponses du chatbot

This ExecPlan is a living document governed by `.agent/PLANS.md`. Keep `Progress`, `Surprises & Discoveries`, `Decision Log` and `Outcomes & Retrospective` current throughout implementation.

Date: 2026-09-05  
Status: Completed  
Research: `docs/research/2026-09-05-chatbot-business-language.md`  
Related issue: N/A

## Purpose and observable outcome

Le chatbot conserve toutes les précisions d'une conversation et n'affiche plus de détails internes. La clé Mistral n'est plus présente dans la configuration versionnée.

## Scope and non-goals

In scope: variable d'environnement Mistral, accumulation des clarifications, contrôle des questions/résumés, erreur métier générique.  
Non-goals: historique persistant, nouvelle API, changement visuel, changement de modèle.

## Current behavior

Voir la recherche liée. Une seule clarification est conservée et les textes backend sont affichés directement.

## Proposed approach

Préserver le contrat existant. Lors d'une nouvelle clarification, enrichir localement la demande initiale avec la réponse précédente. Au backend, rejeter une question contenant des termes internes, utiliser un résumé fixe si le résumé est absent ou technique et toujours transformer un échec final en message métier fixe.

## Affected files and symbols

- `RHIS/src/main/resources/application.yaml` — `spring.ai.mistralai.api-key`.
- `BotReportPlanner.SYSTEM_PROMPT` — portée des règles de langage.
- `BotReportService.handlePlan/createGeneration` — textes exposés.
- `BotReportServiceTest` — garanties de langage.
- `ReportAssistantComponent.handleResponse` — historique et erreur générique.
- `report-assistant.component.spec.ts` — conversation multi-clarifications.

## Milestone 1: Sécuriser la configuration et les réponses backend

Result: aucune clé versionnée et aucune erreur interne affichable.  
Work: lire `MISTRAL_API_KEY`, renforcer le prompt, valider les questions et normaliser résumé/échec.  
Validation: `mvn "-Dtest=BotReportPlannerTest,BotReportServiceTest,BotReportControllerSecurityTest" test` doit réussir.

## Milestone 2: Conserver les précisions frontend

Result: chaque réponse antérieure reste incluse dans la demande suivante.  
Work: accumuler les paires question/réponse et afficher une erreur métier fixe.  
Validation: tests Angular ciblés ; si l'environnement reste bloqué, exécuter au minimum le build et documenter l'échec exact.

## Validation and acceptance

- Une question contenant `dataset` n'est jamais retournée telle quelle.
- Un résumé contenant du jargon devient `Votre rapport est prêt.`
- Un plan en échec retourne un conseil de reformulation sans détail interne.
- La troisième requête d'une conversation contient les deux précisions antérieures.
- La configuration ne contient plus la valeur de la clé Mistral.

## Risks and rollback

Le filtrage est volontairement limité aux termes internes connus. Le rollback consiste à restaurer les quatre fichiers applicatifs ; la clé révoquée ne doit jamais être restaurée.

## Progress

- [x] 2026-09-05 — Audit et recherche terminés.
- [x] 2026-09-05 — Milestone 1 terminé : clé externalisée, prompt renforcé, textes filtrés et erreurs génériques.
- [x] 2026-09-05 — Milestone 2 terminé : précisions antérieures accumulées et affichage frontend protégé.
- [x] 2026-09-05 — Vérification finale : 25 tests backend et 9 tests frontend réussis ; `git diff --check` sans erreur.

## Surprises & Discoveries

- Les tests frontend nécessitent une exécution hors sandbox pour lire les dépendances déjà installées ; ils réussissent dans cet environnement.

## Decision Log

- 2026-09-05 — Préserver le contrat HTTP et accumuler le contexte dans `message`, afin d'éviter une migration API et du stockage serveur.
- 2026-09-05 — Les erreurs finales sont volontairement génériques ; les détails restent réservés aux mécanismes internes de correction.

## Outcomes & Retrospective

La configuration versionnée dépend désormais de `MISTRAL_API_KEY`. Le chatbot conserve les précisions successives sans modifier le contrat HTTP. Les erreurs internes ne sont plus affichées, les résumés techniques sont remplacés et les questions techniques sont corrigées une fois puis transformées en échec métier sûr. Les tests ciblés frontend et backend réussissent. La clé précédemment publiée doit encore être révoquée chez Mistral par un opérateur disposant du compte.
