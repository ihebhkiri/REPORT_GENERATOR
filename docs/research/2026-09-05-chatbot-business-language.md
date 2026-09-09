# Research: fiabilité métier du chatbot

Date: 2026-09-05  
Status: Ready for planning  
Related issue: N/A

## Question

Comment empêcher le chatbot d'oublier des précisions et d'afficher du jargon interne, tout en retirant la clé Mistral du dépôt ?

## Scope

Included: configuration Mistral, contexte des clarifications, textes affichés après génération ou échec.  
Excluded: refonte de l'API, stockage serveur des conversations, changement de modèle ou de format d'export.

## Verified current behavior

- `RHIS/src/main/resources/application.yaml` contient une clé Mistral suivie par Git.
- `ReportAssistantComponent.handleResponse` remplace le contexte à chaque nouvelle question et perd les réponses antérieures.
- `BotReportService` renvoie directement le résumé et les erreurs produits par le modèle ou la validation.
- `ReportAssistantComponent` concatène et affiche directement `response.errors`.
- Le prompt interdit le jargon uniquement dans les questions de clarification.

## Data and control flow

Le frontend envoie la demande initiale et au plus une paire question/réponse. Le backend ajoute ces valeurs au contexte du modèle, valide le plan, crée la génération ou renvoie les erreurs. Le frontend affiche ensuite la question, le résumé ou les erreurs reçues.

## Invariants and constraints

- La validation backend du plan reste autoritative.
- Deux appels au modèle au maximum.
- Le contrat HTTP existant reste compatible.
- Aucun nouveau stockage ni dépendance.
- Aucun texte interne ne doit être montré à l'utilisateur final.

## Existing tests and validation commands

- `mvn "-Dtest=BotReportPlannerTest,BotReportServiceTest,BotReportControllerSecurityTest" test` — 23 tests réussis avant modification.
- Tests Angular ciblés — non exécutables avant modification : résolution des dépendances refusée dans l'environnement.

## Risks and unknowns

| Item | Type | Impact | How to resolve |
|---|---|---|---|
| Une clé déjà publiée reste exploitable après suppression Git | Risk | Accès non autorisé au compte Mistral | Révoquer la clé côté fournisseur |
| Le langage technique libre ne peut pas être classé parfaitement | Risk | Une question générée peut contourner une liste de termes | Refuser les termes internes connus et renforcer le prompt |

## Relevant files and symbols

- `RHIS/src/main/resources/application.yaml` — secret Mistral.
- `RHIS/src/main/java/RHIS/com/RHIS/bot/BotReportPlanner.java` — règles de formulation.
- `RHIS/src/main/java/RHIS/com/RHIS/bot/BotReportService.java` — frontière des réponses utilisateur.
- `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.ts` — contexte et affichage.
- Tests associés frontend et backend.

## Conclusions for planning

Le correctif minimal conserve le contrat actuel : le frontend incorpore les précisions passées dans la demande initiale, le backend contrôle les textes dynamiques et renvoie un échec métier fixe, et la configuration lit la clé depuis l'environnement.

## Open questions

Aucune question bloquante.
