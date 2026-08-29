# rhis_bot — Rapport généré à partir d'une phrase naturelle (Spring AI + Mistral)

**Date :** 29 août 2026
**Statut :** Design approuvé en session
**Branche de travail :** `rhis_bot` (monorepo unique — voir fait git ci-dessous)
**Phase 2 (hors périmètre du présent document) :** interface chat Angular

## Objectif et résultat observable

Un utilisateur authentifié envoie une phrase en français contenant tout le nécessaire, par exemple :

> « Liste des employés du restaurant central avec leur date d'embauche, triée par nom, en Excel »

Le backend interprète la phrase avec un LLM (Mistral AI via Spring AI), la transforme en définition
de rapport valide, puis **réutilise tel quel le pipeline existant** : génération asynchrone,
snapshot, export PDF/XLSX et téléchargement. L'utilisateur suit l'avancement avec les endpoints
de génération/export existants, exactement comme l'écran export actuel.

Critère observable : `POST /api/v1/bot/reports` retourne `202` avec un `generationId` exploitable
par `GET /api/v1/report-generations/{id}`, puis `POST /api/v1/report-generations/{id}/exports`
et `GET /api/v1/report-exports/{id}/file` sans aucune modification de ces endpoints.

## Périmètre et non-goals

**Dans le périmètre (phase 1, backend) :**

- nouveau module `RHIS.com.RHIS.bot` : 1 endpoint REST, orchestration, intégration Spring AI ;
- structured output Mistral → définition de rapport (IDs uniquement) ;
- réponse structurée `NEEDS_CLARIFICATION` si la phrase est ambiguë (pas de génération forcée) ;
- auto-correction : **une seule** nouvelle passe LLM si la validation backend échoue ;
- règle de sécurité explicite pour `/api/v1/bot/**` ;
- tests unitaires, tests de slice sécurité, validation E2E manuelle documentée.

**Hors périmètre :**

- l'envoi de données métier (lignes employés, pointages…) au LLM — seuls le catalogue
  (noms d'affichage des datasets/fields) et la phrase utilisateur sont envoyés ;
- la génération ou modification de SQL par le LLM — le `ReportSqlBuilder` existant reste
  le seul constructeur de SQL ;
- l'UI chat Angular (phase 2), les templates persistés, l'historique de conversation,
  le streaming, la refonte du `SecurityConfig` existant, les migrations de schéma.

## Comportement actuel vérifié (faits du dépôt au 29/08/2026)

- `ReportPreviewRequest(rootDatasetId: Long, selectedFieldIds: List<Long>,
  filters: List<ReportFilterRequest>, sorts: List<ReportSortRequest>)` est le contrat commun
  utilisé par la preview ET la génération (`RHIS/src/main/java/RHIS/com/RHIS/report/controller/dto/`).
- `ReportFilterRequest(fieldId: Long, operator: FilterOperator, values: List<String>)` ;
  valeurs transportées en strings, typées ensuite par le backend.
- `ReportSortRequest(fieldId: Long, direction: SortDirection)`.
- `FilterOperator` (Java) : `EQUALS, CONTAINS, GREATER_THAN, GREATER_THAN_OR_EQUAL,
  LESS_THAN, LESS_THAN_OR_EQUAL, BETWEEN`. (Le type TS frontend annonce 3 opérateurs de
  plus, masqués par l'UI ; un payload contenant ces opérateurs échoue en 400.)
- `ReportExportFormat` : `PDF`, `XLSX`.
- `ReportDefinitionResolver.resolve()` valide toute la définition contre le catalogue
  (datasets actifs, fields visibles, types `DataSetFieldType`, arité des opérateurs,
  joins directs, tris limités aux champs sélectionnés) et lève
  `ReportValidationException` / `ReportDefinitionUnavailableException` (409) avant
  toute construction SQL.
- `ReportGenerationService.create(owner, idempotencyKey, request)` gère idempotence
  scopée owner+clé, capacité par utilisateur, insertion `PENDING`, puis dispatch async ;
  retour `202` + `Location`. Le worker revalide la définition et produit un snapshot
  NDJSON gz indépendant du format d'export.
- `SecurityConfig` actuel : `/api/v1/datasets/**`, `/api/v1/reports/**`,
  `/api/v1/report-generations/**`, `/api/v1/report-exports/**` sont `authenticated()` ;
  remarque : `RHIS/docs/flows/README.md` (14/08/2026) indique `permitAll` — information
  périmée par rapport au code courant. `anyRequest()` final = `permitAll()` → le nouvel
  endpoint `/api/v1/bot/**` doit donc avoir une règle explicite.
- Modèle du catalogue : `DataSetEntity(id, displayName, sourceName, active, displayMain,
  displayRelated)`, `DataSetField(id, displayName, sourceName, active, visible, position,
  dataType, nullable, primaryKey)`.
- Stack : Java 17, Spring Boot 4.1.0, Maven. Spring AI 2.0.x supporte officiellement
  Spring Boot 4.0.x/4.1.x (docs officielles consultées le 29/08/2026) ; BOM
  `org.springframework.ai:spring-ai-bom` + starter `spring-ai-starter-model-mistralai`.
- Git (vérifié le 29/08/2026) : **un seul dépôt**, racine `Downloads/RHIS` —
  `git rev-parse --show-toplevel` retourne la même racine depuis la racine, `RHIS/` et
  `Frontend/Rhis_report_gen/`. Les `.git` imbriqués ne sont pas des dépôts actifs.
  La branche `rhis_bot` est unique et couvre backend et frontend.
- Travail non commité préexistant dans le worktree (rename preview-dialog→preview-panel,
  modifications configuration.component.*, flows 02/03, plans du 24/08) : à préserver,
  hors périmètre de la fonctionnalité.

## Approche retenue

**A — Traducteur structuré one-shot**, avec deux garde-fous :

1. `NEEDS_CLARIFICATION` : si la phrase est ambiguë ou impossible à mapper, le planneur
   retourne un statut structuré avec une question, sans créer de génération.
2. Auto-correction unique : si `ReportDefinitionResolver` rejette le plan (opérateur,
   format de valeur, champ indisponible…), les erreurs structurées + le catalogue sont
   renvoyés au LLM pour une seule nouvelle passe, puis revalidés.

Alternatives rejetées :

- **B — Agent avec tool calling** : plus résilient pour de très grands catalogues, mais
  3 à 6 allers-retours LLM, flux non déterministe, tests plus complexes ; surdimensionné
  pour le catalogue RH actuel.
- **C — Conversationnel multi-tours** : meilleure UX pour les phrases vagues, mais exige
  une mémoire de conversation serveur ; reporté à la phase 2 si besoin.

## Contrat API (nouveau, unique endpoint de la phase 1)

`POST /api/v1/bot/reports` — authentification requise (cookie JWT existant) ;
en-tête `Idempotency-Key` optionnel (UUID généré serveur si absent).

Requête :

```json
{ "message": "Liste des employés du restaurant central en Excel", "format": "XLSX" }
```

- `message` : obligatoire, 1..2000 caractères (`rhis.bot.max-message-length`).
- `format` : optionnel, `PDF` | `XLSX`, défaut `XLSX`. Indicatif : déduit de la phrase si
  possible, renvoyé au client pour qu'il appelle l'export existant. Le snapshot étant
  indépendant du format, cette valeur ne bloque jamais la génération.

Réponses :

| Cas | HTTP | Body |
| --- | --- | --- |
| Plan validé, génération créée | `202` + `Location` | `{ "status": "READY", "generationId": 12, "format": "XLSX", "planSummary": "Rapport des employés du restaurant central (5 colonnes, 1 filtre)." }` |
| Clarification nécessaire | `200` | `{ "status": "NEEDS_CLARIFICATION", "question": "Souhaitez-vous filtrer sur un restaurant en particulier ?" }` |
| Échec après auto-correction | `422` | `{ "status": "FAILED", "errors": ["Le champ X utilise un type non supporté."] }` |
| Requête invalide / non authentifié | `400` / `401` | convention existante |

## Architecture et modules

```text
RHIS.com.RHIS.bot
├── BotReportController        thin ; @AuthenticationPrincipal obligatoire ;
│                              valide le body, délègue au service
├── BotReportService           orchestration : catalogue → plan → resolve
│                              → (auto-correction) → ReportGenerationService.create()
├── BotReportPlanner           Spring AI ChatClient (starter mistralai) +
│                              structured output → BotReportPlan
├── ReportCatalogProvider      datasets actifs + fields visibles → DTO compact
│                              pour le prompt (lit via repos dataset existants)
├── dto/BotReportRequest       { message, format? }
├── dto/BotReportResponse      { status, question?, generationId?, format?, planSummary?, errors? }
├── dto/BotReportPlan          cible du structured output (interne) :
│                              { status, question?, summary?, rootDatasetId?,
│                                selectedFieldIds[], filters[{fieldId, operator, values[]}],
│                                sorts[{fieldId, direction}] }
└── config/BotAiProperties     model, temperature, llm-timeout, max-message-length
```

Flux détaillé :

```text
POST /api/v1/bot/reports
  → ReportCatalogProvider.build()            (lecture seule)
  → BotReportPlanner.plan(catalog, message)  (1er appel Mistral, température 0)
      ├─ NEEDS_CLARIFICATION → 200
      └─ READY → map vers ReportPreviewRequest
            → ReportDefinitionResolver.resolve()          [EXISTANT]
                ├─ OK → ReportGenerationService.create(owner, key, request) [EXISTANT] → 202
                └─ ReportValidationException | ReportDefinitionUnavailableException
                     → BotReportPlanner.plan(catalog, message, erreurs)  (2e et DERNIER appel)
                          ├─ READY + resolve OK → create() → 202
                          └─ échec → 422 FAILED
```

Le LLM ne voit jamais de données métier et ne produit que des IDs/opérateurs/valeurs texte ;
toute exécution reste dans le pipeline validé existant.

## Design du prompt (system, en français)

- Rôle : « Tu es l'assistant RHIS. À partir du catalogue JSON et de la demande utilisateur,
  produis uniquement un objet JSON conforme au schéma fourni. »
- Menu : catalogue JSON compact
  `[{ "datasetId", "displayName", "fields": [{ "fieldId", "displayName", "type" }] }]`.
- Règles strictes :
  - n'utiliser que les `datasetId`/`fieldId` présents dans le catalogue — ne jamais inventer ;
  - opérateurs limités à l'enum Java exact : `EQUALS, CONTAINS, GREATER_THAN,
    GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, BETWEEN` ;
  - formats de valeurs par type (`DATE` = ISO-8601 `yyyy-MM-dd`, `INTEGER` = entier,
    `DECIMAL` = nombre, `BOOLEAN` = true/false, `DATE_TIME` = ISO-8601…) ;
  - `selectedFieldIds` non vide ; tris limités aux champs sélectionnés ;
  - `BETWEEN` attend exactement 2 valeurs, les autres opérateurs 1 ;
  - si la demande est ambiguë, incomplète ou impossible avec le catalogue :
    `status = NEEDS_CLARIFICATION` et **une seule** question courte ;
  - sinon `status = READY` + un `summary` d'une phrase décrivant le rapport compris.
- La date du jour est injectée pour résoudre les expressions relatives (« ce mois-ci »).

## Sécurité et confidentialité

- `SecurityConfig` : ajout d'une règle explicite `.requestMatchers("/api/v1/bot/**").authenticated()`
  placée avant le `anyRequest().permitAll()` final. Aucune autre règle existante n'est modifiée.
- La clé API Mistral est fournie par variable d'environnement `MISTRAL_API_KEY`
  (`${MISTRAL_API_KEY}` dans `application.yaml`) ; aucune clé dans le dépôt.
- Contenu envoyé au LLM : noms d'affichage du catalogue + phrase utilisateur. Aucune ligne
  de données métier, aucun résultat de rapport, aucune donnée personnelle.
- Le endpoint reste soumis aux mêmes limites par utilisateur via
  `ReportGenerationService.create()` (capacité, idempotence) — aucun contournement.

## Configuration (application.yaml)

```yaml
spring:
  ai:
    mistralai:
      api-key: ${MISTRAL_API_KEY}
      chat:
        options:
          model: mistral-small-latest   # à confirmer au milestone 0
          temperature: 0.0

rhis:
  bot:
    max-message-length: 2000
    llm-timeout-seconds: 30
```

`pom.xml` : import `org.springframework.ai:spring-ai-bom:2.0.x` dans
`dependencyManagement` + dépendance `spring-ai-starter-model-mistralai`.

## Tests et validation

- `BotReportPlannerTest` : ChatClient mocké → READY bien mappé, NEEDS_CLARIFICATION,
  JSON invalide/exception → erreur explicite.
- `ReportCatalogProviderTest` : ne retient que datasets actifs + fields visibles.
- `BotReportServiceTest` : resolve OK → create appelé ; resolve échoue → 2ᵉ passe LLM
  (auto-correction) → succès ou 422 ; jamais plus de 2 appels LLM.
- `BotReportControllerSecurityTest` (slice, aligné sur l'intention des tests existants) :
  anonyme → 401 ; authentifié → 202.
- Validation manuelle E2E documentée : login (cookie) → `POST /api/v1/bot/reports`
  → polling génération → export → download (commandes curl fournies dans le plan).
- `mvn test` complet ; couverture Testcontainers signalée explicitement si Docker absent.

## Risques et rollback

| Risque | Mitigation |
| --- | --- |
| Modèle Mistral exact / fiabilité du JSON strict | Vérifié dès le milestone 0 (compile + 1er appel réel) ; température 0 ; structured output Spring AI |
| Mapping LLM imparfait (mauvais opérateur/type) | Validation stricte existante + 1 auto-correction + erreur claire en 422 |
| Phrase hors domaine | NEEDS_CLARIFICATION au lieu d'une génération forcée |
| Clé API exposée | Variable d'environnement uniquement |
| Latence / coût | 1 appel LLM (2 au maximum), catalogue compact, température 0 |
| Régression du pipeline existant | Aucune modification des classes report/dataset existantes ; SecurityConfig : 1 règle additive |

**Rollback** : la fonctionnalité est purement additive (nouveau package `bot`, 2 blocs de
config, 1 règle de sécurité). Aucun changement de schéma de base, aucune modification des
contrats existants → suppression du package + des blocs de config suffit à revenir à l'état
antérieur. La branche `rhis_bot` permet de ne pas toucher `main`.

## Fichiers attendus (chemins relatifs au backend `RHIS/`)

**Nouveaux :** `src/main/java/RHIS/com/RHIS/bot/**` (7 classes + DTOs),
`src/test/java/RHIS/com/RHIS/bot/**`, `docs/flows/07-bot-natural-language-report.md`.

**Modifiés :** `pom.xml` (BOM + starter), `src/main/resources/application.yaml`
(blocs `spring.ai` et `rhis.bot`), `src/main/java/RHIS/com/RHIS/auth/SecurityConfig.java`
(1 règle).

## Journal des décisions

- 2026-08-29 — Fournisseur LLM : **Mistral AI** (choix utilisateur) ; alternatives OpenAI /
  Ollama / Azure écartées ; la configuration Spring AI garde la possibilité de changer de
  starter plus tard.
- 2026-08-29 — Approche **A (traducteur structuré) + auto-correction unique** (choix
  utilisateur) ; tool calling et multi-tours écartés (complexité non justifiée aujourd'hui).
- 2026-08-29 — **API d'abord, UI chat en phase 2** (choix utilisateur).
- 2026-08-29 — Branche **`rhis_bot`** (choix utilisateur). Fait vérifié ensuite : le dépôt
  est unique (monorepo) → la branche couvre backend **et** frontend, ce qui simplifie la
  phase 2 ; aucun `.git` imbriqué actif à gérer.
- 2026-08-29 — Le format d'export est indicatif (porté par la réponse) : le snapshot
  existant étant indépendant du format, le bot ne duplique aucune logique d'export.


