# Report Builder Data Administration Figma Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. The Figma operations are sequential because each task consumes node IDs created by the previous task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create and visually verify a high-fidelity Figma mockup for administering Report Builder datasets and field visibility without changing Angular or Spring code.

**Architecture:** Build one Figma Design file incrementally. Discover available Figma libraries first, establish a small local component set that mirrors the existing PrimeNG Aura/indigo UI, then compose five required desktop states from component instances and document the narrow-screen behavior.

**Tech Stack:** Figma Design and Plugin API, Auto Layout, component variants, Inter, PrimeNG Aura visual language, PrimeIcons 8, Angular 20/PrimeNG 20 implementation constraints.

## Global Constraints

- Do not create or modify Angular, Spring, SQL, DTO, endpoint or migration code.
- Follow `docs/superpowers/specs/2026-08-20-report-builder-data-admin-figma-design.md` exactly.
- Preserve the code rule `dataset active && visible` and `field active && visible && dataset active && visible`.
- Use PrimeIcons 8 exclusively; do not use Material Symbols.
- Use Inter and the existing Aura/indigo, slate-surface visual language.
- Use Auto Layout for every structural relationship.
- Use component instances and named variants for repeated UI.
- Keep `active`, `visible` and `field.visible` distinct and use non-destructive French wording.
- Validate every major section visually before building on it.
- Return and retain every created or mutated Figma node ID between operations.

---

### Task 1: Create and inspect the target Figma file

**Files:**
- Read: `docs/superpowers/specs/2026-08-20-report-builder-data-admin-figma-design.md`
- Read: `Frontend/Rhis_report_gen/src/app/app.config.ts`
- Read: `Frontend/Rhis_report_gen/src/styles.scss`
- Create remotely: Figma Design file `RHIS — Administration des données Report Builder`

**Interfaces:**
- Consumes: validated specification, authenticated Figma plan, app font/theme information.
- Produces: `fileKey`, target page ID, existing library/component/variable/style inventory.

- [ ] **Step 1: Resolve the authenticated Figma plan**

Call Figma `whoami`. If exactly one plan is available, use its key. If several are available, stop and ask the user which team or organization should own the file.

- [ ] **Step 2: Create the Figma Design file**

Load the `figma-create-new-file` skill, then create a Design file named `RHIS — Administration des données Report Builder` in the selected plan’s drafts unless the user supplied a project.

- [ ] **Step 3: Inspect the empty file and available libraries**

Read all pages and top-level nodes, local variable collections, available fonts, linked libraries and available design-system libraries. Record that existing-screen inspection is not applicable when the new file is empty.

- [ ] **Step 4: Verify source-side component discovery**

Confirm that the codebase contains no `*.figma.ts`, `*.figma.tsx` or `*.figma.js` Code Connect files for the required components. Record the needed primitives: button, search input, filter control, toggle, checkbox, badges, message, confirmation and list rows.

### Task 2: Establish visual foundations and the Components area

**Files:**
- No local files created or modified.
- Create remotely: Figma page or section `Components`.

**Interfaces:**
- Consumes: Task 1 `fileKey`, library inventory and verified Inter font styles.
- Produces: IDs or keys for local variables/styles and component sets used by Tasks 3–6.

- [ ] **Step 1: Resolve reusable library assets**

Search available libraries only after Code Connect and existing-screen discovery are complete. Prefer usable linked components and variables. If no coherent Aura-compatible library exists, create a minimal local foundation derived from the existing product styles rather than mixing unrelated kits.

- [ ] **Step 2: Create semantic foundations when required**

Create only the semantic values used by the mockup: page/surface/subtle backgrounds, primary indigo, neutral text levels, border, success, warning, danger, focus, radii and spacing. Give them semantic names and explicit scopes.

- [ ] **Step 3: Create typography and verify Inter**

Use verified Inter font styles for page title, section title, body, label, caption and technical name. Load every used font before creating text and read the font family back after creation.

- [ ] **Step 4: Create component variants**

Create reusable components for `DatasetStatusBadge`, `DatasetListItem`, `SearchInput`, `DatasetFilterControl`, `FieldVisibilityRow`, `UnsavedChangesBar`, `EmptyState` and `SaveFeedback`. Add variants only for states used in the specification.

- [ ] **Step 5: Add descriptions and inspect the component tree**

Give each main component a concise purpose/usage description. Verify names, variants, component properties, Auto Layout, touch-target sizing and absence of Material Symbols.

- [ ] **Step 6: Screenshot the Components area**

Check that variants are legible, evenly spaced and not clipped. Fix any issue before composing screens.

### Task 3: Compose `01 — Dataset active`

**Files:**
- No local files created or modified.
- Create remotely: Figma frame `01 — Dataset active`.

**Interfaces:**
- Consumes: Task 2 component IDs/keys, variables and text styles.
- Produces: wrapper frame ID and section IDs for header, dataset list, settings panel, field list and action area.

- [ ] **Step 1: Create the 1440 px wrapper and skeleton**

Create the wrapper away from existing content. Add placeholder Auto Layout sections for the application header, page heading, 320 px dataset master panel, flexible detail panel and sticky save region.

- [ ] **Step 2: Build the administration header**

Use `ReportGen Pro`, `Administration`, breadcrumb `Administration > Données du Report Builder`, title `Configuration des données` and the approved description. Use PrimeIcons `pi-cog` where an administration icon is useful.

- [ ] **Step 3: Build the dataset master panel**

Add search, filters `Toutes`, `Actives`, `Inactives`, `Masquées`, and realistic rows for Employee, Contract, Work Time, Department and Restaurant. Select Employee and show text badges plus visible-field counts.

- [ ] **Step 4: Build the active dataset settings**

Show Employee / `rhis_employee`, status `Actif`, `Table active` on, `Visible pour les utilisateurs` on, the non-destructive help copy, and `18 champs visibles sur 25`.

- [ ] **Step 5: Build the searchable field list**

Show realistic rows with French labels, technical names and types, including visible and hidden fields. Use checkbox/toggle states with explicit labels and PrimeIcons only for secondary affordances.

- [ ] **Step 6: Validate the complete frame**

Screenshot the full frame and individual master/detail sections. Check text clipping, contrast, selected state, consistent spacing, readable technical names and 44 px interaction targets.

### Task 4: Compose inactive, search and unsaved states

**Files:**
- No local files created or modified.
- Create remotely: frames `02 — Dataset inactive`, `03 — Search & filters`, `04 — Unsaved changes`.

**Interfaces:**
- Consumes: Task 3 frame structure and all Task 2 components.
- Produces: three verified frame IDs and any required new component variants.

- [ ] **Step 1: Create `02 — Dataset inactive` from instances**

Reuse the active-frame structure. Select Work Time, show status `Inactif`, the message `Cette table n’est pas disponible dans le Report Builder.`, retained visibility values and readable read-only field rows.

- [ ] **Step 2: Verify the inactive treatment**

Ensure read-only content remains legible, controls are unmistakably unavailable, configuration appears preserved and no visual treatment implies SQL deletion.

- [ ] **Step 3: Create `03 — Search & filters`**

Show search value `contr`, filter `Masquées`, a narrowed Contract result and an adjacent or component-level empty-result variant with `Réinitialiser les filtres`.

- [ ] **Step 4: Create `04 — Unsaved changes`**

Show three property changes distributed across multiple datasets, `Modifié` indicators in the master list and a sticky bar reading `3 modifications non sauvegardées` with `Annuler` and `Sauvegarder`.

- [ ] **Step 5: Validate all three frames**

Screenshot each frame and its state-specific section. Verify that status meaning does not depend only on color and that the sticky bar remains prominent without covering content.

### Task 5: Compose save feedback variants

**Files:**
- No local files created or modified.
- Create remotely: frame or variants section `05 — Save feedback`.

**Interfaces:**
- Consumes: Task 2 `SaveFeedback` and `UnsavedChangesBar` component sets.
- Produces: verified `Saving`, `Saved` and `Error` variants.

- [ ] **Step 1: Build `Saving`**

Show a disabled duplicate-safe save action, PrimeIcon spinner and `Sauvegarde en cours…` status.

- [ ] **Step 2: Build `Saved`**

Show a restrained success state `Modifications sauvegardées`, PrimeIcon check and no remaining unsaved count.

- [ ] **Step 3: Build `Error`**

Show `Échec de la sauvegarde. Vos modifications sont conservées.`, PrimeIcon exclamation, retained unsaved count and `Réessayer` action.

- [ ] **Step 4: Validate feedback accessibility**

Check text/icon redundancy, contrast, consistent dimensions between variants and plausible status announcement semantics.

### Task 6: Document responsive behavior and perform final QA

**Files:**
- No local files created or modified.
- Create remotely: compact responsive behavior annotation or frame near the main screens.

**Interfaces:**
- Consumes: all frame and component IDs from Tasks 2–5.
- Produces: final Figma URL/node references and a completed QA report.

- [ ] **Step 1: Add the narrow-screen behavior**

Show or annotate the sequential pattern: table list first, selection opens a full-width detail view, `Retour aux tables` uses `pi-arrow-left`, and the save bar wraps without hiding actions.

- [ ] **Step 2: Inspect the full layer hierarchy**

Verify top-level ordering, semantic layer names, component instances, variant names, Auto Layout usage and the absence of orphaned placeholders.

- [ ] **Step 3: Assert typography and iconography**

Read back all text font families and confirm Inter. Inspect all icon layers and confirm that every icon is a PrimeIcon or an approved PrimeIcons-derived SVG; confirm zero Material Symbols.

- [ ] **Step 4: Perform visual QA**

Capture the Components area and all five required frames. Check clipping, overlap, inconsistent spacing, unreadable disabled states, weak focus treatment, status reliance on color and accidental destructive wording.

- [ ] **Step 5: Apply targeted corrections**

Modify only defective nodes or variants, then repeat the affected screenshots and metadata checks until all acceptance criteria pass.

- [ ] **Step 6: Deliver the Figma file**

Return the editable Figma URL, identify the five required frames and summarize any implementation gap intentionally left outside the mockup, especially the current `active` synchronization conflict and missing admin API contract.
