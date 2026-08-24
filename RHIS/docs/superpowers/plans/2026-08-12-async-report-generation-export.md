# Async Report Generation and Export Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build owner-scoped asynchronous report snapshots and reusable PDF/XLSX exports, then connect the existing Angular configuration and export pages to that workflow.

**Architecture:** Extract the catalog-backed report definition resolver and make the SQL builder expose preview, count, and unlimited ordered queries. Persist only job metadata in PostgreSQL; stream query rows into a compressed NDJSON snapshot through a storage abstraction, then stream that immutable snapshot into Apache POI SXSSF or JasperReports. A bounded Spring executor runs jobs, short transactions own state transitions, and Angular uses an URL UUID plus two-second RxJS polling while keeping only the lightweight report definition in `sessionStorage`.

**Tech Stack:** Java 17, Spring Boot 4.1, Spring Security/JPA/JDBC, Jackson, Apache POI 5.5.1, JasperReports 7.0.7 with its OpenPDF exporter, PostgreSQL/H2 tests, Angular 20, strict TypeScript, Signals, RxJS, Jasmine/Karma.

## Global Constraints

- Preserve preview behavior: five-second timeout, SQL `LIMIT 7`, at most six returned rows.
- Never accumulate the complete result set or snapshot in a Java collection.
- SQL identifiers come only from validated catalog metadata; values remain JDBC-bound.
- All generation/export operations are authenticated and owner-scoped; foreign UUIDs return 404.
- A snapshot is immutable and remains available for both formats until deletion or expiration.
- Local filesystem storage is the V1 development implementation; production object storage remains behind `ReportArtifactStorage`.
- Expiration is 30 minutes after `READY` or the latest export activity; active workers refresh a heartbeat.
- Do not stage or overwrite pre-existing unrelated changes in either repository.

---

### Task 1: Shared definition resolution and SQL variants

**Files:**
- Create: `src/main/java/RHIS/com/RHIS/report/service/ReportDefinitionResolver.java`
- Modify: `src/main/java/RHIS/com/RHIS/report/service/ReportPreviewService.java`
- Modify: `src/main/java/RHIS/com/RHIS/report/service/ReportSqlBuilder.java`
- Modify: `src/main/java/RHIS/com/RHIS/report/service/ReportQueryModel.java`
- Test: `src/test/java/RHIS/com/RHIS/report/service/ReportDefinitionResolverTest.java`
- Test: `src/test/java/RHIS/com/RHIS/report/service/ReportSqlBuilderTest.java`
- Test: existing preview tests

**Interfaces:**
- Produces: `ResolvedReportDefinition ReportDefinitionResolver.resolve(ReportPreviewRequest request)`.
- Produces: `PreparedReportQuery buildPreview(...)`, `PreparedReportQuery buildFull(...)`, and `PreparedCountQuery buildCount(...)`.

- [ ] Move catalog lookup, validation, typed filter parsing, direct-FK resolution, and order preservation from `ReportPreviewService` into `ReportDefinitionResolver.resolve(...)`; keep the resolver transaction read-only.
- [ ] Write resolver tests first for selected/filter/sort rules, ambiguous joins, composite join order, hidden metadata, unsupported types, and request ordering; run `mvn test "-Dtest=ReportDefinitionResolverTest"` and observe the initial failure.
- [ ] Make `ReportPreviewService.preview(...)` delegate only to resolver, `buildPreview`, and executor; run existing service/controller tests.
- [ ] Split SQL generation into a shared select/from/join/filter/order core: preview appends `LIMIT 7`, full appends no limit, count uses `SELECT COUNT(*)` with identical joins and filters and no order.
- [ ] Assert exact SQL and parameter lists for all three variants, including escaped `CONTAINS`, explicit sorts, PK fallback order, and absence of a full-query limit.
- [ ] Run `mvn test "-Dtest=ReportDefinitionResolverTest,ReportSqlBuilderTest,ReportPreviewServiceTest,ReportPreviewExecutorTest,ReportPreviewPostgresIntegrationTest"`.

---

### Task 2: Job persistence, public contracts, and owner security

**Files:**
- Create: `report/model/{ReportGenerationStatus,ReportGenerationPhase,ReportExportStatus,ReportExportFormat}.java`
- Create: `report/entity/{ReportGenerationEntity,ReportExportEntity}.java`
- Create: `report/repository/{ReportGenerationRepository,ReportExportRepository}.java`
- Create: `report/controller/dto/{ReportGenerationResponse,CreateReportExportRequest,ReportExportResponse}.java`
- Create: `report/controller/{ReportGenerationController,ReportExportController}.java`
- Create: `report/service/{ReportGenerationService,ReportExportService,ReportJobDispatcher}.java`
- Modify: `report/controller/ReportExceptionHandler.java`
- Modify: `auth/SecurityConfig.java`
- Test: `report/controller/{ReportGenerationControllerTest,ReportExportControllerTest}.java`
- Test: `report/service/ReportGenerationServiceTest.java`

**Interfaces:**
- `create(ownerId, idempotencyKey, request) -> ReportGenerationResponse` returns the existing row for `(owner,idempotencyKey)`.
- `get/delete(ownerId,generationId)` and `get/file(ownerId,exportId)` query by both ID and owner.
- `createExport(ownerId,generationId,format)` reuses `READY`, rejects active duplicates, and replaces a failed export.

- [ ] Write MVC/service tests for 202/Location responses, malformed or missing `Idempotency-Key`, idempotent replay, unauthenticated 401, foreign-owner 404, cascade deletion, duplicate active export, and non-READY download.
- [ ] Add JPA entities with UUID IDs, JSON definition text mapped to JSONB-compatible text, enums as strings, progress/count/locations/timestamps/error codes, FK cascade, unique owner/idempotency and generation/format constraints, plus owner/status/expiration indexes.
- [ ] Derive `ownerId` from `@AuthenticationPrincipal UserPrincipal`; never accept it from the request body.
- [ ] Add authenticated endpoint mappings exactly as specified: `/api/v1/report-generations` and `/api/v1/report-exports`; leave no storage location in DTOs.
- [ ] Restrict report routes in `SecurityConfig` to authenticated users and return sanitized `ProblemDetail` responses for invalid state, capacity, validation, missing resource, and execution failures.
- [ ] Run `mvn test "-Dtest=ReportGenerationControllerTest,ReportExportControllerTest,ReportGenerationServiceTest,ReportControllerSecurityTest"`.

---

### Task 3: Bounded execution and streamed snapshot

**Files:**
- Create: `report/config/ReportJobProperties.java`
- Create: `report/config/ReportJobConfiguration.java`
- Create: `report/storage/{ReportArtifactStorage,FileSystemReportArtifactStorage}.java`
- Create: `report/snapshot/{ReportSnapshotMetadata,ReportSnapshotWriter,ReportSnapshotReader}.java`
- Create: `report/service/{ReportFullQueryExecutor,ReportGenerationWorker,ReportJobStateService}.java`
- Modify: `src/main/resources/application.yaml`
- Test: `report/snapshot/ReportSnapshotStorageTest.java`
- Test: `report/service/{ReportFullQueryExecutorTest,ReportGenerationWorkerTest}.java`

**Interfaces:**
- `ReportArtifactStorage.writeAtomically(key, writer)`, `open(key)`, `delete(key)`, and `exists(key)` keep paths private.
- `ReportFullQueryExecutor.count(query)` and `stream(query, RowConsumer)` set configurable timeout/fetch size and bind parameters.
- `ReportGenerationWorker.run(generationId)` transitions `PENDING -> RUNNING -> READY|FAILED` through `REQUIRES_NEW` state methods.

- [ ] Write storage tests proving atomic promotion, path traversal rejection, recursive generation deletion, and cleanup after a failed writer.
- [ ] Configure a named `ThreadPoolTaskExecutor` with configurable core/max/queue values and rejection handling that marks a submitted job failed/capacity-limited instead of blocking HTTP threads.
- [ ] Write gzip NDJSON as one metadata record followed by one JSON object per row; expose a reader iterator/callback that closes streams and never returns all rows.
- [ ] Implement JDBC count and forward-only streaming with `fetchSize`, query timeout, typed column reads, row callback, cancellation checks, and no encompassing JPA transaction.
- [ ] Distribute monotonic progress across validation (0-10), count (10-20), reading (20-95), and atomic snapshot finalization (95-100); update heartbeat/progress in short transactions.
- [ ] Test 0, 1, and 10,000 rows with a consumer that records maximum in-flight rows rather than retaining rows; assert 100 only after the artifact exists and failures remove partial artifacts.
- [ ] Run `mvn test "-Dtest=ReportSnapshotStorageTest,ReportFullQueryExecutorTest,ReportGenerationWorkerTest,ReportPreviewPostgresIntegrationTest"`.

---

### Task 4: XLSX and PDF export workers

**Files:**
- Modify: `pom.xml`
- Create: `report/export/{ReportExportWriter,XlsxReportExportWriter,PdfReportExportWriter}.java`
- Create: `report/service/ReportExportWorker.java`
- Test: `report/export/{XlsxReportExportWriterTest,PdfReportExportWriterTest}.java`
- Test: `report/service/ReportExportWorkerTest.java`

**Interfaces:**
- `ReportExportWriter.supports(format)` and `write(snapshotReader, outputStream, progress)` select an implementation.
- `ReportExportWorker.run(exportId)` owns `PENDING -> RUNNING -> READY|FAILED` and refreshes parent generation activity.

- [ ] Add Apache POI `poi-ooxml:5.5.1` and JasperReports `jasperreports`, `jasperreports-pdf`, and `jasperreports-fonts` 7.0.7; record JasperReports LGPL/OpenPDF licensing and verify dependency resolution.
- [ ] Write XLSX tests first: ordered headers/cells, typed numeric/boolean/date values, null blanks, 10,000 rows, frozen header, bounded SXSSF row window, and disposal of POI temp files.
- [ ] Implement one reusable header/data style set, inline strings, fixed widths capped to a safe range, and streaming output through `SXSSFWorkbook`.
- [ ] Write PDF tests first: every row present across pages, repeated headers, ordered columns, null rendering, long-value clipping/wrapping policy, and valid document page count.
- [ ] Implement a branded JasperReports 7 JRXML shell plus a dynamic landscape A4 table, repeated headers, wrapped text, a streaming `JRDataSource`, and disk-backed virtualization with deterministic cleanup.
- [ ] Ensure an export failure deletes only its partial file, preserves the snapshot, records a sanitized error code, and allows retry.
- [ ] Run `mvn test "-Dtest=XlsxReportExportWriterTest,PdfReportExportWriterTest,ReportExportWorkerTest"`.

---

### Task 5: Expiration, cancellation, abandoned jobs, and download

**Files:**
- Create: `report/service/ReportJobCleanupService.java`
- Modify: job services/controllers/repositories/configuration
- Test: `report/service/ReportJobCleanupServiceTest.java`
- Test: controller integration tests

**Interfaces:**
- `cleanupExpired(now)` cancels active in-memory futures, deletes artifacts, scrubs definition/locations, and marks generation `EXPIRED`.
- `failStaleRunning(now)` marks workers with old heartbeats `FAILED` without expiring healthy workers.

- [ ] Add scheduled cleanup with configurable scan interval, 30-minute ready/activity TTL, heartbeat timeout, and 24-hour metadata purge.
- [ ] Store submitted `Future<?>` handles by resource UUID so DELETE can request cancellation; workers also check persisted cancellation/expiry between batches.
- [ ] Serve `Resource` downloads only for owner-scoped `READY` exports with backend-generated filename, exact MIME type, and attachment `Content-Disposition`.
- [ ] Test explicit DELETE during generation/export, repeated DELETE returning 404, stale heartbeat failure, healthy heartbeat preservation, artifact cleanup, 24-hour purge, and snapshot retention after export failure.
- [ ] Run the complete backend suite with `mvn test`, then `mvn package`.

---

### Task 6: Angular API, draft persistence, and generation submission

**Files (frontend repository):**
- Create: `src/app/features/rapports/models/report-generation.model.ts`
- Create: `src/app/features/rapports/services/report-generation.service.ts`
- Create: `src/app/features/rapports/services/report-draft-storage.service.ts`
- Test: matching `*.spec.ts`
- Modify: `pages/configuration/configuration.component.{ts,html}` and spec

**Interfaces:**
- `ReportGenerationService.create(definition,idempotencyKey)`, `generation(id)`, `deleteGeneration(id)`, `createExport(id,format)`, `export(id)`, and `download(id)`.
- `ReportDraftStorage.save/load/clear` stores only `ReportPreviewRequest` plus selected dataset route identity.

- [ ] Write HttpTestingController tests for all URLs, headers, credentials, typed bodies, blob download, and `ProblemDetail` propagation.
- [ ] Write session-storage tests for versioned JSON, invalid/corrupt data eviction, exact ordered definition restoration, and absence of full result rows.
- [ ] Add a Generate action guarded by the same selected-column/filter validity as Preview plus an `isGenerating` signal; generate a fresh UUID idempotency key per explicit attempt and guard double clicks.
- [ ] Save the draft before POST; on 202 navigate to `/rapports/export/:generationId`; on failure remain on Configuration and display `detail`.
- [ ] Restore selected fields, filters, and sorts only after datasets/fields resolve, dropping nothing when every referenced ID is still accessible.
- [ ] Run focused Angular tests and `npm.cmd exec -- tsc -p tsconfig.app.json --noEmit`.

---

### Task 7: Angular export page, polling, download, and cleanup

**Files (frontend repository):**
- Modify: `src/app/features/rapports/rapports.routes.ts`
- Replace: `src/app/features/rapports/pages/export/export.component.{ts,html,scss}`
- Modify: `src/app/features/rapports/pages/export/export.component.spec.ts`

**Interfaces:**
- Route parameter is `generationId`, never a dataset ID.
- Polling uses `timer(0, 2000)` + `switchMap`, terminates on `READY|FAILED|EXPIRED` and `takeUntilDestroyed`, while transient network errors delay/retry without recreating a job.

- [ ] Replace the prototype export form/dialog with generation phase/progress UI, processed/total counts, independent PDF/XLSX job cards, recoverable retry, and accessible status text.
- [ ] Resume generation polling from the URL after refresh; once ready, create/poll each export independently and retain ready export IDs in page-local session state so refresh resumes them.
- [ ] Download ready blobs using the backend filename from `Content-Disposition`; do not delete the generation after a download.
- [ ] Implement Previous and navigation-away best-effort deletion while avoiding deletion on reload; always preserve the draft, and redirect expired jobs to Configuration with an explanation.
- [ ] Test terminal polling, destruction cancellation, transient network recovery, refresh resume, two formats from one generation, download, deletion, Previous restoration, and expired redirect.
- [ ] Run `npm.cmd test -- --watch=false` and `npm.cmd run build`; run `git diff --check` in both repositories.

---

### Task 8: Acceptance and load verification

**Files:**
- Add only focused test fixtures/helpers required by the acceptance tests.

- [ ] Run the complete backend and frontend suites from clean processes.
- [ ] Execute a PostgreSQL/Testcontainers scenario with 50,000 deterministic rows; record peak heap before/after generation plus XLSX/PDF export and assert the full row count in both artifacts.
- [ ] Verify two authenticated users cannot observe, delete, export, or download each other's UUID resources.
- [ ] Verify explicit deletion and scheduled expiration leave no snapshot/export files and no definition/location data.
- [ ] Review diffs for unrelated edits, secrets, exposed filesystem paths, unbounded executors, `List<Map<...>>` accumulation, and accidental SQL limits.
- [ ] Do not commit or stage without explicit user direction because both repositories already contain pre-existing changes.
