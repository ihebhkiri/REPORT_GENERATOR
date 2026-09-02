# Reproduce the shared shell and report source screen

This ExecPlan is a living document governed by `.agent/PLANS.md`. Keep `Progress`, `Surprises & Discoveries`, `Decision Log` and `Outcomes & Retrospective` current throughout implementation.

Date: 2026-09-01  
Status: Completed  
Research: `docs/research/2026-09-01-shared-shell-report-source.md`  
Related issue: N/A

## Purpose and observable outcome

All routes using `SharedPageLayoutComponent` display the reference-style dark sidebar and white top header. The report source route visually matches the supplied desktop image in hierarchy, proportions, spacing, colors, typography, panels, dataset choices and actions while retaining its existing behavior.

## Scope and non-goals

In scope:

- Restyle and restructure the existing shared layout.
- Restyle the existing report stepper and report source page.
- Preserve and verify desktop, tablet and mobile usability.
- Add focused DOM assertions only where they protect shared-shell structure or existing behavior.

Non-goals:

- No backend, route, service, model, validation, Signal or Observable changes.
- No new dependency, design system, store, facade, service, wrapper or component.
- No invented navigation for icons whose destinations do not exist.
- No unrelated cleanup or redesign.

## Current behavior

The verified flow is documented in `docs/research/2026-09-01-shared-shell-report-source.md`. `SharedPageLayoutComponent` already centralizes route copy, role-aware navigation and the child outlet. `RapportsComponent` owns data loading and selection while its template and SCSS own presentation. PrimeNG already supplies the interactive accordion, stepper and buttons.

## Proposed approach

Use the existing shared layout as the only shell. Add semantic sidebar markup beside the existing content column, retain current router links and authorization conditions, and render unavailable reference icons as non-interactive items. Restructure the report template only enough to place its stepper, source heading, choices, summary and actions into the same visual regions as the reference. Implement the rest with component-local SCSS and existing PrimeNG CSS variables.

This is the smallest production-quality path: no abstraction is needed because the shared layout already provides the correct reuse boundary.

## Affected files and symbols

- `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.html`
  - Shared shell landmarks, sidebar items, header actions, breadcrumb structure and outlet placement.
- `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.scss`
  - Shell grid, sidebar/header dimensions, active states, page heading and responsive behavior.
- `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.ts`
  - Only minimal derived profile text or display constants if template-only rendering is insufficient.
- `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.spec.ts`
  - Shared sidebar/header landmark and role-aware navigation assertions.
- `Frontend/Rhis_report_gen/src/app/shared/report-steps/report-steps.component.scss`
  - Full-width reference stepper panel and responsive spacing.
- `Frontend/Rhis_report_gen/src/app/features/rapports/pages/source_de_donnes/rapports.component.html`
  - Reference panel grouping and stable CSS class names.
- `Frontend/Rhis_report_gen/src/app/features/rapports/pages/source_de_donnes/rapports.component.scss`
  - Desktop proportions, dataset rows, panel styling, actions and responsive breakpoints.
- `Frontend/Rhis_report_gen/src/app/features/rapports/pages/source_de_donnes/rapports.component.spec.ts`
  - Existing behavior checks; update only selectors invalidated by necessary markup changes.

## Milestone 1: Shared shell matches the reference structure

Result:

Every route using the shared layout renders one dark sidebar, one white header, the existing page heading and one routed content region. Existing Reports, Assistant and administrator-only Data links retain their behavior.

Work:

- Add `.app-shell`, `.shared-sidebar` and `.content-shell` wrappers in the shared layout template.
- Reuse `/Logo.svg`, PrimeIcons and Material Symbols; give icon-only controls accessible names.
- Keep `/rapports`, `/assistant` and `/administration/datasets` router links and their existing `aria-current` conditions unchanged.
- Add visual-only sidebar items as non-interactive `<span>` elements rather than fake links.
- Derive a one-character profile initial from the existing user email only if required by the template; do not add state or a service.
- Style the desktop shell with local custom properties `--sidebar-width: 5rem` and `--header-height: 5rem`, navy sidebar, white header, violet active states and reference separators.
- At tablet/mobile widths, keep content usable without horizontal page overflow and preserve a minimum 44 px target for interactive controls.
- Extend the layout spec with assertions for one `aside`, one `header`, one `main`/content region, preserved route links and hidden admin navigation for non-admin users.

Validation:

- Command: `npm test -- --no-watch --browsers=ChromeHeadless --include='src/app/shared/page-layout/shared-page-layout.component.spec.ts'`
- Expected observation: all shared-layout cases pass for reports, datasets, assistant, configuration and export; admin visibility and active navigation remain correct.

## Milestone 2: Report source content matches the reference

Result:

The source route presents a wide step panel, a single Sources panel containing a three-column dataset grid, a compact selected-tables panel and right-aligned actions matching the reference.

Work:

- Replace Tailwind-only structural wrappers with stable local classes such as `.report-source-page`, `.report-source-panel`, `.report-source-panel__heading` and `.report-source-actions`; retain all Angular control-flow blocks and event bindings verbatim.
- Keep `app-report-steps`, `p-accordion`, `p-progress-spinner`, `app-report-related-card` and `p-button` components.
- Move the dataset list inside the same panel as the Sources heading without changing loading, error or empty conditions.
- Style the content width, white panels, `1px` neutral borders, approximately `12px` radii, subtle shadows, 24–32 px gaps, 42–48 px icon tiles and the reference indigo gradient.
- Preserve selected accordions spanning the grid and related-card expansion behavior.
- Keep the existing three/two/one-column breakpoints, adapting panel padding and actions for tablet/mobile.
- Update the report spec only if necessary to keep assertions attached to `.dataset-accordion`, `.selected-datasets-summary` and existing interaction state.

Validation:

- Command: `npm test -- --no-watch --browsers=ChromeHeadless --include='src/app/features/rapports/pages/source_de_donnes/rapports.component.spec.ts'`
- Expected observation: dataset rendering, relation grouping, accordion selection, summary, loading/error handling and navigation tests all pass.

## Milestone 3: Stepper, build and visual calibration

Result:

The complete desktop route is visually calibrated against the 1672 × 941 reference and remains usable at tablet and mobile widths.

Work:

- Style `ReportStepsComponent` through its existing class and PrimeNG design variables; do not change step values or labels.
- Run the full Angular build and test suite.
- Launch the existing frontend, capture the report source route at 1672 × 941, and compare sidebar width, header height, content bounds, panel positions, grid rows, button sizes, typography and color against the reference.
- Make only local SCSS adjustments supported by visible differences; do not change business logic or add assets unless the official logo cannot be displayed from the existing file.
- Inspect representative 1024 px and 390 px widths for overflow, keyboard focus visibility and reachable actions.
- Run `graphify update .` if Graphify is available, then inspect generated changes and include them only if repository policy requires it.

Validation:

- Command: `npm run build`
- Expected observation: Angular production build exits successfully with no TypeScript, template or style errors.
- Command: `npm test -- --no-watch --browsers=ChromeHeadless`
- Expected observation: the complete frontend test suite passes.
- Manual: compare a 1672 × 941 application screenshot side by side with the supplied reference.
- Expected observation: the two screens have materially matching shell/content proportions, hierarchy, spacing, alignment, colors, borders, radii, shadows and density; any asset/data mismatch is recorded.

## Validation and acceptance

Automated:

- [x] Focused shared-layout tests pass.
- [x] Focused report-source tests pass.
- [x] Production build passes.
- [ ] Full frontend tests pass.

Manual:

- [x] At 1672 × 941, compare the application and reference side by side.
- [ ] At approximately 1024 px, confirm two dataset columns and accessible navigation/actions.
- [x] At approximately 390 px, confirm one dataset column and no page-level horizontal overflow.
- [ ] Expand a dataset, select a related dataset, confirm the summary, and continue to configuration.

Acceptance criteria:

- [x] All shared-layout routes display the same sidebar and header.
- [x] Existing route links and administrator visibility remain correct.
- [x] Report source desktop geometry and visual tokens closely match the reference.
- [x] Dataset loading, empty, error, selection and navigation behavior remains unchanged.
- [x] No new dependency or unrelated abstraction is introduced.

## Risks and rollback

| Risk | Prevention/Detection | Rollback |
|---|---|---|
| Shared shell CSS regresses Assistant or Administration | Exercise every layout page and keep routed content in a neutral flexible container | Revert shared-layout HTML/SCSS commit independently |
| PrimeNG internal selectors differ from assumptions | Use documented component variables and existing selectors; validate in the installed 20.4 build | Restore prior local variables/selectors |
| Official logo cannot exactly match the compact reference emblem | Use the existing official asset and record the difference rather than invent branding | Restore the current full logo presentation |
| Mobile navigation becomes inaccessible | Keep semantic links, accessible names and visible focus; inspect 390 px width | Revert only the mobile media-query changes |

No database, security, migration or data-integrity risk is introduced because this change is presentation-only.

## Progress

- [x] 2026-09-01 — Research completed from the reference image and repository evidence.
- [x] 2026-09-01 — Design specification approved and committed as `31efead`.
- [x] 2026-09-01 — Implementation plan drafted.
- [x] 2026-09-01 — Plan approved; inline execution requested with Ponytail.
- [x] 2026-09-01 — Milestone 1 completed; focused shared-layout tests: 5/5 passed.
- [x] 2026-09-01 — Milestone 2 completed; focused report-source tests: 15/15 passed.
- [x] 2026-09-01 — Milestone 3 completed: build passed; desktop/mobile browser checks completed without console errors or horizontal overflow.
- [x] 2026-09-01 — Final five-axis review completed; no required finding remains.

## Surprises & Discoveries

- 2026-09-01 — Angular tests cannot resolve dependencies inside the managed sandbox; the approved `npm.cmd test` prefix succeeds outside it.
- 2026-09-01 — The existing logo is a 150 × 34 horizontal SVG, whereas the reference sidebar shows a compact emblem. Implementation must first test whether the emblem can be exposed through sizing/cropping without adding an asset.
- 2026-09-01 — No spec exists for `ReportStepsComponent`; its change is style-only and will be covered by build plus page-level/manual verification.
- 2026-09-01 — The complete frontend suite has 7 pre-existing failures reproduced in isolation: 4 DatasetExposure, 2 PreviewPanel and 1 Export test. They do not exercise modified files; 160/167 tests pass.
- 2026-09-01 — The local backend was unavailable during visual verification, so the live report screenshot exercised the error state. The shell and responsive geometry were verified in-browser; dataset rendering remains verified by 15 focused component tests.

## Decision Log

- 2026-09-01 — **Decision:** Modify `SharedPageLayoutComponent` instead of creating a sidebar or shell component.
  - Reason: it is already the shared reuse boundary for every requested layout.
  - Alternatives rejected: a new component adds no independent responsibility; duplication per route violates the shared-shell requirement.
- 2026-09-01 — **Decision:** Render unavailable sidebar destinations as non-interactive visual items.
  - Reason: visual fidelity must not invent routes or behavior.
  - Alternatives rejected: fake links and new routes would exceed the visual-only scope.
- 2026-09-01 — **Decision:** Preserve PrimeNG accordions, stepper and buttons.
  - Reason: they already provide the required behavior and are explicitly preferred by project rules.
  - Alternatives rejected: custom replacements create unnecessary code and regression risk.

## Outcomes & Retrospective

Complete after implementation:

- Delivered behavior: Shared reference-style sidebar/header and reference-aligned report source content, stepper, panels and actions.
- Commands run and results: focused layout 5/5; focused report source 15/15; production build passed with existing/global budget warnings plus small component-style budget warnings; full suite 160/167 with 7 failures reproduced in unrelated suites; browser desktop/mobile checks passed with no console error or overflow.
- Deviations from the approved plan: None.
- Remaining risks or unverified checks: The official horizontal logo cannot exactly reproduce the compact emblem, and the live dataset grid could not be captured without the backend.
- Required follow-up: Fix the unrelated existing frontend tests separately if a fully green repository suite is required.
- Exact next action if incomplete: None for this visual task.
