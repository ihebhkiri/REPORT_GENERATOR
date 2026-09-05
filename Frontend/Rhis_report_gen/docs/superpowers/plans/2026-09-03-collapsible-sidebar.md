# Collapsible sidebar Implementation Plan

> **For agentic workers:** Use executing-plans to implement this plan task-by-task, inline. No delegation or commit requested.

**Goal:** Toggle the existing sidebar between its original collapsed rendering and a 15rem expanded rendering.

**Architecture:** Keep SharedPageLayoutComponent and all existing navigation nodes. A local `signal(true)` named `isCollapsed` controls an expanded CSS class, labels and an accessible native button. Reuse the cropped full-logo asset.

**Tech Stack:** Angular 20, TypeScript, existing SCSS and Jasmine/Karma.

## Global Constraints

Approved spec: `../specs/2026-09-03-collapsible-sidebar-design.md` (approved in conversation).
No new sidebar, menu model, service, dependency, route or persistence. Preserve role conditions, active states, collapsed dimensions and mobile hiding. Preserve pre-existing export edits. Work on `codex/collapsible-sidebar`; no commits.

## Task 1: Implement and verify the two states

Files: `src/app/shared/page-layout/shared-page-layout.component.{ts,html,scss,spec.ts}`.
Consumes: existing `isAdmin()`, `isReports`, `isDatasets`, `isAssistant`, router links and logo.
Produces: `readonly isCollapsed = signal(true)` and a native toggle button.

- [x] Establish baseline with `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/shared/page-layout/shared-page-layout.component.spec.ts`.
- [x] Add a regression test exercising the same DOM nodes through both states, label visibility, logo width, roles and active hrefs. Use `button.click(); fixture.detectChanges(); expect(button.getAttribute('aria-expanded')).toBe('true');`, then click again and expect false. Run before implementation to demonstrate failure.
- [x] Add `signal` to the existing Angular import and `readonly isCollapsed = signal(true);` to the component. Bind `[class.sidebar-expanded]="!isCollapsed()"` on `.app-shell`.
- [x] Add `.sidebar-label` spans inside the existing navigation elements with the labels from the spec; hide them by default with `display: none`. Preserve every route and role branch. Give functional links accessible names in collapsed mode.
- [x] Replace the footer toggle span with `<button type="button" class="sidebar-collapse" (click)="isCollapsed.set(!isCollapsed())" [attr.aria-expanded]="!isCollapsed()" [attr.aria-label]="isCollapsed() ? 'Développer la navigation' : 'Réduire la navigation'">`. Keep the original chevron and rotate it in expanded mode. Remove footer aria-hidden, retaining it on decorative help.
- [x] Expanded CSS: `--sidebar-width: 15rem`, full logo container width `9.375rem`, labels `display: inline; font-size: .875rem`, items width `calc(100% - 1.75rem)` with border-box sizing, left alignment, `.75rem` gap/padding. Reset native button padding/border and add focus-visible styling. No animation needed.
- [x] Run focused component and route tests, then `npm.cmd run build`. Verify layout in browser if available; otherwise report the exact limitation. Review the final diff using code-review-and-quality.
- [x] Run `graphify update .` from repository root and report generated changes separately.

## Progress

- [x] 2026-09-03: approach and written spec approved; source and styles inspected.
- [x] 2026-09-03: baseline, implementation, focused tests, full suite, build and browser verification executed; unrelated full-suite failures remain below.

## Surprises & Discoveries

Baseline confirmed: five failures from removed `.primary-nav` and old configuration copy; stale expectations corrected. Regression test then failed five times specifically on missing button before implementation. Component and route tests now pass (10/10); Karma warns about PrimeIcons font asset 404s.
Browser inspection at 720px height found the existing footer overflows the fixed sidebar, making the white toggle nearly invisible outside its blue background. Add `overflow-y: auto` to the existing sidebar so its contents remain accessible by scrolling; normal-height dimensions remain unchanged.
Git reports dubious ownership in sandbox; branch creation requires an explicit per-command safe.directory and approval, not global config changes.

## Decision Log

- 2026-09-03: local signal and CSS only; no persistence, animation or mobile drawer. Existing behavior and approved scope take precedence over speculative additions.
- 2026-09-03: execute inline after two approvals; no additional design gate for this local change.
- 2026-09-03: minimal accessibility correction for short windows: scroll the existing sidebar vertically rather than redesigning or compressing its menu.

## Risks and rollback

Preserve narrow-screen hiding and collapsed visuals. Revert only this feature's component/document changes if needed; do not touch the user's export edits. No database or backend change.

## Outcomes & Retrospective

Delivered on `codex/collapsible-sidebar` (from `improving-ui`), uncommitted. Only the four shared layout code/test files changed for this feature; the pre-existing export edits were not edited. Spec and plan added; Graphify generated files updated separately.

Verification:
- `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/shared/page-layout/*.spec.ts`: 10/10 passed.
- `npm.cmd test -- --watch=false --browsers=ChromeHeadless`: 161 passed, 7 failed. These failing fixtures instantiate untouched components directly, not SharedPageLayoutComponent: ConfigurationComponent expects `Suivant` instead of existing `Génerer` (1); PreviewPanelComponent expects absent row counts (2); DatasetExposureComponent expects old empty-state copy and absent `#dataset-detail-title`, including focus checks (4). Not fixed in this task.
- `npm.cmd run build`: passed after final CSS adjustment. Warnings: initial 603.43kB / 500kB warning budget; shared layout CSS 4.57kB / 4kB; datasets CSS 7.97kB / 4kB; reports CSS 4.59kB / 4kB. No budget changed.
- Karma also reports missing PrimeIcons font assets and PDF image assets; real browser icons render correctly.
- Browser `/assistant`: expanded logo and labels checked at desktop 1440x1000; tablet 1024x720 confirmed collapsed width 72px, expanded width 240px, content left edge 240px. Enter collapses, Space expands, focus is visible. Scrollable sidebar keeps footer usable at short height. At 390x844 the sidebar remains hidden. Browser viewport restored and temporary tab closed.
- `git diff --check`: passed. Five-axis self-review: no routes, role conditions, active-state logic, services or dependencies changed; accessible toggle and no duplicate navigation. No pixel-diff baseline or authenticated backend flow exercised; role behavior is covered by tests.
- `graphify update .`: completed with warnings about eight source files producing no nodes, community label changes and a retained out-of-corpus node; generated graph changes are uncommitted.

No integration attempted because the full suite is not green. Next action belongs to user review; unrelated failures require their own scoped correction.

## Follow-up: animation (2026-09-03)

User requested and approved a discreet 200ms ease-in-out animation. Added CSS transitions on the existing grid columns, logo width and chevron transform; reduced-motion disables all three. Horizontal overflow is clipped during the width transition. No TypeScript, routes or menu changes. This supersedes the initial no-animation choice.

Tests now wait for actual CSS animation completion before asserting final geometry and check transition durations against the active reduced-motion preference. `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/shared/page-layout/*.spec.ts`: 10 passed. `npm.cmd run build`: passed; shared layout CSS now 4.77kB (4kB warning budget); other budget warnings unchanged. Full suite was not rerun for this CSS-only follow-up; the seven previously recorded unrelated failures remain unresolved. No new manual visual or forced reduced-motion browser run in this follow-up. Diff reviewed with code-review-and-quality; no dependency or authorization impact.

## Follow-up: separate compact logo and right chevron (2026-09-03)

User explicitly requested `public/logo_collapsed.png` in collapsed mode and the existing `public/rhis-solutions-logo.png` in expanded mode. The single image now switches source, fits its container and preserves aspect ratio. Chevron is `pi-chevron-right` in both modes as requested; removed the now-unwanted rotation. Expanded toggle uses `align-self: flex-end` and the existing .875rem side inset. Width animation and reduced-motion support remain.

Changed only shared layout HTML, SCSS and spec for this follow-up. The user's new compact PNG and pre-existing deletion of `public/Logo.svg` are untouched. Tests verify both image sources, chevron direction, expanded right alignment and return to collapsed alignment: 10/10 passed with `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/shared/page-layout/*.spec.ts`. `npm.cmd run build` passed with budget warnings (shared layout 4.69kB). Karma also reports `/Logo.svg` missing in a separate route, alongside the existing PrimeIcons asset warnings; no unrelated asset change made. `git diff --check` passed. Full suite and manual browser visual check not repeated for this local correction.
