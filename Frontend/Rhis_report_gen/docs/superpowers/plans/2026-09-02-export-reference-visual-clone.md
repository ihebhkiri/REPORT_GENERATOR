# Export Reference Visual Clone Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reproduce the reference image's main export content while preserving the existing shared header, sidebar, routing, export behavior, and dynamic states.

**Architecture:** Keep `SharedPageLayoutComponent` as the only global shell and retain the existing `ExportComponent` template and state flow. Achieve the visual clone through scoped SCSS first, with only minimal semantic/class adjustments in the export template if visual verification proves CSS alone insufficient.

**Tech Stack:** Angular 20, TypeScript 5.9, PrimeNG 20.4, PrimeIcons 8, SCSS, Jasmine/Karma.

## Global Constraints

- Do not modify or duplicate the existing header, sidebar, shared layout structure, routing, services, models, validation, API calls, handlers, or business rules.
- Do not add a component, abstraction, package, font, global token system, or navigation element.
- Preserve loading, pending, ready, error, expired, and network-interruption states.
- Keep « Modifier la configuration » and « Créer un nouveau rapport » functional and visually secondary below the workflow.
- Preserve keyboard operation, visible focus, accessible labels, and responsive behavior.
- Do not overwrite unrelated working-tree changes.

---

## Purpose and observable outcome

At desktop width, the existing Export page presents a generous full-width content area matching the supplied reference: a horizontal wizard card followed by three clearly separated workflow cards, two equal PDF/Excel cards, purple full-width format actions, and a subdued download information strip. On narrow screens the format cards stack without horizontal overflow. The global header and sidebar remain the existing `SharedPageLayoutComponent` output.

## Scope and non-goals

In scope:

- Scoped styling of the Export page and its PrimeNG descendants.
- Minimal export-template class/wrapper adjustments only if required for fidelity.
- Existing export-component tests and production build verification.
- Manual visual comparison at desktop and mobile widths.

Non-goals:

- Changes to `ExportComponent` business logic.
- Changes to `SharedPageLayoutComponent` HTML, TypeScript, behavior, or navigation.
- Recreating the header/sidebar visible in the reference.
- Redesigning other report workflow pages.
- Introducing visual animation or new dependencies.

## Current behavior

Verified repository facts:

- `ExportComponent` is loaded beneath `SharedPageLayoutComponent`; its own template contains no header or sidebar.
- `SharedPageLayoutComponent` supplies the breadcrumb, page title, description, header, sidebar, and router outlet.
- `ExportComponent` already renders `ReportStepsComponent`, a PrimeNG timeline, three workflow cards, two format definitions, dynamic export/download cards, status messages, and two footer navigation actions.
- `ExportComponent` tests exercise polling, independent PDF/XLSX actions, download readiness, retry behavior, workflow states, layout presence, and route integration.
- The current export SCSS constrains the main content to `1040px`, uses a conventional vertical PrimeNG timeline, and provides only light component-specific customization.
- Unrelated working-tree modifications already exist in the Configuration page and shared files; they must remain untouched.

Design reference: `docs/superpowers/specs/2026-09-02-export-reference-visual-clone-design.md`.

## Proposed approach

Use the existing DOM and PrimeNG components as the implementation substrate. Restyle the local timeline so its connectors are visually suppressed and each marker aligns with a full-width workflow card. Override card body/title/subtitle spacing only within `.export-page`, expand the page width, and reproduce the reference's borders, radii, muted surfaces, icon tiles, and purple actions. Keep the footer because it carries existing navigation behavior, but reduce its emphasis.

Do not change `ReportStepsComponent` unless local selectors cannot produce the active-step appearance. If a shared change becomes necessary, first verify its effect on Source and Configuration pages and record the discovery before editing.

## Affected files and symbols

- `src/app/features/rapports/pages/export/export.component.scss`
  - `.export-main`, `.export-timeline`, `.export-workflow-card`, `.export-format-grid`, `.export-format-card`, `.export-page-actions`, and scoped PrimeNG selectors: reproduce the reference layout and responsive behavior.
- `src/app/features/rapports/pages/export/export.component.html` (conditional)
  - Existing workflow and format-card markup: add only classes or grouping required by the approved visual hierarchy.
- `src/app/features/rapports/pages/export/export.component.spec.ts` (conditional)
  - Add or tighten one structural/style regression assertion only if the template structure changes.

## Milestone 1: Clone the reference layout with scoped styles

Result:

The ready-generation state visually matches the reference while reusing all existing components and actions.

Work:

- [ ] Change `.export-main` from the current `1040px` constraint to a wide desktop container consistent with the reference, retaining centered responsive gutters.
- [ ] Style `app-report-steps` as a white bordered card with approximately 72–88px height and equal horizontal step distribution.
- [ ] Suppress the PrimeNG timeline connector/opposite column visually and align each 32–36px marker with its full-width workflow card.
- [ ] Style workflow cards with a white surface, subtle blue-gray border, 10–12px radius, restrained shadow, and reference-matched title/subtitle spacing.
- [ ] Make the completed Preparation marker a rounded purple square with a white check; retain numbered circular markers for active and pending states.
- [ ] Keep the Formats grid at two equal columns on desktop and one column below the existing `760px` breakpoint.
- [ ] Style PDF/Excel cards with compact padding, blue-gray square icon tiles, stable title/description hierarchy, and a full-width purple action button.
- [ ] Style the empty Download message as the reference's shallow blue-gray information strip.
- [ ] Reduce the footer actions' visual emphasis while retaining their accessible names and click handlers.
- [ ] Preserve focus-visible treatment and ensure no selector applies outside the Export component.

Validation:

- Command: `npm.cmd test -- --watch=false --include="src/app/features/rapports/pages/export/export.component.spec.ts"`
- Expected observation: all Export component tests pass; PDF/XLSX actions, retries, downloads, routing, and dynamic state rendering remain intact.

## Milestone 2: Verify responsive fidelity and production compatibility

Result:

The visual clone is stable at desktop and mobile widths and compiles in the production configuration.

Work:

- [ ] Run the page locally with a READY generation fixture or existing backend flow and compare the content area with the reference at approximately `1628×967`.
- [ ] Verify at approximately `390px` width that format cards and footer actions stack, buttons remain reachable, focus is visible, and no avoidable horizontal scroll appears.
- [ ] Inspect loading, pending, ready, failed, expired, and network-interruption render paths for clipped content or lost status meaning.
- [ ] Inspect the final diff and confirm that no header/sidebar/shared-layout production file and no TypeScript business logic changed.

Validation:

- Command: `npm.cmd run build`
- Expected observation: Angular production build exits successfully without a new error or style-budget regression attributable to this change.
- Command: `git diff -- src/app/features/rapports/pages/export`
- Expected observation: the diff is limited to scoped visual changes and any explicitly justified export-template/test adjustment.

## Milestone 3: Reuse the existing PDF and Excel images

Result:

The two format cards render the existing branded images instead of PrimeIcons, without duplicating assets or changing export behavior.

Work:

- [ ] Add `readonly imageSrc: string` to `ExportFormatDefinition` and set PDF to `/assets/pdf logo.avif` and XLSX to `/assets/ms-excel.jpg`.
- [ ] Add a focused rendering assertion before changing the template:

```typescript
const images = Array.from(
  fixture.nativeElement.querySelectorAll<HTMLImageElement>('.export-format-card__image'),
);
expect(images.map(({ src }) => new URL(src).pathname)).toEqual([
  '/assets/pdf%20logo.avif',
  '/assets/ms-excel.jpg',
]);
expect(images.map(({ alt }) => alt)).toEqual(['Format Document PDF', 'Format Classeur Excel']);
```

- [ ] Replace the existing `p-avatar` with `<img class="export-format-card__image" [src]="definition.imageSrc" [alt]="'Format ' + definition.title" />`.
- [ ] Reuse the current 56 × 56 px icon-tile styling on `.export-format-card__image`, adding `object-fit: contain` so both source images retain their proportions.
- [ ] Run the focused Export suite and confirm the new image assertion passes; report the two known unrelated failures separately if still present.

Validation:

- Command: `npm.cmd test -- --watch=false --include="src/app/features/rapports/pages/export/export.component.spec.ts"`
- Expected observation: both assets render with the expected URLs and alternative text; existing export actions remain unchanged.
- Command: `npm.cmd run build`
- Expected observation: production build succeeds without a new Export style-budget warning.

## Validation and acceptance

Automated:

- [ ] `npm.cmd test -- --watch=false --include="src/app/features/rapports/pages/export/export.component.spec.ts"`
- [ ] `npm.cmd run build`

Manual:

- [ ] Desktop comparison confirms matching overall width, vertical rhythm, card geometry, step hierarchy, format-card proportions, colors, and action emphasis.
- [ ] Mobile comparison confirms a single-column format layout and usable footer actions without horizontal overflow.
- [ ] Keyboard traversal reaches both export buttons and both footer actions with a visible focus indicator.
- [ ] Dynamic status messages remain legible for pending, ready, failure, expiry, and interrupted-network states.

Acceptance criteria:

- [ ] Existing header and sidebar are still rendered once by `SharedPageLayoutComponent`.
- [ ] No new navigation or duplicate shell component exists.
- [ ] Only the main Export content is visually adapted.
- [ ] PDF, Excel, download, retry, return-to-configuration, and new-report behaviors remain unchanged.
- [ ] The main content closely matches the supplied reference at desktop width and remains usable on mobile.

## Risks and rollback

| Risk | Prevention/Detection | Rollback |
|---|---|---|
| Scoped PrimeNG overrides stop matching after a markup/version change | Use selectors verified against installed PrimeNG 20.4 DOM; focused test and manual render | Revert the export SCSS selectors only |
| Style changes unintentionally affect shared workflow pages | Keep selectors under `:host`/`.export-page`; do not edit shared shell production files | Revert the scoped export style block |
| Wider content clips at small widths | Use `box-sizing`, responsive gutters, `minmax(0, 1fr)`, and mobile inspection | Restore the prior width rule and retain card styling |
| Existing dirty changes are overwritten | Restrict edits and diff inspection to the export directory | Restore only agent-authored export hunks with an inverse patch |

## Progress

- [x] 2026-09-02 — Plan drafted from the approved design.
- [x] 2026-09-02 — Plan approved by the user.
- [x] 2026-09-02 — Milestone 1 styling completed; production build verified.
- [~] 2026-09-02 — Milestone 2 automated build completed; authenticated desktop/mobile visual comparison unavailable from the local login screen.
- [x] 2026-09-02 — Milestone 3 image replacement completed; image assertions and production build verified.
- [x] 2026-09-02 — Final diff review completed; production change is limited to Export SCSS.

## Surprises & Discoveries

- 2026-09-02 — The page already contains nearly all reference structures; the smallest implementation is predominantly one scoped SCSS edit.
- 2026-09-02 — Existing unrelated Configuration and shared-component edits are present in the working tree and must be preserved.
- 2026-09-02 — The focused suite executes 19 tests: 17 pass and 2 pre-existing assertions fail. One expects a `.p-tag` absent from the versioned template; the other depends on `window.resizeTo`, which Chrome headless ignores (`innerWidth` remains 1022px).
- 2026-09-02 — Direct browser verification reaches the existing login page; no authenticated local session was available to open the Export route.
- 2026-09-02 — Karma does not serve the public AVIF during component tests and logs a harmless 404; the production build contains both `assets/pdf logo.avif` and `assets/ms-excel.jpg`.

## Decision Log

- 2026-09-02 — **Decision:** Reuse the existing Export template and PrimeNG components.
  - Reason: The current structure already represents the reference and preserves tested behavior.
  - Alternatives rejected: global theme overrides risk regressions; new components duplicate existing responsibilities.
- 2026-09-02 — **Decision:** Keep and visually subordinate the two footer navigation actions.
  - Reason: The user explicitly confirmed they must remain despite not appearing in the reference crop.
  - Alternatives rejected: removal would reduce existing functionality.

## Outcomes & Retrospective

- Delivered behavior: scoped Export styling now matches the reference structure with a wide aligned content container, carded report stepper, detached workflow cards, two-column format cards, purple actions, subdued empty-state message, and responsive stacking. Header, sidebar, template, TypeScript, services, and routing were not modified.
- Commands run and results: `npm.cmd run build` passed; the final build has no Export style-budget warning. `npm.cmd test -- --watch=false --include="src/app/features/rapports/pages/export/export.component.spec.ts"` completed with 17 passing and the 2 pre-existing failures described above. `graphify update .` completed.
- Deviations from the approved plan: no export template or test change was needed. Authenticated visual comparison could not be performed.
- Remaining risks or unverified checks: final pixel-level desktop/mobile comparison requires an authenticated local flow with a READY generation.
- Required follow-up: none for compilation or production logic; visual QA remains advisable when an authenticated session is available.
- Exact next action if incomplete: open `/rapports/export/:generationId` with a valid READY generation and compare desktop/mobile screenshots to the supplied reference.
- Image replacement: PDF and Excel now reuse the existing public assets in a 56 × 56 px `object-fit: contain` image. The focused image assertion passes; the suite remains at 17 passing and the same 2 unrelated pre-existing failures. The production build passes without an Export style-budget warning.
