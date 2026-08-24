# Report Code Documentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add concise, verified TSDoc/Javadoc to the frontend and backend report-preview flow without changing behavior or formatting unrelated code.

**Architecture:** Keep documentation next to the implementation that enforces each invariant. Document page orchestration and dataset resolution in Angular, then validation, SQL construction, JDBC execution, and internal handoff models in Spring Boot. Do not document controllers, DTOs, exceptions, constructors, or obvious private methods.

**Tech Stack:** Angular 20, TypeScript strict mode, RxJS, Java 17, Spring Boot, Spring JDBC, Maven, Jasmine/Karma.

## Global Constraints

- Documentation only: no refactoring, behavior changes, contract changes, or unrelated formatting.
- Before each comment, verify the invariant against current production code and its relevant tests.
- If an invariant is not guaranteed, stop and report it instead of changing code or documenting intended behavior.
- Explain responsibility, business rule, transformation, assumption, edge case, or technical constraint; never paraphrase obvious code.
- Use TSDoc `/** ... */` in TypeScript and Javadoc `/** ... */` in Java.
- Preserve all existing frontend Signals, RxJS flows, routes, child contracts, backend transaction boundaries, SQL semantics, limits, ordering, and exception behavior.
- Do not stage or commit: both repositories contain pre-existing uncommitted work in the target files.

---

### Task 1: Document Angular report configuration responsibilities

**Files:**
- Modify: `../Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration/configuration.component.ts`
- Modify: `../Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration/report-configuration-loader.service.ts`
- Test: `../Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration/configuration.component.spec.ts`
- Test: `../Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration/report-configuration-loader.service.spec.ts`

**Interfaces:**
- Consumes: `ReportConfigurationLoader.load(mainDatasetId, relatedDatasetIds): Observable<ReportConfigurationLoadResult>`.
- Produces: documentation only; no TypeScript interface, input, output, Signal, route, or observable changes.

- [ ] **Step 1: Verify the page/loader responsibility seam**

Inspect `ConfigurationComponent.loadConfiguration()`, `openPreview()`, `retryPreview()`, `updateSelectedFields()`, `updateFilters()`, and `updateSorts()`. Confirm the page owns synchronous editor/preview state and only `openPreview()`/`retryPreview()` call `loadPreview()`. Confirm the loader contains dataset resolution and field loading.

Run:

```powershell
rg -n "loadConfiguration|openPreview|retryPreview|loadPreview|updateSelectedFields|updateFilters|updateSorts|resolveSelectedDatasets|loadFieldGroups" src/app/features/rapports/pages/configuration -g "*.ts"
```

Expected: page actions remain in `configuration.component.ts`; `resolveSelectedDatasets()` and `loadFieldGroups()` exist only in `report-configuration-loader.service.ts`.

- [ ] **Step 2: Verify loader ordering and relation invariants**

Confirm in `resolveSelectedDatasets()` that:

- a related dataset is accepted only when `relation.sourceDatasetId === mainDatasetId`;
- self-relations are excluded by `relation.targetDatasetId !== mainDatasetId`;
- related dataset identity comes from relation metadata rather than a second lookup in the datasets list;
- the returned array places the main dataset first;
- related datasets use `localeCompare(..., 'fr', { sensitivity: 'base' })`;
- `filter().map()` keeps supported fields in API order and `forkJoin` receives datasets in resolved order.

Run:

```powershell
npm.cmd test -- --watch=false --include=src/app/features/rapports/pages/configuration/configuration.component.spec.ts --include=src/app/features/rapports/pages/configuration/report-configuration-loader.service.spec.ts
```

Expected: `TOTAL: 24 SUCCESS`.

- [ ] **Step 3: Add the class TSDoc to `ConfigurationComponent`**

Insert immediately before `@Component`:

```typescript
/**
 * Owns the page-local report definition and preview lifecycle.
 * Dataset resolution and field loading stay behind `ReportConfigurationLoader`, while this
 * component remains the source of truth for child-editor state and explicit preview requests.
 */
```

Do not document individual Signals or obvious event handlers.

- [ ] **Step 4: Add the class TSDoc to `ReportConfigurationLoader`**

Insert immediately before `@Injectable`:

```typescript
/**
 * Resolves route-selected datasets into the field groups consumed by report configuration.
 * Only direct outgoing relations from the main dataset are eligible; relation metadata is
 * authoritative for related datasets. The main dataset stays first, related datasets are sorted
 * by their French display name, and fields retain the order returned by the API.
 */
```

Do not add method-level comments to the loader: the class block captures the non-obvious rules without restating private methods.

- [ ] **Step 5: Verify frontend documentation changes**

Run:

```powershell
npm.cmd exec -- tsc -p tsconfig.app.json --noEmit
npm.cmd test -- --watch=false --include=src/app/features/rapports/pages/configuration/configuration.component.spec.ts --include=src/app/features/rapports/pages/configuration/report-configuration-loader.service.spec.ts
git diff --check
```

Expected: strict compilation succeeds, 24 tests pass, and `git diff --check` exits with code `0`.

Review the two target files and confirm the only new text is the two TSDoc blocks above.

---

### Task 2: Document backend preview validation, SQL, and JDBC constraints

**Files:**
- Modify: `src/main/java/RHIS/com/RHIS/report/service/ReportPreviewService.java`
- Modify: `src/main/java/RHIS/com/RHIS/report/service/ReportSqlBuilder.java`
- Modify: `src/main/java/RHIS/com/RHIS/report/service/ReportPreviewExecutor.java`
- Modify: `src/main/java/RHIS/com/RHIS/report/service/ReportQueryModel.java`
- Test: `src/test/java/RHIS/com/RHIS/report/service/ReportPreviewServiceTest.java`
- Test: `src/test/java/RHIS/com/RHIS/report/service/ReportSqlBuilderTest.java`
- Test: `src/test/java/RHIS/com/RHIS/report/service/ReportPreviewExecutorTest.java`

**Interfaces:**
- Consumes: `ReportPreviewService.preview(ReportPreviewRequest)`, `ReportSqlBuilder.build(ResolvedReportDefinition)`, and `ReportPreviewExecutor.execute(PreparedReportQuery)`.
- Produces: documentation only; no Java type, visibility, annotation, transaction, SQL, record component, or exception changes.

- [ ] **Step 1: Verify service validation rules before documenting them**

Confirm that:

- catalog visibility/type checks run before SQL construction;
- filters may reference accessible unselected fields because filter IDs are loaded and included in `referencedFields()`;
- `resolveSort()` rejects a sort field absent from `selectedFieldIds`;
- `resolveJoin()` filters source=root and target=referenced dataset;
- grouping by constraint name accepts one direct constraint, including composite columns, and rejects zero or multiple constraints;
- composite join columns are sorted by relation position;
- `resolveSelectedFields()` and `resolveSorts()` preserve request order.

Run:

```powershell
mvn test "-Dtest=ReportPreviewServiceTest"
```

Expected: 14 tests pass.

- [ ] **Step 2: Add Javadoc to `ReportPreviewService`**

Insert before `@Service`:

```java
/**
 * Validates a preview request against visible dataset metadata before delegating SQL generation
 * and execution.
 *
 * <p>Filters may reference accessible fields not selected for output, but sort fields must be
 * selected. Every joined dataset must be reachable from the root through exactly one direct
 * outgoing foreign-key constraint. Selected-field and sort order are preserved.</p>
 */
```

Insert before `resolveJoin(...)`:

```java
/**
 * Resolves one outgoing foreign-key constraint to the target dataset. Composite-key columns are
 * kept in catalog position order; zero or multiple matching constraints are rejected as ambiguous.
 */
```

- [ ] **Step 3: Verify SQL builder constraints before documenting them**

Confirm that:

- request values reach SQL only through `?` placeholders and the ordered parameters list;
- table/column names originate from validated catalog entities/relations and pass through `quoteIdentifier()`;
- `CONTAINS` escapes `!`, `%`, and `_` before binding and declares `ESCAPE '!'`;
- explicit sorts retain their order;
- absent explicit sorts use root primary-key fields loaded by catalog position;
- absent sort and absent primary-key metadata produce no `ORDER BY`;
- `FETCH_LIMIT` is `7`.

Run:

```powershell
mvn test "-Dtest=ReportSqlBuilderTest"
```

Expected: 2 tests pass.

- [ ] **Step 4: Add Javadoc to `ReportSqlBuilder`**

Insert before `@Component`:

```java
/**
 * Builds a parameterized PostgreSQL preview query from a validated, catalog-backed definition.
 *
 * <p>Values are bound as JDBC parameters; table and column identifiers are quoted separately
 * because they originate from validated metadata. The query fetches one row beyond the six-row
 * preview limit so {@code hasMore} can be determined without a count query.</p>
 */
```

Insert before `appendOrderBy(...)`:

```java
/**
 * Applies the requested sort order verbatim. Without an explicit sort, root primary-key fields
 * provide a stable order when cataloged; if none exist, no {@code ORDER BY} clause is added.
 */
```

Insert before `escapeLikeValue(...)`:

```java
/**
 * Escapes the configured {@code !} escape character before PostgreSQL wildcard characters so a
 * {@code CONTAINS} value is matched literally.
 */
```

- [ ] **Step 5: Verify executor constraints before documenting them**

Confirm that:

- `PREVIEW_LIMIT` is `6` while the builder SQL limit is `7`;
- the seventh row is removed from the response and only determines `hasMore`;
- `returnedRowCount` equals the visible row count;
- `setQueryTimeout(5)` is applied to the prepared statement before parameters are bound;
- temporal values and UUIDs use typed `ResultSet.getObject` calls.

Run:

```powershell
mvn test "-Dtest=ReportPreviewExecutorTest"
```

Expected: 1 test passes.

- [ ] **Step 6: Add Javadoc to `ReportPreviewExecutor`**

Insert before `@Component`:

```java
/**
 * Executes prepared preview queries with a five-second JDBC timeout and maps typed result cells.
 *
 * <p>The SQL builder fetches at most seven rows; this executor exposes at most six and derives
 * {@code hasMore} from the extra row without running a count query.</p>
 */
```

Insert before `readValue(...)`:

```java
/**
 * Uses typed JDBC reads for temporal and UUID columns so the response contains domain Java types
 * rather than driver-specific representations.
 */
```

- [ ] **Step 7: Verify and document internal query-model transformations**

Confirm `ReportPreviewService` constructs `ResolvedReportDefinition` only after catalog access, operator, value, sort, and join validation; confirm `parseValue()` converts filter strings before `ResolvedFilter` construction; confirm `ReportSqlBuilder` constructs `PreparedReportQuery` with copied ordered parameters and the same columns used by `appendSelect()`.

Insert before `ResolvedReportDefinition`:

```java
/**
 * Catalog-backed report definition produced after field, filter, sort, and join validation.
 * Client field identifiers are resolved and textual filter values are already coerced.
 */
```

Insert before `ResolvedFilter`:

```java
/** Filter whose request values have been converted to the Java type declared by field metadata. */
```

Insert before `PreparedReportQuery`:

```java
/**
 * Immutable handoff from SQL construction to JDBC execution. Parameter order matches the SQL
 * placeholders, and column order matches the aliases emitted by the {@code SELECT} clause.
 */
```

- [ ] **Step 8: Verify backend documentation changes**

Run:

```powershell
mvn test "-Dtest=ReportPreviewServiceTest,ReportSqlBuilderTest,ReportPreviewExecutorTest"
mvn -DskipTests compile
git diff --check
```

Expected: 17 tests pass, compilation succeeds, and `git diff --check` exits with code `0`.

Review the four target files and confirm only the Javadoc blocks specified above were added.

---

### Task 3: Cross-project documentation-only verification

**Files:**
- Review only: all files listed in Tasks 1 and 2.
- No files created or modified in this task.

**Interfaces:**
- Consumes: documented frontend and backend report flow from Tasks 1 and 2.
- Produces: verified documentation-only change set ready for user review.

- [ ] **Step 1: Run the frontend production checks**

Run:

```powershell
npm.cmd test -- --watch=false
npm.cmd exec -- tsc -p tsconfig.app.json --noEmit
npm.cmd run build
```

Expected: all frontend tests pass, strict TypeScript compilation succeeds, and the Angular build succeeds with no new warning attributable to documentation.

- [ ] **Step 2: Run the backend report checks**

Run:

```powershell
mvn test "-Dtest=ReportPreviewServiceTest,ReportSqlBuilderTest,ReportPreviewExecutorTest"
mvn -DskipTests compile
```

Expected: 17 report unit tests pass and Java compilation succeeds.

- [ ] **Step 3: Audit the final scope**

Run in each repository:

```powershell
git status --short
git diff --check
```

Inspect every target file and confirm:

- only TSDoc/Javadoc blocks were added;
- no imports, annotations, signatures, statements, whitespace, or formatting outside those blocks changed;
- every documented invariant still points to enforcing production code;
- no controller, DTO, exception, constructor, getter, setter, or obvious method received documentation.

- [ ] **Step 4: Report the result without committing**

Report the exact files modified, invariants documented, frontend/backend verification commands and outcomes, any pre-existing warning, and confirmation that no behavior changed. Do not stage or commit the dirty worktrees.
