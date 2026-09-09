# Mobile navigation and report actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. No delegation or commit is authorized.

**Goal:** Make the existing sidebar usable as a mobile drawer and align mobile report navigation/actions with the project’s primary Indigo color and intended content order.

**Architecture:** Extend `SharedPageLayoutComponent` with one mobile-open signal and reuse the existing sidebar DOM as an off-canvas panel. Use CSS ordering on the existing configuration blocks instead of duplicating or moving actions. Reuse current PrimeNG buttons and local action styles on Export without adding dependencies or shared abstractions.

**Tech Stack:** Angular 20 standalone components, TypeScript 5.9, SCSS, PrimeNG 20, Jasmine/Karma.

## Global Constraints

Approved spec: `../specs/2026-09-07-mobile-navigation-report-actions-design.md`.
Breakpoint: mobile is below `48rem`, matching the shared layout and configuration page.
Primary color: existing PrimeNG Indigo 500 (`#6366f1`).
Preserve desktop sidebar collapse/expand, route/role visibility, configuration state, generation rules and export behavior.
Preserve all unrelated dirty working-tree changes, including the in-progress authentication/logout work. No commit, push, merge, dependency or backend change.

---

### Task 1: Mobile sidebar drawer

**Files:**
- Modify: `src/app/shared/page-layout/shared-page-layout.component.ts`
- Modify: `src/app/shared/page-layout/shared-page-layout.component.html`
- Modify: `src/app/shared/page-layout/shared-page-layout.component.scss`
- Test: `src/app/shared/page-layout/shared-page-layout.component.spec.ts`
- Test: `src/app/shared/page-layout/shared-page-layout.routes.spec.ts`

**Interfaces:**
- Consumes: existing `.shared-sidebar`, `.header-brand`, router links, `isCollapsed` desktop state and `RouterOutlet`.
- Produces: `readonly isMobileSidebarOpen = signal(false)`, `toggleMobileSidebar()`, `closeMobileSidebar()` and `handleMobileSidebarKeydown(event: KeyboardEvent)`.

- [ ] **Step 1: Add failing interaction tests.** Assert the hamburger is a native button with `aria-controls="shared-sidebar"` and false `aria-expanded`; click it and expect true plus `.mobile-sidebar-backdrop`. Dispatch Escape and expect false. Open again, activate an existing route link and expect closure. Retain existing desktop collapse tests.
- [ ] **Step 2: Run the focused shared-layout tests and confirm failure.**

  ```powershell
  npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/shared/page-layout/*.spec.ts"
  ```

  Expected before implementation: failure because the hamburger is currently a decorative span and no drawer state/backdrop exists.

- [ ] **Step 3: Implement the smallest accessible state.** Add `isMobileSidebarOpen`, toggle/close methods and an Escape handler that closes only when open. Replace the decorative bars span with:

  ```html
  <button type="button" class="mobile-menu-toggle"
          aria-controls="shared-sidebar"
          [attr.aria-expanded]="isMobileSidebarOpen()"
          aria-label="Ouvrir la navigation"
          (click)="toggleMobileSidebar()">
    <span class="pi pi-bars" aria-hidden="true"></span>
  </button>
  ```

  Give the aside `id="shared-sidebar"`, bind `.mobile-open`, close on its navigation links, and render a native backdrop button while open. Attach `(keydown.escape)` at the shell boundary. Use the same DOM for desktop and mobile.

- [ ] **Step 4: Implement mobile-only drawer CSS.** Replace `.shared-sidebar { display: none; }` under 48rem with a fixed `15rem` panel translated off-screen by default and translated to zero when `.mobile-open`. Show labels and left-align items in this mode. Add a fixed translucent backdrop below the sidebar and above content. Style `.mobile-menu-toggle` as a transparent 44px target with visible focus. Disable drawer transitions under `prefers-reduced-motion`. Do not alter desktop selectors.
- [ ] **Step 5: Run the focused shared-layout tests.** Expected: all shared layout component and route tests pass, including logout/menu tests already present.

### Task 2: Configuration mobile color and content order

**Files:**
- Modify: `src/app/features/rapports/pages/configuration/configuration.component.html`
- Modify: `src/app/features/rapports/pages/configuration/configuration.component.scss`
- Test: `src/app/features/rapports/pages/configuration/configuration.component.spec.ts`

**Interfaces:**
- Consumes: existing `isMobile()`, `mobileTab()`, `.configuration-pane`, `.report-actions`, `app-preview-panel` and tab keyboard behavior.
- Produces: conditional mobile order classes without new business state.

- [ ] **Step 1: Add failing regression tests.** For mobile configuration selection, assert the configuration pane precedes actions in visual order. For preview selection, assert the preview has a lower CSS order than actions. Assert the selected tab background resolves to the project primary color and focus outline no longer uses `#2563eb`.
- [ ] **Step 2: Run the focused configuration test and confirm the new assertions fail.**

  ```powershell
  npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/configuration.component.spec.ts"
  ```

- [ ] **Step 3: Bind the active mobile tab to the action card.** Add `[class.preview-actions-order]="mobileTab() === 'preview'"` on the existing `.report-actions`; do not duplicate the card or its buttons.
- [ ] **Step 4: Apply mobile order and primary tokens.** Use `var(--p-primary-500, #6366f1)` for selected-tab background and focus outline. Under 48rem set explicit flex orders so tabs remain first, the visible panel second and actions last when preview is selected. Keep the current desktop DOM/order and hidden-panel behavior.
- [ ] **Step 5: Run the focused configuration test.** Expected: mobile tabs, keyboard navigation, preview state and action-order tests pass. Record any already-existing unrelated assertions separately.

### Task 3: Export footer action parity

**Files:**
- Modify: `src/app/features/rapports/pages/export/export.component.html`
- Modify: `src/app/features/rapports/pages/export/export.component.scss`
- Test: `src/app/features/rapports/pages/export/export.component.spec.ts`

**Interfaces:**
- Consumes: existing `returnToConfiguration()` and `startNewReport()` actions.
- Produces: secondary left action and primary right action matching Configuration.

- [ ] **Step 1: Add failing tests.** Assert « Modifier la configuration » is secondary and outlined, and « Créer un nouveau rapport » is primary rather than secondary. At mobile width, assert both rendered buttons fill the footer width and preserve DOM order.
- [ ] **Step 2: Run the focused export test and confirm failure.**

  ```powershell
  npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/export/export.component.spec.ts"
  ```

- [ ] **Step 3: Align button variants.** Remove `[text]="true"` from the first action and give it `severity="secondary"` plus `[outlined]="true"`. Remove `severity="secondary"` and `[outlined]="true"` from the second action so it uses the configured primary preset. Preserve labels, icons and click handlers.
- [ ] **Step 4: Align responsive sizing.** Keep the horizontal desktop footer. Under 47.5rem retain its column layout and set both `p-button` hosts and their generated `.p-button` elements to width 100%, matching the configuration action card’s clear secondary/primary hierarchy.
- [ ] **Step 5: Run the focused export test.** Expected: action variants, handlers and responsive sizing tests pass.

### Task 4: Final verification and review

**Files:**
- Update: this plan’s `Progress`, `Surprises & Discoveries` and `Outcomes & Retrospective` sections.
- Generated only if available: repository-root `graphify-out/` files.

**Interfaces:** None.

- [ ] **Step 1: Run all focused tests together.**

  ```powershell
  npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/shared/page-layout/*.spec.ts" --include="src/app/features/rapports/pages/configuration/configuration.component.spec.ts" --include="src/app/features/rapports/pages/export/export.component.spec.ts"
  ```

- [ ] **Step 2: Run the production build.**

  ```powershell
  npm.cmd run build
  ```

  Expected: exit 0. Report existing bundle/style budget warnings without changing budgets.

- [ ] **Step 3: Run the full frontend suite.**

  ```powershell
  npm.cmd test -- --watch=false --browsers=ChromeHeadless
  ```

  Compare with the currently documented baseline of 175 passing and 7 failing tests; do not silently fix failures outside this responsive task.

- [ ] **Step 4: Inspect mobile behavior at approximately 390 × 844 when browser control is available.** Verify drawer overlay/closure, active Indigo tab, preview-before-actions order and full-width export actions. Also verify desktop at 1440px retains its current layout.
- [ ] **Step 5: Review the final diff.** Run `git diff --check` and review correctness, accessibility, existing authentication/logout changes, mobile overflow, reduced motion and absence of duplicated actions.
- [ ] **Step 6: Run `graphify update .` from the repository root if available.** Keep generated changes separate in the report.

## Progress

- [x] 2026-09-07 — Repository, relevant components and existing responsive behavior inspected.
- [x] 2026-09-07 — Design approved in conversation and written specification approved.
- [x] 2026-09-07 — Executable plan drafted and self-reviewed.
- [x] 2026-09-07 — Direct execution with Ponytail selected.
- [x] 2026-09-07 — Tasks 1–3 implemented with the existing layout, PrimeNG buttons and CSS ordering.
- [x] 2026-09-07 — 35 directly affected shared-layout/export tests pass; configuration coverage was also exercised in the combined focused run.
- [x] 2026-09-07 — Production build passes with existing bundle/style budget warnings.
- [x] 2026-09-07 — Final diff reviewed and Graphify updated.

## Surprises & Discoveries

- The hamburger is currently a decorative span and the sidebar is `display: none` below 48rem.
- Configuration already has functional accessible tabs; only its hard-coded blue token and mobile block order need correction.
- Export’s first footer action is text-only while its second is secondary outlined, the reverse of the requested Configuration hierarchy.
- The working tree already contains uncommitted authentication, logout, chatbot, asset and Graphify changes; edits must remain confined to the listed overlapping files and preserve those changes.
- Angular templates reject inline arrow functions; the drawer signal therefore uses a small `toggleMobileSidebar()` method.
- Existing shared-layout desktop tests inspect mobile rules without a mobile viewport, so mobile label/item rules must be scoped to `.mobile-open`.
- The combined focused run reached 71 passing tests with one pre-existing Configuration assertion expecting « Suivant » while the current UI renders « Génerer ».
- The full suite was interrupted after 28/184 tests: unrelated HTTP specs emitted 403 responses that activated the global auth interceptor, causing six auth-spec failures and a Karma disconnect. The auth-only retry then hit a sandbox filesystem access error during Angular bundling.

## Decision Log

- 2026-09-07 — Reuse the sidebar DOM and local signal instead of adding PrimeNG Drawer or duplicating navigation.
- 2026-09-07 — Reorder existing blocks with mobile CSS and one state class instead of duplicating action buttons or changing desktop DOM order.
- 2026-09-07 — Use the configured primary token with a hex fallback rather than adding a new color.
- 2026-09-07 — Keep implementation inline and uncommitted unless the user explicitly selects another supported execution method or requests Git integration.

## Outcomes & Retrospective

Implemented the mobile drawer with backdrop and Escape/link closure, replaced the hard-coded mobile tab blue with the primary token, placed the visible Configuration/Preview panel before its actions on mobile, and aligned Export footer button hierarchy and sizing with Configuration.

Verification: 35/35 directly modified shared-layout/export specs pass; the combined focused run reached 71/72 with the documented unrelated « Suivant » assertion; `npm.cmd run build` passes. `git diff --check` passed before the final transition-only CSS adjustment, and final review found no remaining issue in the responsive diff. Graphify rebuilt successfully with 3055 nodes and 6190 edges. Manual browser inspection was skipped because the user declined it.

## Self-review

All approved requirements map to Tasks 1–3. No placeholders remain. Method, signal, class and selector names are consistent across tasks. Accessibility, reduced motion, desktop preservation, focused checks, full-suite baseline and dirty-worktree constraints are explicit.
