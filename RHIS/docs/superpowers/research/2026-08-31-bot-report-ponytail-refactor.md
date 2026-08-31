# Bot Report Ponytail Refactor Research

**Date:** 2026-08-31
**Task slug:** `bot-report-ponytail-refactor`
**Repositories and branches:** backend and frontend working tree on `rhis_bot`

## Question

Which Bot Report code can be removed or simplified without changing its HTTP contract, authorization, validation, report-generation flow, or Angular behavior?

## Scope

- In: Angular `features/report-assistant`, backend `RHIS.com.RHIS.bot`, and their direct tests.
- Out: report-generation services, SQL construction, persistence, endpoints, DTO shapes, UI design, dependencies, and unrelated dirty files.

## Facts

- The working tree already contains extensive user changes, including every principal Bot Report backend file and the untracked Angular feature. They must remain intact.
- `ReportAssistantComponent.submit()` builds the natural-language request; `BotReportService.createReport()` adds a fresh `Idempotency-Key` and credentials.
- `BotReportController.createReport()` maps `READY` to `202` with `Location`, `FAILED` to `422`, and clarification to `200`.
- `BotReportService.generate()` builds the authorized catalog, calls the planner at most twice, validates a `READY` plan, invokes `ReportDefinitionResolver.resolve()`, then delegates to `ReportGenerationService.create()`.
- `BotReportService.createGeneration()` calls `validatePlan()` immediately before `toPreviewRequest()`. The latter repeats the same READY/root/selected-fields guard and has no other caller.
- `ReportAssistantComponent.handleHttpError()` handles a structured `422 FAILED` by fabricating a complete response and dummy request solely to reuse `handleResponse()`.
- The catalog records are used to serialize the planner input and validate plan exposure; they are justified domain structures rather than speculative layers.

## Relevant Execution Path

1. Angular component → Angular `BotReportService.createReport()` → `POST /api/v1/bot/reports`.
2. `BotReportController.createReport()` → `BotReportService.generate()` → `ReportCatalogProvider.buildCatalog()` → `BotReportPlanner.plan()`.
3. Clarification returns without resolver or generation; READY passes catalog validation and `ReportDefinitionResolver.resolve()` before `ReportGenerationService.create()`.
4. The controller returns the existing body, status, and `Location` header.

## Existing Contracts

- API: natural-language message, optional structured clarification, and optional request header; public statuses are `200`, `202`, and `422`; `202` includes `Location`.
- Data/security: catalog exposure and resolver validation remain backend-authoritative; SQL values remain parameterized in the existing report pipeline.
- Frontend: loading, clarification, READY, FAILED, session-expired, keyboard, and accessibility behavior remain unchanged.

## Validation Evidence

| Check | Command or inspection | Result |
|---|---|---|
| Callers and full flow | Graphify queries plus `rg` across frontend/backend | Verified; no additional callers of the private conversion/validation methods |
| Angular baseline | `npm.cmd test -- --watch=false --include="src/app/features/report-assistant/*.spec.ts"` | 8 tests passed |
| Backend baseline | `mvn "-Dtest=BotReportServiceTest,BotReportPlannerTest,BotReportControllerSecurityTest,ReportCatalogProviderTest,BotAiPropertiesTest" test` | 27 passed; one brittle prompt-text assertion failed because it expects an ASCII apostrophe while the prompt uses a typographic apostrophe |

## Risks and Unknowns

- Risk: changing retry orchestration could alter the two-attempt contract; leave it unchanged.
- Risk: moving validation out of the backend would weaken the trust boundary; remove only the duplicate private guard after the authoritative guard.
- Unknowns: none that change the approved surgical refactor.

## Options

1. **Surgical three-file refactor** — remove the duplicate backend guard, simplify Angular 422 handling, and correct the brittle test assertion.
2. **Backend-only** — smaller but leaves concrete frontend casting and fake-data complexity.
3. **Test-only** — restores the baseline but leaves both objective duplications.

## Recommendation

Use option 1. It removes concrete complexity with no new abstraction and keeps every public and security-sensitive boundary unchanged.

## Human Review Gate

Option 1 approved by the user on 2026-08-31. Written design review remains required before implementation by the active brainstorming workflow.
