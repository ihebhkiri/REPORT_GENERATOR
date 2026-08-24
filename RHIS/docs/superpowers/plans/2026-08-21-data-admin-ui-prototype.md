# Data Administration UI Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone Angular administration page backed only by local mock data, presenting table `active`, `displayMain`, `displayRelated` and field `active`/`visible` exactly as defined by the validated UI/UX specification.

**Architecture:** Add one lazy-loaded standalone page under a dedicated `data-admin` feature. Keep synchronous UI state in Signals, derive filtering/selection/change counts with `computed()`, and keep mock data in a plain local file; do not add a service, store, facade or backend contract. Use PrimeNG 20 primitives already installed and scope the monochrome `DESIGN.md` overrides to this page so existing indigo screens are unchanged.

**Tech Stack:** Angular 20.3, TypeScript 5.9, Signals, PrimeNG 20.4, PrimeIcons 8, HTML, SCSS, Jasmine/Karma.

## Global Constraints

- Source specification: `RHIS/docs/superpowers/specs/2026-08-21-database-tables-fields-admin-ui-design.md`.
- Frontend working directory: `C:/Users/Surface Pro/Downloads/RHIS/Frontend/Rhis_report_gen`.
- Use mock data only; add no HTTP request, API contract, database behavior or persistence.
- Table `active` and field `active` are technical read-only statuses.
- Only table `displayMain`, table `displayRelated` and field `visible` are editable.
- Preserve the current header pattern and add no sidebar or Figma artifact.
- Use standalone Angular components, Signals and `computed()`; do not use RxJS for synchronous mock state.
- Use PrimeNG as the UI library and PrimeIcons for icons; add no dependency and no Material Symbol.
- Follow `DESIGN.md`: monochrome palette, Geist/Inter, 24 px container radius, 18 px interactive radius, 4 px spacing grid and compact density.
- Do not modify the global Aura/indigo preset; overrides must remain scoped to the data administration page.
- Do not add bulk actions, virtual scrolling, a service, store, facade, mapper or generic component wrapper.
- Use visible labels and semantic HTML. Do not introduce a custom ARIA contract in this UI prototype.

---

## File Structure

Create or modify only these frontend files:

```text
src/app/app.routes.ts
src/app/app.routes.spec.ts
src/app/features/data-admin/models/data-admin.model.ts
src/app/features/data-admin/data/data-admin.mock-data.ts
src/app/features/data-admin/data/data-admin.mock-data.spec.ts
src/app/features/data-admin/pages/data-configuration/
  data-configuration.component.ts
  data-configuration.component.html
  data-configuration.component.scss
  data-configuration.component.spec.ts
```

`data-admin.model.ts` owns the feature-local types. `data-admin.mock-data.ts` owns immutable fixtures. The page component owns orchestration, filtering, draft changes and mock save feedback. No child component is justified because the master and detail areas share one small state boundary and are not reused elsewhere.

---

### Task 1: Define the administration model and realistic fixtures

**Files:**
- Create: `src/app/features/data-admin/models/data-admin.model.ts`
- Create: `src/app/features/data-admin/data/data-admin.mock-data.ts`
- Test: `src/app/features/data-admin/data/data-admin.mock-data.spec.ts`

**Interfaces:**
- Produces: `AdminDataTable`, `AdminDataField`, `AvailabilityFilter`, `DisplayFilter`, `SaveStatus` and `MOCK_ADMIN_TABLES`.
- Consumers: `DataConfigurationComponent` and its tests in Tasks 2–4.

- [ ] **Step 1: Write the fixture contract test**

```ts
import { MOCK_ADMIN_TABLES } from './data-admin.mock-data';

describe('MOCK_ADMIN_TABLES', () => {
  it('covers all table display combinations and technical states', () => {
    const states = MOCK_ADMIN_TABLES.map(
      (table) => `${table.active}:${table.displayMain}:${table.displayRelated}`,
    );

    expect(states).toContain('true:true:true');
    expect(states).toContain('true:true:false');
    expect(states).toContain('true:false:true');
    expect(states).toContain('true:false:false');
    expect(states.some((state) => state.startsWith('false:'))).toBeTrue();
  });

  it('provides mixed technical and visibility states for employee fields', () => {
    const employees = MOCK_ADMIN_TABLES.find((table) => table.sourceName === 'employees');

    expect(employees).toBeDefined();
    expect(employees?.fields.some((field) => field.active && field.visible)).toBeTrue();
    expect(employees?.fields.some((field) => field.active && !field.visible)).toBeTrue();
    expect(employees?.fields.some((field) => !field.active)).toBeTrue();
  });
});
```

- [ ] **Step 2: Run the focused test and verify the missing module failure**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/features/data-admin/data/data-admin.mock-data.spec.ts
```

Expected: FAIL because `data-admin.mock-data.ts` does not exist.

- [ ] **Step 3: Add the exact feature-local types**

Create `data-admin.model.ts`:

```ts
export type AvailabilityFilter = 'all' | 'active' | 'inactive';
export type DisplayFilter = 'all' | 'shown' | 'hidden';
export type SaveStatus = 'idle' | 'saving' | 'saved' | 'error';

export interface AdminDataField {
  readonly id: number;
  readonly displayName: string;
  readonly sourceName: string;
  readonly dataType: string;
  readonly active: boolean;
  readonly visible: boolean;
}

export interface AdminDataTable {
  readonly id: number;
  readonly displayName: string;
  readonly sourceName: string;
  readonly active: boolean;
  readonly displayMain: boolean;
  readonly displayRelated: boolean;
  readonly fields: readonly AdminDataField[];
}
```

- [ ] **Step 4: Add the exact table matrix and field fixtures**

Create `data-admin.mock-data.ts` with `employees` fields `id`, `employee_number`, `first_name`, `last_name`, `email`, `department_id`, `contract_id`, `internal_code`, `legacy_code`, `created_at` and `updated_at`. Set `internal_code.visible=false`, `legacy_code.active=false`, and keep at least one active hidden temporal field.

The exported table list must use this exact state matrix:

```ts
export const MOCK_ADMIN_TABLES: readonly AdminDataTable[] = [
  table(1, 'Employees', 'employees', true, true, true, employeeFields),
  table(2, 'Contracts', 'contracts', true, true, true, contractFields),
  table(3, 'Departments', 'departments', true, false, true, departmentFields),
  table(4, 'Work hours', 'work_hours', false, true, true, workHourFields),
  table(5, 'Payroll entries', 'payroll_entries', true, true, false, payrollFields),
  table(6, 'Users', 'users', true, false, false, userFields),
  table(7, 'Roles', 'roles', true, false, true, roleFields),
  table(8, 'Audit log', 'audit_log', false, false, false, auditFields),
];
```

Use small private `field(...)` and `table(...)` fixture factories returning the readonly interfaces. They remove repetitive fixture syntax only; they contain no runtime behavior.

- [ ] **Step 5: Run the fixture tests**

Run the focused command from Step 2. Expected: PASS.

- [ ] **Step 6: Commit the model and fixtures**

```powershell
git add src/app/features/data-admin/models/data-admin.model.ts src/app/features/data-admin/data/data-admin.mock-data.ts src/app/features/data-admin/data/data-admin.mock-data.spec.ts
git commit -m "feat: add data administration mock model"
```

---

### Task 2: Implement synchronous page state and draft behavior

**Files:**
- Create: `src/app/features/data-admin/pages/data-configuration/data-configuration.component.ts`
- Create: `src/app/features/data-admin/pages/data-configuration/data-configuration.component.html`
- Create: `src/app/features/data-admin/pages/data-configuration/data-configuration.component.scss`
- Test: `src/app/features/data-admin/pages/data-configuration/data-configuration.component.spec.ts`

**Interfaces:**
- Consumes: `AdminDataTable`, `AvailabilityFilter`, `DisplayFilter`, `SaveStatus`, `MOCK_ADMIN_TABLES`.
- Produces: standalone `DataConfigurationComponent` and public Signal-based state used by its template.

- [ ] **Step 1: Write failing state tests**

Create the component spec with `provideNoopAnimations()` and these tests:

```ts
it('selects employees and exposes its fields by default', () => {
  expect(component.selectedTable()?.sourceName).toBe('employees');
  expect(component.filteredFields().length).toBeGreaterThan(0);
});

it('combines availability, main and related filters', () => {
  component.availabilityFilter.set('active');
  component.mainFilter.set('hidden');
  component.relatedFilter.set('shown');

  expect(component.filteredTables().map((table) => table.sourceName)).toEqual([
    'departments',
    'roles',
  ]);
});

it('keeps the selected detail when filters hide its row', () => {
  component.selectTable(1);
  component.mainFilter.set('hidden');

  expect(component.filteredTables().some((table) => table.id === 1)).toBeFalse();
  expect(component.selectedTable()?.id).toBe(1);
});

it('counts only editable display and visibility changes', () => {
  component.setDisplayMain(false);
  component.setDisplayRelated(false);
  component.setFieldVisible(1, false);

  expect(component.changeCount()).toBe(3);
  expect(component.modifiedTableIds().has(1)).toBeTrue();
});

it('does not modify an inactive table or inactive field', () => {
  component.selectTable(4);
  component.setDisplayMain(false);
  expect(component.selectedTable()?.displayMain).toBeTrue();

  component.selectTable(1);
  const legacyCode = component.selectedTable()?.fields.find(
    (field) => field.sourceName === 'legacy_code',
  );
  component.setFieldVisible(legacyCode!.id, true);
  expect(component.selectedTable()?.fields.find((field) => field.id === legacyCode!.id)?.visible)
    .toBeFalse();
});
```

- [ ] **Step 2: Add the minimal component shell and verify RED**

Create the standalone component with empty external HTML/SCSS files and the imports below, but without the tested methods:

```ts
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-data-configuration',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    ButtonModule,
    CheckboxModule,
    InputTextModule,
    PopoverModule,
    SkeletonModule,
    TooltipModule,
  ],
  templateUrl: './data-configuration.component.html',
  styleUrl: './data-configuration.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataConfigurationComponent {}
```

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/features/data-admin/pages/data-configuration/data-configuration.component.spec.ts
```

Expected: FAIL on missing Signals and methods.

- [ ] **Step 3: Implement the Signal state contract**

Add these public properties and methods with the exact names:

```ts
readonly tables = signal<readonly AdminDataTable[]>(cloneTables(MOCK_ADMIN_TABLES));
private readonly savedTables = signal<readonly AdminDataTable[]>(cloneTables(MOCK_ADMIN_TABLES));

readonly selectedTableId = signal<number | null>(1);
readonly tableSearch = signal('');
readonly fieldSearch = signal('');
readonly availabilityFilter = signal<AvailabilityFilter>('all');
readonly mainFilter = signal<DisplayFilter>('all');
readonly relatedFilter = signal<DisplayFilter>('all');
readonly saveStatus = signal<SaveStatus>('idle');
readonly isLoading = signal(false);
readonly mobileDetailOpen = signal(false);

readonly filteredTables = computed(() => this.filterTables());
readonly selectedTable = computed(() =>
  this.tables().find((table) => table.id === this.selectedTableId()) ?? null,
);
readonly filteredFields = computed(() => this.filterFields());
readonly changeCount = computed(() => this.countChanges());
readonly modifiedTableIds = computed(() => this.findModifiedTableIds());

selectTable(tableId: number): void;
showTableList(): void;
updateTableSearch(event: Event): void;
updateFieldSearch(event: Event): void;
setAvailabilityFilter(filter: AvailabilityFilter): void;
setMainFilter(filter: DisplayFilter): void;
setRelatedFilter(filter: DisplayFilter): void;
resetFilters(): void;
setDisplayMain(displayMain: boolean): void;
setDisplayRelated(displayRelated: boolean): void;
setFieldVisible(fieldId: number, visible: boolean): void;
discardChanges(): void;
saveChanges(): void;
```

All updates must use immutable `signal.update()` transformations. `setDisplayMain` and `setDisplayRelated` return without changes when the selected table is missing or inactive. `setFieldVisible` returns without changes when the table or field is inactive.

Use one private `normalize(value: string): string` helper with `trim().toLocaleLowerCase('fr')`. Do not add RxJS, `effect()`, `Subject`, debounce or a service.

Clone fixtures and saved baselines with this feature-local function in the component file:

```ts
function cloneTables(tables: readonly AdminDataTable[]): readonly AdminDataTable[] {
  return tables.map((table) => ({
    ...table,
    fields: table.fields.map((field) => ({ ...field })),
  }));
}
```

Filtering must use these exact predicates after normalizing the search value:

```ts
private filterTables(): readonly AdminDataTable[] {
  const query = this.normalize(this.tableSearch());
  return this.tables().filter((table) =>
    (!query || this.normalize(`${table.displayName} ${table.sourceName}`).includes(query)) &&
    (this.availabilityFilter() === 'all' ||
      table.active === (this.availabilityFilter() === 'active')) &&
    (this.mainFilter() === 'all' ||
      table.displayMain === (this.mainFilter() === 'shown')) &&
    (this.relatedFilter() === 'all' ||
      table.displayRelated === (this.relatedFilter() === 'shown'))
  );
}

private filterFields(): readonly AdminDataField[] {
  const query = this.normalize(this.fieldSearch());
  return (this.selectedTable()?.fields ?? []).filter(
    (field) =>
      !query ||
      this.normalize(`${field.displayName} ${field.sourceName} ${field.dataType}`).includes(query),
  );
}
```

- [ ] **Step 4: Implement comparison against the saved baseline**

`countChanges()` counts only:

```ts
Number(current.displayMain !== saved.displayMain) +
Number(current.displayRelated !== saved.displayRelated) +
current.fields.filter(
  (field) => field.visible !== saved.fields.find((savedField) => savedField.id === field.id)?.visible,
).length
```

`findModifiedTableIds()` returns a new `Set<number>` containing tables with at least one counted change. Never compare or mutate `active`.

- [ ] **Step 5: Run the state tests**

Run the focused component test command. Expected: PASS.

- [ ] **Step 6: Commit the page state**

```powershell
git add src/app/features/data-admin/pages/data-configuration
git commit -m "feat: add data administration page state"
```

---

### Task 3: Build the desktop master-detail interface

**Files:**
- Modify: `src/app/features/data-admin/pages/data-configuration/data-configuration.component.html`
- Modify: `src/app/features/data-admin/pages/data-configuration/data-configuration.component.scss`
- Test: `src/app/features/data-admin/pages/data-configuration/data-configuration.component.spec.ts`

**Interfaces:**
- Consumes: all public Signals and mutation methods from Task 2.
- Produces: desktop page, filter popover, table rows, detail controls and field rows.

- [ ] **Step 1: Add failing DOM tests for the three table dimensions**

```ts
it('renders the three table dimensions without editable active controls', () => {
  const page = fixture.nativeElement as HTMLElement;

  expect(page.textContent).toContain('Disponibilité technique');
  expect(page.textContent).toContain('Afficher comme source principale');
  expect(page.textContent).toContain('Afficher dans les tables liées');
  expect(page.querySelector('[data-control="table-active"]')).toBeNull();
  expect(page.querySelectorAll('[data-control="table-display"]').length).toBe(2);
});

it('renders field active as text and visible as the only field control', () => {
  const firstRow = (fixture.nativeElement as HTMLElement).querySelector('.field-row');

  expect(firstRow?.textContent).toContain('Active');
  expect(firstRow?.querySelectorAll('p-checkbox').length).toBe(1);
});

it('shows a non-color-only selected table state', () => {
  const selected = (fixture.nativeElement as HTMLElement).querySelector('.table-row--selected');

  expect(selected?.querySelector('.table-row__selection-marker')).not.toBeNull();
  expect(selected?.querySelector('.pi-chevron-right')).not.toBeNull();
});
```

- [ ] **Step 2: Build the semantic page and header**

Use a `header`, `main`, `nav`, `section`, `button`, `label` and list elements. Keep the existing `ReportGen Pro` header pattern and add the breadcrumb and approved copy. Use only PrimeIcons such as `pi-database`, `pi-search`, `pi-filter`, `pi-check-circle`, `pi-ban`, `pi-eye`, `pi-eye-slash` and `pi-chevron-right`.

- [ ] **Step 3: Build the master panel and filter popover**

Use `pInputText` for search, `p-button` to toggle `p-popover`, native button groups inside the popover for the three independent filters, and removable summary chips below the toolbar. Render rows with `@for (table of filteredTables(); track table.id)`.

Each row must include these stable hooks used by tests and QA:

```html
<button
  type="button"
  class="table-row"
  [class.table-row--selected]="selectedTableId() === table.id"
  [class.table-row--inactive]="!table.active"
  (click)="selectTable(table.id)"
>
  <span class="table-row__selection-marker"></span>
  <span class="table-row__identity">...</span>
  <span class="table-status">{{ table.active ? 'Active' : 'Inactive' }}</span>
  <span class="table-status">Main {{ table.displayMain ? 'shown' : 'hidden' }}</span>
  <span class="table-status">Related {{ table.displayRelated ? 'shown' : 'hidden' }}</span>
  <i class="pi pi-chevron-right"></i>
</button>
```

Do not place a checkbox in a table row.

- [ ] **Step 4: Build the detail settings and field list**

Render the technical availability as text. Use exactly two table checkboxes with `data-control="table-display"`, visible labels and `inputId`. Disable both when `!selectedTable().active`.

Render fields with:

```html
@for (field of filteredFields(); track field.id) {
  <div class="field-row" [class.field-row--inactive]="!field.active">
    <div class="field-row__identity">
      <strong>{{ field.displayName }}</strong>
      <span>{{ field.sourceName }} · {{ field.dataType }}</span>
    </div>
    <span class="field-row__status">
      <i [class]="field.active ? 'pi pi-check-circle' : 'pi pi-ban'"></i>
      {{ field.active ? 'Active' : 'Inactive' }}
    </span>
    <label [for]="'field-visible-' + field.id">Visible</label>
    <p-checkbox
      [inputId]="'field-visible-' + field.id"
      [binary]="true"
      [ngModel]="field.visible"
      [disabled]="!selectedTable()!.active || !field.active"
      (onChange)="setFieldVisible(field.id, $event.checked)"
    />
  </div>
}
```

Import `FormsModule` and use one-way `[ngModel]` with `(onChange)` for the PrimeNG binary checkbox. Do not create a Reactive Form for these simple independent booleans.

- [ ] **Step 5: Apply scoped DESIGN.md tokens and density**

Define local custom properties on `:host` or `.data-admin-page`:

```scss
:host {
  --admin-canvas: #f5f5f5;
  --admin-paper: #ffffff;
  --admin-surface-alt: #fafafa;
  --admin-ink: #0a0a0a;
  --admin-ink-soft: #171717;
  --admin-muted: #737373;
  --admin-hairline: #e5e5e5;
  --admin-error: #e7000b;
  display: block;
  min-height: 100%;
}
```

Use a 1280 px page maximum, 340 px master column, 24 px outer radius, 18 px control radius, 56 px field rows, 1 px hairlines and the subtle shadow copied from `DESIGN.md`. Override inherited PrimeNG primary/focus variables only inside `.data-admin-page`; do not edit `app.config.ts`.

- [ ] **Step 6: Run focused tests and production build**

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/features/data-admin/pages/data-configuration/data-configuration.component.spec.ts
npm.cmd run build
```

Expected: both commands succeed and the component SCSS remains below the configured 8 kB error budget.

- [ ] **Step 7: Commit the desktop interface**

```powershell
git add src/app/features/data-admin/pages/data-configuration
git commit -m "feat: build data administration master detail UI"
```

---

### Task 4: Complete empty, loading, save feedback and responsive states

**Files:**
- Modify: `src/app/features/data-admin/pages/data-configuration/data-configuration.component.ts`
- Modify: `src/app/features/data-admin/pages/data-configuration/data-configuration.component.html`
- Modify: `src/app/features/data-admin/pages/data-configuration/data-configuration.component.scss`
- Test: `src/app/features/data-admin/pages/data-configuration/data-configuration.component.spec.ts`

**Interfaces:**
- Consumes: Task 2 draft state and Task 3 DOM.
- Produces: all approved visual states and sequential layout below 1024 px.

- [ ] **Step 1: Add failing tests for empty, locked and save states**

```ts
it('shows distinct empty states for tables and field search', () => {
  component.tables.set([]);
  fixture.detectChanges();
  expect(fixture.nativeElement.textContent).toContain('Aucune table disponible');

  component.tables.set(structuredClone(MOCK_ADMIN_TABLES));
  component.fieldSearch.set('does-not-exist');
  fixture.detectChanges();
  expect(fixture.nativeElement.textContent).toContain(
    'Aucun champ ne correspond à votre recherche.',
  );
});

it('keeps inactive table settings readable and disabled', () => {
  component.selectTable(4);
  fixture.detectChanges();

  expect(fixture.nativeElement.textContent).toContain(
    'Cette table n’est plus détectée dans la base.',
  );
  expect(
    Array.from(fixture.nativeElement.querySelectorAll('[data-control="table-display"]'))
      .every(
        (control: Element) =>
          (control.querySelector('input') as HTMLInputElement | null)?.disabled === true,
      ),
  ).toBeTrue();
});

it('discards local changes and restores the saved baseline', () => {
  component.setDisplayMain(false);
  component.discardChanges();

  expect(component.changeCount()).toBe(0);
  expect(component.selectedTable()?.displayMain).toBeTrue();
});

it('shows saving then saved feedback without persistence', fakeAsync(() => {
  component.setDisplayMain(false);
  component.saveChanges();
  expect(component.saveStatus()).toBe('saving');

  tick(600);
  expect(component.saveStatus()).toBe('saved');
  expect(component.changeCount()).toBe(0);
}));

it('keeps the mock error feedback renderable without a fake backend', () => {
  component.saveStatus.set('error');
  fixture.detectChanges();

  expect(fixture.nativeElement.textContent).toContain(
    'Échec de la sauvegarde. Vos modifications sont conservées.',
  );
});
```

- [ ] **Step 2: Implement deterministic mock save feedback**

Use one 600 ms `setTimeout` to make the loading state visible. Store its handle, clear it through `DestroyRef.onDestroy`, clone the current tables into `savedTables`, and set `saveStatus='saved'`. Keep `saveStatus='error'` renderable for component tests and visual inspection, but add no random failure or fake backend.

- [ ] **Step 3: Render every approved state**

Use `@if` branches for:

- skeleton table and field rows when `isLoading()`;
- no tables;
- no filtered table results with `Réinitialiser les filtres`;
- no selected table;
- no fields;
- no filtered field results with `Effacer la recherche`;
- inactive table message;
- sticky unsaved bar;
- `saving`, `saved` and `error` feedback.

Reserve bottom space while the save bar is visible so it never covers the final field row.

- [ ] **Step 4: Implement sequential responsive behavior**

At `max-width: 1023px`, show only the master panel until `mobileDetailOpen()` becomes true. Selecting a table sets it true; `showTableList()` returns to the master without changing filters, selection or draft data. At 1024–1199 px, use a 300 px master column. At 1200 px and above, use 340 px.

Do not implement a drawer or rely on hover.

- [ ] **Step 5: Add focus, target and reduced-motion rules**

All row buttons and controls must have at least a 44 px interactive area. Use `:focus-visible` with a 2 px `#0a0a0a` outline and 2 px offset. Status meaning must remain present in text when icons are hidden. Disable non-essential transitions under `prefers-reduced-motion: reduce`.

- [ ] **Step 6: Run focused tests and build**

Run the Task 3 focused test and build commands. Expected: PASS.

- [ ] **Step 7: Commit the complete state coverage**

```powershell
git add src/app/features/data-admin/pages/data-configuration
git commit -m "feat: complete data administration UI states"
```

---

### Task 5: Register the route and perform final verification

**Files:**
- Modify: `src/app/app.routes.ts`
- Create: `src/app/app.routes.spec.ts`
- Verify: all files from Tasks 1–4

**Interfaces:**
- Consumes: `DataConfigurationComponent` lazy export.
- Produces: route `/administration/donnees`.

- [ ] **Step 1: Write the failing lazy-route test**

```ts
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { routes } from './app.routes';

describe('application routes', () => {
  it('loads the data administration page', async () => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideNoopAnimations()],
    });

    const harness = await RouterTestingHarness.create('/administration/donnees');

    expect(harness.routeNativeElement?.querySelector('.data-admin-page')).not.toBeNull();
  });
});
```

- [ ] **Step 2: Verify RED**

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/app.routes.spec.ts
```

Expected: FAIL because the route does not exist.

- [ ] **Step 3: Add the lazy route without introducing a shell or guard**

Add before the test route in `app.routes.ts`:

```ts
{
  path: 'administration/donnees',
  loadComponent: () =>
    import('./features/data-admin/pages/data-configuration/data-configuration.component').then(
      (module) => module.DataConfigurationComponent,
    ),
},
```

Do not add a route guard because no reusable admin guard exists in the current frontend; backend authorization and route protection belong to the later integration phase.

- [ ] **Step 4: Run all frontend tests and production build**

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless
npm.cmd run build
git diff --check
```

Expected: all tests pass, production build succeeds, and `git diff --check` reports no whitespace errors.

- [ ] **Step 5: Perform visual QA at required widths**

Start the development server:

```powershell
npm.cmd start
```

Open `/administration/donnees` and verify at 1440, 1200, 1024 and 900 px:

- three table dimensions remain distinct and readable;
- no `active` checkbox exists for tables or fields;
- all four `displayMain/displayRelated` combinations are represented;
- selection uses marker, typography and chevron, not color alone;
- table and field scroll areas preserve sticky headers;
- inactive content is readable and locked without global opacity;
- search, combined filters, no-results and reset actions work;
- local changes survive table navigation and mobile back navigation;
- save bar does not cover the last field row;
- no indigo accent, gradient, Material Symbol, sidebar or oversized card appears.

- [ ] **Step 6: Commit the route and final test**

```powershell
git add src/app/app.routes.ts src/app/app.routes.spec.ts
git commit -m "feat: expose data administration prototype"
```

---

## Completion Criteria

- The page is reachable at `/administration/donnees` and uses only local fixtures.
- Table `active` and field `active` cannot be changed through the UI.
- Table `displayMain`, table `displayRelated` and active-field `visible` can be changed independently.
- Search, three filter dimensions, selection, draft preservation, cancel and mock save feedback are covered by tests.
- Every visual state from the specification is present.
- Responsive behavior is verified at the four required widths.
- Full Angular tests and the production build pass.
- No backend, DTO, API, database, global theme or unrelated frontend file changes are included.
