# Report Export Vertical Stepper Implementation Plan

> **Superseded:** the global `exportStage` design in this document was replaced by `2026-08-17-report-export-independent-formats-primeng.md` after validation of independent PDF/XLSX movement and PrimeNG-only components.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current export page layout with the validated horizontal report wizard and progressive vertical export stepper while preserving every existing API, polling, navigation, cleanup, and download behavior.

**Architecture:** Keep `ExportComponent` as the single page-level orchestrator. Add only computed presentation state and label helpers to the component, restructure its Angular template, and replace page-local SCSS; do not add services, child components, state management, or dependencies.

**Tech Stack:** Angular 20 standalone components, TypeScript 5.9, Signals, RxJS 7.8, PrimeNG 20, HTML, SCSS, Jasmine/Karma.

## Global Constraints

- Preserve existing REST contracts, models, polling intervals, cleanup, retries, Blob download, and routes.
- Keep PDF and XLSX exports independent.
- Reuse the existing Inter/system typography, Material Symbols, PrimeNG Button, and PrimeNG ProgressBar.
- Add no facade, store, service, mapper, UI library, or generic stepper abstraction.
- Keep the top wizard labels visible: `Source de données`, `Configuration`, and `Export`.
- Display PDF and Excel horizontally in the active download step on desktop and stack them on narrow mobile.

---

### Task 1: Derived export workflow state

**Files:**
- Modify: `src/app/features/rapports/pages/export/export.component.ts`
- Test: `src/app/features/rapports/pages/export/export.component.spec.ts`

**Interfaces:**
- Consumes: existing `generation`, `pdfExport`, and `xlsxExport` signals.
- Produces: `exportStage`, `readyExportCount`, `preparationSummary`, `formatsSummary`, `generationPhaseLabel()`, and `exportStatusLabel()` for the template.

- [ ] **Step 1: Write failing tests for stage transitions**

Add Jasmine cases which set the existing signals directly and assert:

```typescript
component.generation.set({ ...generation, status: 'RUNNING', phase: 'READING_ROWS', progress: 71 });
expect(component.exportStage()).toBe('preparation');

component.generation.set(generation);
expect(component.exportStage()).toBe('formats');

component.pdfExport.set({
  exportId: 'PDF-id', generationId: generation.generationId, format: 'PDF',
  status: 'READY', progress: 100, errorCode: null,
});
expect(component.exportStage()).toBe('download');
```

- [ ] **Step 2: Run the focused spec and verify it fails**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/features/rapports/pages/export/export.component.spec.ts
```

Expected: compilation failure because `exportStage` does not exist.

- [ ] **Step 3: Add minimal computed presentation state**

Import `computed` from `@angular/core` and add:

```typescript
export type ExportStage = 'preparation' | 'formats' | 'download';

readonly readyExportCount = computed(() =>
  Number(this.pdfExport()?.status === 'READY') + Number(this.xlsxExport()?.status === 'READY'),
);

readonly exportStage = computed<ExportStage>(() => {
  if (this.generation()?.status !== 'READY') return 'preparation';
  return this.readyExportCount() > 0 ? 'download' : 'formats';
});
```

Add explicit label helpers that translate existing technical phases/statuses to French UI copy. Do not alter the underlying values.

- [ ] **Step 4: Run the focused spec and verify it passes**

Run the command from Step 2. Expected: PASS.

### Task 2: Semantic wizard and progressive timeline

**Files:**
- Modify: `src/app/features/rapports/pages/export/export.component.html`
- Modify: `src/app/features/rapports/pages/export/export.component.scss`
- Test: `src/app/features/rapports/pages/export/export.component.spec.ts`

**Interfaces:**
- Consumes: the computed presentation state from Task 1 and existing `createExport()`, `download()`, `previous()`, and `newReport()` methods.
- Produces: accessible horizontal wizard, vertical timeline, and responsive format actions.

- [ ] **Step 1: Write failing DOM assertions**

After `fixture.detectChanges()`, assert:

```typescript
const text = fixture.nativeElement.textContent as string;
expect(text).toContain('Source de données');
expect(text).toContain('Configuration');
expect(text).toContain('Export');
expect(text).toContain('Préparation');
expect(text).toContain('Formats');
expect(text).toContain('Téléchargement');
expect(fixture.nativeElement.querySelector('[aria-current="step"]')).not.toBeNull();
```

When PDF and XLSX are ready, assert two `.export-format` elements and the two download labels.

- [ ] **Step 2: Run the focused spec and verify it fails**

Run the command from Task 1, Step 2. Expected: missing wizard/timeline assertions fail.

- [ ] **Step 3: Replace the template structure**

Implement:

```text
header
main
  heading + horizontal application wizard
  network/error feedback
  ordered vertical workflow
    preparation summary/detail
    formats summary/detail
    download summary/detail
      horizontal PDF card
      horizontal Excel card
  normal-flow footer actions
```

Use `aria-current="step"` only on the active local step. Keep each PrimeNG progress bar explicitly labeled with `role="progressbar"` and `aria-label` on its host.

- [ ] **Step 4: Replace page-local SCSS**

Use the validated tokens and layout:

```scss
$page: #f7f9fb;
$surface: #ffffff;
$text: #0f172a;
$muted: #64748b;
$primary: #2563eb;
$success: #18794e;
```

Desktop uses a two-column `.export-format-grid`; below 640px it becomes one column. Keep wizard labels visible on mobile below their markers. Remove the fixed bottom action bar and its compensating page padding.

- [ ] **Step 5: Run the focused spec and verify it passes**

Run the command from Task 1, Step 2. Expected: PASS.

### Task 3: Error states and regression verification

**Files:**
- Modify: `src/app/features/rapports/pages/export/export.component.spec.ts`

**Interfaces:**
- Consumes: final component/template behavior.
- Produces: regression coverage and verified production build.

- [ ] **Step 1: Add tests for independent format states**

Cover one ready export plus one missing/running export. Assert that the ready format downloads while the other remains independently creatable or displays progress.

- [ ] **Step 2: Add a preparation failure rendering test**

Set generation to `FAILED`, detect changes, and assert the page stays on Preparation and renders `Revenir et réessayer`.

- [ ] **Step 3: Run the complete test suite**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless
```

Expected: all specs PASS.

- [ ] **Step 4: Run the production build**

Run:

```powershell
npm.cmd run build
```

Expected: Angular build completes without template, type, or SCSS errors.

- [ ] **Step 5: Inspect the final diff**

Confirm no API, service, model, route, polling, or dependency file changed as part of this UI implementation.
