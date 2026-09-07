# Reproduce the supplied PDF reference

Date: 2026-09-07
Status: Completed
Research: `docs/research/2026-09-07-pdf-reference-layout.md`

## Purpose and observable outcome
Match the supplied Burger King image using the existing Jasper PDF pipeline.

## Scope and non-goals
Only PDF visual resources, table presentation and focused tests. Preserve all data, expressions, equal-width sizing, pagination mechanism, asynchronous export, XLSX and API contracts.

## Current behavior
See research. Existing local branding changes are the starting point and must be preserved.

## Proposed approach
Use A4 landscape with 16pt side margins, larger logo/title/date, orange separator, 37pt headers and 32pt rows, cream alternating backgrounds, gray grid, lower page numbering and a full-width decorative SVG footer. Keep DejaVu Sans embedded and the existing logo artwork. Text columns align left; numeric/date types center. No new dependency or generic abstraction.

## Affected files and symbols
- `RHIS/src/main/resources/reports/report-export-base.jrxml`: geometry, styles, conditional row background and footer.
- `RHIS/src/main/resources/reports/burger-king-footer.svg`: curved footer artwork.
- `RHIS/src/main/java/RHIS/com/RHIS/report/export/PdfReportExportWriter.java`: required footer stream parameter only.
- `RHIS/src/main/java/RHIS/com/RHIS/report/export/JasperDynamicTableConfigurer.java`: heights, padding and type-based alignment only.
- `RHIS/src/test/java/RHIS/com/RHIS/report/export/JasperDynamicTableConfigurerTest.java`: visual geometry expectations.
- `RHIS/src/test/java/RHIS/com/RHIS/report/export/ReportExportWriterTest.java`: representative eight-column PDF fixture and footer assertion.

## Milestone 1: Presentation
Implement the listed local changes. Run `mvn '-Dtest=JasperDynamicTableConfigurerTest,ReportExportWriterTest#*Pdf*' test` from RHIS. Require successful JRXML compilation, preserved content, repeated headers and footer, valid Landscape pages.

## Milestone 2: Visual verification
Generate a synthetic eight-column ten-row reference fixture through the real writer. Render with Poppler, inspect the first and continuation pages, adjust only visual properties. Run remaining PDF cleanup/load checks, `git diff --check`, and Graphify update.

## Validation and acceptance
- [x] Focused tests pass: 4 table tests and 8 writer tests, zero failures/skips.
- [x] Real PDF compiled/generated and visually inspected with Poppler.
- [x] Width distribution, full wrapped text and multipage headers remain intact.
- [x] Final diff reviewed for scope and resource handling.

## Risks and rollback
Larger typography changes page breaks and wraps more text; retain stretch behavior and exercise long values. Exact font/logo and unequal reference widths remain constrained by reuse and sizing requirements. Rollback only this task's hunks/resources, never pre-existing edits.

## Progress
- [x] Inspected source, local changes, assets, tests and export flow.
- [x] User explicitly requests explanation then implementation; visual direction is the supplied image.
- [x] Created dedicated branch `codex/pdf-reference-layout`.
- [x] Implement presentation; initial five existing PDF tests passed and first/long-content page rendered.
- [x] Final presentation verified after reducing vertical cell padding to fit wrapped reference rows.
- [x] Rendered reference and continuation page; reviewed final diff and updated Graphify.

## Surprises & Discoveries
- Existing Burger King changes predate this task; writer identity and SVG are retained.
- Available logo has a cream backing absent from the reference.
- First reference fixture produced two pages because equal-width restaurant cells wrap. Reduced vertical cell padding from 6pt to 3pt; retesting the one-page reference acceptance.
- Maven requires execution outside the restricted sandbox to access the dependency cache. Initial invocation at root had no POM; subsequent commands run inside RHIS. An attempted direct javac check was blocked by cache permissions; Maven remains the authoritative validation.
- OpenPDF exposes the landscape page as 595x842 with rotation 90; the test uses getPageSizeWithRotation (842x595). No report orientation change was needed.

## Decision Log
- Preserve equal-width algorithm rather than hardcode the eight sample columns.
- Retain DejaVu Sans font extension for portability and Unicode.
- Footer artwork uses SVG paths, no bitmap dependency.
- Sampled reference colors supersede the earlier branding palette: title #BF1509, header #BF1801, rows #F7EEE1, footer #F7EDDF. Title reduced to 25pt to match its visible width with DejaVu Sans.

## Outcomes & Retrospective
### Follow-up: ten complete rows per continuation page
The user's latest request supersedes the variable-height layout below: 10 rows on continuation pages and no record split across pages. Local correction: detail height 47pt (474pt available / 10), SplitType.PREVENT, and TextAdjust.SCALE_FONT. Cell text shrinks only when required, preserving values and equal-width columns. First page accommodates 7 rows, continuation pages 10, final page the remainder. Very long arbitrary text can become small; fixed count and complete content require this typography trade-off.

Regression fixture `keepsPdfRowsTogetherWithTenRowsOnContinuationPages` uses 29 ten-column records with full UUIDs and checks page counts 7/10/10/2 plus complete UUID and employee identifier on each expected page. Writes `RHIS/target/pdf-qa/ten-rows-per-page.pdf`. The earlier one-page reference fixture now expects two pages under the new user requirement.

Follow-up validation passed: `mvn -o '-Dtest=JasperDynamicTableConfigurerTest,ReportExportWriterTest#*Pdf*+cleansJasperSwapFilesAfterFailureAndCancellation' test -q` from RHIS, 12 tests total, zero failures/errors/skips. Includes existing long Unicode text preservation. Poppler rendered pages 1, 2 and 4; visually inspected 7 first-page rows, 10 complete continuation-page rows and the final remainder. `git diff --check` passed. Delivered synthetic sample: `output/pdf/rapport-10-lignes-par-page.pdf`. No production changes beyond the three Jasper table settings.

### Previous visual-reference milestone
Delivered: reference geometry, sampled red/cream colors, type-based alignment, unchanged equal-width algorithm and expressions, alternating row style, full-bleed SVG footer with fail-fast loading. Reference fixture fits on one page. Existing logo artwork retained.

Final successful command (from RHIS):
```powershell
mvn -o '-Dtest=JasperDynamicTableConfigurerTest,ReportExportWriterTest#*Pdf*+cleansJasperSwapFilesAfterFailureAndCancellation+exportsFiftyThousandRowsWithoutTruncationAndRecordsPeakHeap' test -q
```
Result: 12 tests, zero failures/errors/skips. Includes empty PDF, Unicode/wrapping, null values, row ordering, repeated headers/footer, concurrent exports, cancellation/cleanup, reference fixture, and 50,000-row load verification. Measured incremental peak heap for the load PDF was 155,075,808 bytes; this is an observation, not a production memory limit.

Reproduce just the reference PDF:
```powershell
mvn -o '-Dtest=ReportExportWriterTest#writesReferenceLayoutPdf' test -q
pdftoppm -f 1 -singlefile -scale-to 1491 -png target/pdf-qa/reference-layout.pdf target/pdf-qa/reference-layout
```
The fixture writes `RHIS/target/pdf-qa/reference-layout.pdf`; delivered copy: `output/pdf/rapport-burger-king-reference.pdf` (synthetic sample data only). Final copy rendered and inspected. Existing `target/pdf-qa/GeneratePdfQa.java` was also run with the final classes/resources to generate a ten-page long-content sample; first-page stretching and page 2 repeated headers/footer were inspected.

`git diff --check` passed. `graphify update .` succeeded: 3098 nodes, 6242 edges, 199 communities; it reported unchanged-label/filter warnings, no code extraction failure. Graph output was already dirty before this task.

Remaining differences: equal-width columns wrap more text (including long unbroken words) than the unequal-width reference, DejaVu Sans metrics differ, reused logo has cream backing, decorative curves are approximated with flat SVG fills rather than the image's slight shading. Generation timestamp remains live. Larger rows change page breaks while preserving the pagination mechanism. No frontend/browser end-to-end flow or full backend suite rerun; changes are limited to PDF presentation. No dependency, commit or merge added. No further action required for the scoped implementation.
