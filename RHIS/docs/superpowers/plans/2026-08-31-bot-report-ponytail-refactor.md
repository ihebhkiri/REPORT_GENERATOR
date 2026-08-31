# Bot Report Ponytail Refactor Implementation Plan

> **For agentic workers:** Execute inline and milestone by milestone. Subagents are not authorized for this task.

**Goal:** Remove three verified Bot Report complexities while preserving all frontend/backend behavior and public contracts.

**Architecture:** Keep the existing Angular → controller → catalog/planner → backend validation → resolver → generation flow. Delete only a duplicate private backend guard, replace fake frontend error objects with direct state updates, and repair one brittle prompt-text assertion.

**Tech Stack:** Angular 20, TypeScript 5.9, RxJS 7.8, Java 17, Spring Boot 4.1, JUnit 5, Mockito, Maven.

## Global Constraints

- Preserve request/response DTOs, endpoints, statuses, bodies, security, `Location`, and `Idempotency-Key`.
- Preserve backend catalog, plan, dataset/field/relation/filter/sort validation and parameterized SQL flow.
- Preserve clarification behavior: no resolver, SQL, or generation.
- Preserve all unrelated and pre-existing working-tree changes.
- Add no dependency, abstraction, layer, interface, helper, file move, or broad reformatting.
- Do not commit without explicit authorization.

## Purpose and observable outcome

Bot Report behaves identically, but production code no longer validates the same plan shape twice or fabricates transport objects to render a structured 422 error. The directly associated test suite is green, and frontend/backend builds succeed.

## Scope and non-goals

In scope:

- `BotReportService.toPreviewRequest()` duplicate guard.
- `ReportAssistantComponent.handleHttpError()` structured 422 branch.
- `BotReportPlannerTest.promptDefinesMultiDatasetRules()` apostrophe mismatch.

Non-goals:

- Retry orchestration, prompts, DTOs, catalog records, controller mappings, resolver/generation services, SQL, HTML, SCSS, or unrelated test cleanup.

## Current behavior

Research: `RHIS/docs/superpowers/research/2026-08-31-bot-report-ponytail-refactor.md`.

`ReportAssistantComponent.submit()` calls the Angular service, which posts the natural-language request with credentials and `Idempotency-Key`. `BotReportController.createReport()` delegates to `BotReportService.generate()` and maps READY/FAILED/clarification to 202/422/200. The service builds the authorized catalog, performs at most two planner calls, validates READY plans, invokes `ReportDefinitionResolver.resolve()`, and calls `ReportGenerationService.create()`.

## Affected files and symbols

| File | Action | Responsibility |
|---|---|---|
| `RHIS/src/main/java/RHIS/com/RHIS/bot/BotReportService.java` | Modify | Remove duplicated READY-shape validation from private conversion method |
| `Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.ts` | Modify | Handle structured 422 failure without fake response/request objects |
| `RHIS/src/test/java/RHIS/com/RHIS/bot/BotReportPlannerTest.java` | Modify | Match the prompt's actual typographic apostrophe |
| `RHIS/docs/superpowers/plans/2026-08-31-bot-report-ponytail-refactor.md` | Update | Living progress and verification evidence |

## Milestone 1: Apply the surgical simplifications

Result:

The duplicate backend guard and fake frontend transport objects are removed, with no new helper or abstraction.

Work:

- In `BotReportService.toPreviewRequest()`, delete only:

```java
if (!plan.isReady() || plan.rootDatasetId() == null
        || plan.selectedFieldIds() == null || plan.selectedFieldIds().isEmpty()) {
    throw new ReportValidationException(
            "Le modèle n'a pas produit de définition de rapport exploitable.");
}
```

- Keep `createGeneration()` calling `validatePlan(plan, catalog)` immediately before `toPreviewRequest(plan)`.
- In `ReportAssistantComponent.handleHttpError()`, replace the fabricated response and dummy request with:

```ts
this.clarificationContext.set(null);
this.addMessage('assistant', body.errors.join(' ') || 'La création du rapport a échoué.');
return;
```

Validation:

- Command: `rg -n "toPreviewRequest|validatePlan|handleHttpError|handleResponse"` on the two modified production files.
- Expected observation: the backend conversion method has one caller after authoritative validation; the 422 branch has no cast or dummy request.

## Milestone 2: Repair and run focused tests

Result:

The directly associated tests protect the unchanged behavior and pass.

Work:

- In `BotReportPlannerTest.promptDefinesMultiDatasetRules()`, replace the impossible cross-line substring `ordre d'apparition` with the actual prompt fragment `d’apparition`.
- Remove the assertion for `ne produis jamais de SQL`, which is absent from the current prompt; do not add it to the prompt because that would change behavior outside a refactor.
- Do not modify the system prompt.

Validation:

- Command: `npm.cmd test -- --watch=false --include="src/app/features/report-assistant/*.spec.ts"` from `Frontend/Rhis_report_gen`.
- Expected observation: 8 tests pass, including request construction, idempotency, clarification, READY, and session error behavior.
- Command: `mvn "-Dtest=BotReportServiceTest,BotReportPlannerTest,BotReportControllerSecurityTest,ReportCatalogProviderTest,BotAiPropertiesTest" test` from `RHIS`.
- Expected observation: all 28 targeted backend tests pass.

## Milestone 3: Integrated build and final scope review

Result:

Both applications build, Graphify is refreshed, and the final diff contains no unintended production changes.

Work:

- Run the Angular build.
- Run the backend package with tests already covered by the focused suite.
- Run Graphify update if available.
- Compare the final diff against the pre-existing dirty state and inspect only the approved paths.

Validation:

- Command: `npm.cmd run build` from `Frontend/Rhis_report_gen`.
- Expected observation: Angular production build succeeds; warnings are recorded.
- Command: `mvn package` from `RHIS`.
- Expected observation: backend compilation, tests, and packaging succeed; any environment-dependent skipped coverage is recorded.
- Command: `graphify update .` from the repository root.
- Expected observation: update succeeds; generated changes are identified separately from source edits.
- Command: `git diff --check` plus scoped `git diff`/`git status` inspection.
- Expected observation: no whitespace errors or source modifications outside the three approved files.

## Validation and acceptance

Automated:

- [x] Angular targeted tests pass: 8/8 after one Chrome ping-timeout rerun.
- [x] Backend targeted tests pass: 28/28.
- [x] Angular build passes with pre-existing bundle budget warnings.
- [~] Full `mvn package` is blocked by eight unrelated report/export failures; `mvn -DskipTests package` succeeds and produces the executable jar.
- [x] Graphify update completes: 2777 nodes, 5890 edges, 175 communities.

Acceptance criteria:

- [x] Public HTTP and DTO contracts are unchanged.
- [x] Backend validation, authorization, SQL, clarification, and generation flow are unchanged.
- [x] Angular user-visible messages and accessibility are unchanged.
- [x] Production code is objectively smaller and contains no new concept.
- [x] Existing user changes remain present.

## Risks and rollback

| Risk | Prevention/Detection | Rollback |
|---|---|---|
| Duplicate guard removal bypasses validation | Verify the only caller and the preceding `validatePlan()` call; run service tests | Restore the deleted private guard |
| Direct 422 handling changes UI state/text | Preserve the exact two FAILED state updates and run component tests | Restore the previous constructed-response branch |
| Dirty working tree overlap | Use line-scoped patches and review the scoped diff | Reverse only this plan's exact line edits; never reset files |

## Progress

- [x] 2026-08-31 — Research and baseline completed: Angular 8/8; backend 27/28 with one apostrophe assertion failure.
- [x] 2026-08-31 — Design approved by the user.
- [x] 2026-08-31 — Plan drafted and reviewed.
- [x] 2026-08-31 — Milestone 1 completed and verified by scoped caller/state inspection and `git diff --check`.
- [x] 2026-08-31 — Milestone 2 completed: Angular 8/8 and backend 28/28 targeted tests passed.
- [x] 2026-08-31 — Milestone 3 completed with full-suite failures recorded; artifact packaging, Graphify, and scoped diff checks succeeded.
- [x] 2026-08-31 — Final five-axis review completed with no finding in the approved refactor.

## Surprises & Discoveries

- 2026-08-31 — Angular tests require execution outside the filesystem sandbox to resolve installed dependencies.
- 2026-08-31 — The only backend baseline failure is the ASCII/typographic apostrophe mismatch at `BotReportPlannerTest:82`; 27 other targeted tests pass.
- 2026-08-31 — The failed assertion also crossed a line break in the text block (`ordre` / `d’apparition`); checking the stable fragment `d’apparition` preserves the intended rule without changing the prompt.
- 2026-08-31 — After the first assertion passed, the same test exposed a second stale assertion for text absent from the prompt (`ne produis jamais de SQL`). It was removed instead of changing planner behavior.
- 2026-08-31 — Full `mvn package` runs 99 tests but fails in four XLSX tests (`rowAccessWindowSize`) and four PostgreSQL integration tests (`ReportJobConfiguration` context); these files and failures are outside Bot Report.
- 2026-08-31 — Global `git diff --check` reports pre-existing trailing whitespace in `DataSetRepository.java:60`; scoped checks for the modified Bot Report files pass.

## Decision Log

- 2026-08-31 — **Decision:** Apply only the three-file surgical option.
  - Reason: it removes two concrete production complexities and one brittle test without changing architecture or behavior.
  - Alternatives rejected: backend-only leaves fake frontend objects; test-only leaves both production duplications; broader retry/catalog/UI refactors lack sufficient benefit.

## Outcomes & Retrospective

- Delivered behavior: duplicate backend READY-shape validation removed; Angular structured 422 handling no longer fabricates transport objects; stale prompt-text assertions corrected without changing the prompt.
- Commands run and results: targeted Angular 8/8; targeted backend 28/28; Angular production build passed with two budget warnings; backend jar packaged with tests skipped; Graphify updated; scoped diff checks passed.
- Deviations from the approved plan: the prompt test had a second stale assertion discovered only after the first was corrected; it was removed rather than changing behavior.
- Remaining risks or unverified checks: full backend suite remains red due to eight unrelated report/export failures; global whitespace check remains red due to pre-existing `DataSetRepository.java:60`.
- Required follow-up: none identified.
- Exact next action if incomplete: none for Bot Report; investigate the unrelated report/export baseline only as a separate task.
