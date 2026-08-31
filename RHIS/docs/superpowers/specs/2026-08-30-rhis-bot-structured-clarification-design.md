# RHIS Bot — Clarification structurée sans répétition

**Date :** 30 août 2026  
**Statut :** design approuvé oralement, revue du document requise  
**Task slug :** `rhis-bot-structured-clarification`

## Problème

L'interface affiche une conversation mais le backend reste stateless. Après `NEEDS_CLARIFICATION`, Angular concatène demande, question et réponse dans un nouveau texte libre. Si le modèle redemande une précision, ce texte enrichi devient à son tour la nouvelle demande initiale : le contexte s'imbrique et le modèle peut répéter une question déjà répondue.

## Objectif

Une réponse claire, telle que « tous les contrats », complète explicitement la demande initiale. Le modèle reçoit les trois éléments séparément et ne peut pas reposer la même question. Aucun historique serveur ni stockage de conversation n'est ajouté.

## Contrat HTTP

`BotReportRequest` devient :

```java
public record BotReportRequest(
        @NotBlank String message,
        ReportExportFormat format,
        String clarificationQuestion,
        String clarificationAnswer
) {}
```

- `message` reste toujours la demande initiale, jamais un texte récursivement enrichi.
- `clarificationQuestion` et `clarificationAnswer` sont soit tous deux absents, soit tous deux non blancs.
- Le premier appel les omet. Un appel de clarification les renseigne.
- Le changement est compatible JSON pour les clients existants : les propriétés absentes deviennent `null`.

Le modèle TypeScript reflète ces deux propriétés optionnelles/nullables.

## Frontend

`ClarificationContext` conserve `originalMessage` et `question`. Lors de la réponse :

```ts
{
  message: context.originalMessage,
  format,
  clarificationQuestion: context.question,
  clarificationAnswer: answer,
}
```

L'historique visuel reste inchangé. Si le backend pose une nouvelle question différente, Angular conserve toujours le même `originalMessage` et remplace seulement `question`. Il ne concatène plus les tours dans `message`.

## Backend et prompt

`BotReportPlanner.plan()` reçoit le contexte structuré. Son user prompt distingue : demande initiale, question déjà posée et réponse utilisateur. Le system prompt impose que la réponse complète la demande, ne constitue pas une nouvelle demande, et qu'une question déjà répondue ne doit pas être répétée.

Après le retour LLM, `BotReportService` compare une nouvelle question à `clarificationQuestion` après normalisation minimale (`trim`, espaces consécutifs, casse). Si elles sont identiques, la proposition est rejetée comme validation LLM et passe dans l'unique correction existante. Le second appel reçoit l'erreur « La question a déjà reçu une réponse » avec le même contexte structuré. Si le modèle répète encore, la réponse HTTP devient `FAILED`; aucune génération n'est créée.

Une question réellement différente reste autorisée et retourne `NEEDS_CLARIFICATION`.

## Validation

- Les deux champs de clarification doivent être fournis ensemble et non blancs.
- Leur longueur totale contribue à la limite existante de 2000 caractères afin de ne pas augmenter sans borne le prompt.
- Aucun contenu de clarification n'est persisté.
- La validation backend des datasets, fields et relations reste inchangée.

## Tests

- Angular envoie la demande initiale inchangée avec question/réponse séparées.
- Une deuxième question conserve la demande initiale et ne crée aucun contexte imbriqué.
- Le planner rend les trois blocs explicitement dans le prompt.
- Le service accepte une clarification qui produit `READY`.
- Une question différente peut produire `NEEDS_CLARIFICATION`.
- La répétition de la même question déclenche une correction maximum puis `FAILED`.
- Aucun job n'est créé lors d'une répétition persistante.
- Les requêtes historiques sans champs de clarification restent valides.

## Hors périmètre

- historique conversationnel persistant ;
- nombre illimité de réponses précédentes ;
- fuzzy matching sémantique entre deux formulations différentes ;
- changement des statuts ou de `BotReportResponse`.

## Fichiers concernés

- Backend : `BotReportRequest`, `BotReportController`, `BotReportService`, `BotReportPlanner` et leurs tests.
- Frontend : model, component, service tests et component tests de `features/report-assistant`.
- Documentation : flow 07 et note de progression cross-repository.

## Contraintes de livraison

Aucune dépendance, migration, store, facade ou session serveur. Les changements utilisateur préexistants sont préservés. Aucun commit sans autorisation explicite.
