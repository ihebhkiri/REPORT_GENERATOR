# RHIS Bot Structured Clarification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transmettre les clarifications de façon structurée et empêcher RHIS Bot de répéter une question déjà répondue.

**Architecture:** Angular conserve la demande initiale et envoie séparément la question et sa réponse. Le backend valide cette paire, rend le contexte explicitement dans le prompt et utilise l'unique correction LLM existante lorsqu'une question identique est répétée.

**Tech Stack:** Angular, TypeScript, Java 17, Spring Boot 4.1, JUnit 5, Mockito, Jasmine, Maven.

## Global Constraints

- Aucun stockage serveur, historique persistant, dépendance, migration, store ou facade.
- Compatibilité JSON des anciens clients : propriétés de clarification absentes acceptées.
- `BotReportResponse`, statuts et limite de deux appels LLM inchangés.
- Aucun commit; préserver les changements utilisateur préexistants.

---

### Task 1: Contrat et prompt backend structurés

**Files:**
- Modify: `src/main/java/RHIS/com/RHIS/bot/controller/dto/BotReportRequest.java`
- Modify: `src/main/java/RHIS/com/RHIS/bot/BotReportPlanner.java`
- Test: `src/test/java/RHIS/com/RHIS/bot/BotReportPlannerTest.java`

**Interfaces:**
- Consumes: demande initiale, question précédente, réponse utilisateur.
- Produces: `BotReportRequest(message, format, clarificationQuestion, clarificationAnswer)` et `plan(catalogJson, request, previousErrors)`.

- [ ] Ajouter un test vérifiant que le user prompt contient trois blocs distincts et que l'ancienne requête sans clarification reste sérialisable.
- [ ] Exécuter `mvn '-Dtest=BotReportPlannerTest' test`; attendre l'échec de compilation du nouveau constructeur/signature.
- [ ] Ajouter les deux composants nullable au record et faire accepter au planner la requête complète.
- [ ] Rendre `Demande initiale`, `Question déjà posée`, `Réponse utilisateur` uniquement quand la paire existe; ordonner au modèle de ne pas répéter la question.
- [ ] Réexécuter le test; attendre tous les cas verts.

### Task 2: Validation et garde-fou backend

**Files:**
- Modify: `src/main/java/RHIS/com/RHIS/bot/BotReportService.java`
- Test: `src/test/java/RHIS/com/RHIS/bot/BotReportServiceTest.java`
- Test: `src/test/java/RHIS/com/RHIS/bot/BotReportControllerSecurityTest.java`

**Interfaces:**
- Consumes: `BotReportRequest` structuré et `BotReportPlan`.
- Produces: validation de paire, limite totale et rejet d'une question normalisée identique.

- [ ] Ajouter les tests : requête historique valide, paire incomplète rejetée, clarification vers `READY`, nouvelle question différente autorisée, même question répétée deux fois donnant `FAILED` et zéro génération.
- [ ] Exécuter `mvn '-Dtest=BotReportServiceTest,BotReportControllerSecurityTest' test`; attendre les nouveaux échecs.
- [ ] Valider la paire et sa longueur dans `validateMessage`.
- [ ] Faire passer la requête au planner lors des deux appels.
- [ ] Détecter une répétition par `trim`, espaces réduits et casse ignorée; la première répétition déclenche la correction existante, la seconde devient `FAILED`.
- [ ] Réexécuter les tests; attendre tous les cas verts et deux appels maximum.

### Task 3: Frontend non récursif

**Files:**
- Modify: `../Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.model.ts`
- Modify: `../Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.ts`
- Test: `../Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.spec.ts`
- Test: `../Frontend/Rhis_report_gen/src/app/features/report-assistant/bot-report.service.spec.ts`

**Interfaces:**
- Consumes: `BotReportRequest` avec clarification nullable.
- Produces: payload conservant toujours `originalMessage` sans concaténation.

- [ ] Modifier le test existant pour attendre `message: 'Liste des employés'`, `clarificationQuestion: 'Quel restaurant ?'`, `clarificationAnswer: 'Le restaurant central'`.
- [ ] Ajouter un troisième tour et vérifier que la demande initiale reste identique, sans chaîne `Demande initiale :` imbriquée.
- [ ] Exécuter `npm.cmd test -- --watch=false --include=src/app/features/report-assistant/report-assistant.component.spec.ts`; attendre l'échec des assertions.
- [ ] Étendre le modèle et remplacer `buildRequestMessage` par la construction directe du request DTO.
- [ ] Conserver `originalMessage` lors d'une nouvelle clarification différente.
- [ ] Réexécuter les tests component et service; attendre tous les tests verts.

### Task 4: Documentation et validation

**Files:**
- Modify: `docs/flows/07-bot-natural-language-report.md`
- Create: `docs/superpowers/progress/2026-08-30-rhis-bot-structured-clarification.md`

**Interfaces:**
- Consumes: comportement final backend/frontend.
- Produces: documentation et état de validation exacts.

- [ ] Documenter le contrat structuré et l'absence de mémoire serveur.
- [ ] Exécuter les tests backend Bot ciblés et les tests Angular assistant ciblés.
- [ ] Exécuter `mvn -DskipTests package` et `npm.cmd run build`.
- [ ] Exécuter `git diff --check` sur les fichiers de la task et relire le diff.
- [ ] Rapporter séparément toute défaillance préexistante de la suite complète.

## Completion Criteria

- « Tous les contrats » est envoyé comme réponse à la question précédente, pas comme nouvelle demande.
- La demande initiale ne devient jamais récursive.
- Une question identique ne peut pas être renvoyée deux fois à l'utilisateur.
- Une question différente reste possible.
- Aucun contrat de réponse, stockage ou dépendance supplémentaire.
