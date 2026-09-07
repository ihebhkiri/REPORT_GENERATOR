# Burger King PDF Branding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. No delegation is required.

**Goal:** Generate professional PDF reports with Burger King identity and palette instead of RHIS branding.

**Architecture:** Keep the existing JasperReports pipeline. Package the approved logo beside the Jasper template, change the existing writer constants and template colors, then verify the generated PDF through the existing writer test.

**Tech Stack:** Java, Spring Boot, JasperReports, OpenPDF test utilities, JUnit 5, Maven.

## Global Constraints

- Preserve PDF data, landscape layout, dynamic columns, pagination, dates, progress and download contracts.
- Use `#D62300`, `#FF8732` and `#F5EBDC` from the supplied Burger King logo.
- Do not add dependencies, abstractions, frontend changes or XLSX changes.
- Approved design: `../specs/2026-09-07-burger-king-pdf-branding-design.md`.

---

### Task 1: Replace the PDF identity and palette

**Files:**
- Create: `src/main/resources/reports/burger-king-logo.svg`
- Modify: `src/main/java/RHIS/com/RHIS/report/export/PdfReportExportWriter.java`
- Modify: `src/main/resources/reports/report-export-base.jrxml`
- Test: `src/test/java/RHIS/com/RHIS/report/export/ReportExportWriterTest.java`

**Interfaces:**
- Consumes: existing `REPORT_LOGO` and `REPORT_TITLE` Jasper parameters.
- Produces: unchanged `PdfReportExportWriter.write(InputStream, OutputStream, IntConsumer)` behavior with Burger King branding.

- [ ] **Step 1: Update the existing PDF assertions to expect the new identity.** Replace `Rapport RHIS` with `Rapport Burger King`; assert `/Creator` equals `Burger King`; retain the XObject assertion proving an image is embedded.
- [ ] **Step 2: Run the focused writer tests and confirm the branding assertions fail.**

  ```powershell
  mvn test -Dtest=ReportExportWriterTest
  ```

- [ ] **Step 3: Package the supplied logo.** Copy `../Frontend/Rhis_report_gen/public/Burger_King_logo.svg` to `src/main/resources/reports/burger-king-logo.svg` without modifying its artwork.
- [ ] **Step 4: Update the writer constants.** Set `LOGO_RESOURCE` to `/reports/burger-king-logo.svg`, `REPORT_TITLE` to `Rapport Burger King`, and PDF metadata creator to `Burger King`.
- [ ] **Step 5: Update the Jasper template.** Use red `#D62300` for the title and column headers, orange `#FF8732` for the separator, cream `#F5EBDC` for subtle header borders or secondary surface, white data cells and a dark neutral for supporting text. Preserve dimensions and dynamic styles.
- [ ] **Step 6: Run the focused tests.** Expected: `ReportExportWriterTest` passes and generated PDFs contain the new title, metadata and image object.

### Task 2: Verify the rendered PDF and regressions

**Files:**
- Update: this plan only with final results.

**Interfaces:** None.

- [ ] **Step 1: Run Jasper dynamic-table tests.**

  ```powershell
  mvn test -Dtest=JasperDynamicTableConfigurerTest,ReportExportWriterTest
  ```

- [ ] **Step 2: Run the backend package verification.**

  ```powershell
  mvn package
  ```

- [ ] **Step 3: Generate or reuse a test PDF and inspect the rendered first page.** Confirm the logo keeps its proportions, the report title is visible, headers are red with readable white text, the orange separator is restrained and table content remains legible.
- [ ] **Step 4: Run `git diff --check` and review the final diff for correctness, resource compatibility and unrelated changes.**
- [ ] **Step 5: Run `graphify update .` at repository root when available and record its result.**

## Progress

- [x] 2026-09-07 — Existing PDF writer, Jasper template, logo asset and PDF tests inspected.
- [x] 2026-09-07 — Design approved and committed.
- [x] 2026-09-07 — Branding implementation complete.
- [x] 2026-09-07 — Five PDF tests and three Jasper table tests pass; rendered first page inspected successfully.
- [x] 2026-09-07 — Full backend package attempted; 101 tests ran with eight unrelated baseline failures.
- [x] 2026-09-07 — Final diff check passed and Graphify rebuilt with 3073 nodes and 6206 edges.

## Surprises & Discoveries

- The Burger King SVG already exists in the frontend public assets and declares the exact palette required by the design.
- PDF branding is entirely backend-side: `PdfReportExportWriter` supplies the logo/title and the Jasper template owns the colors.
- JasperReports renders the supplied SVG directly, so no raster conversion or new dependency is needed.
- The full Maven run fails outside this change: four XLSX assertions use the default `xlsxRowWindow=0`, and four PostgreSQL integration scenarios cannot create `reportJobExecutor` because test job properties are unset.

## Decision Log

- 2026-09-07 — Reuse the supplied artwork and existing Jasper parameters; avoids a dependency and any export API change.
- 2026-09-07 — Keep the table body neutral and reserve brand colors for hierarchy to preserve dense-report readability.

## Outcomes & Retrospective

Burger King branding is implemented in generated PDFs. The writer now embeds the supplied SVG, emits `Rapport Burger King`, writes `Burger King` creator metadata and uses the approved red, orange and cream palette. The unused RHIS report logo resource was removed.

Verification performed: `ReportExportWriterTest#*Pdf*` passed 5/5; `JasperDynamicTableConfigurerTest` passed 3/3; a generated first page was rendered with Poppler and visually inspected with no clipping, distortion or contrast issue. `mvn package` ran 101 tests but remains red because of the eight unrelated configuration failures recorded above.
