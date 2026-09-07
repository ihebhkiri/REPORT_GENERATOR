# Research: PDF reference layout

Date: 2026-09-07
Status: Ready for planning

## Question and scope
Identify the existing PDF presentation pipeline; frontend, XLSX, API and business logic are excluded.

## Verified current behavior
- `RHIS/src/main/java/RHIS/com/RHIS/report/export/PdfReportExportWriter.java`: streams snapshot values unchanged as strings, loads required template/logo with fail-fast, compiles Jasper, uses swap virtualization, exports PDF and reports progress.
- `JasperDynamicTableConfigurer.configure`: sole production caller is the writer. Equal widths plus remainder distribute the complete printable width. Header 24pt, detail 18pt; FLOAT, CONTAINER_HEIGHT and STRETCH_HEIGHT retain wrapped values.
- `RHIS/src/main/resources/reports/report-export-base.jrxml`: A4 landscape, DejaVu Sans, title/date in title band, page number in footer. Existing local edits already install Burger King branding.
- `RHIS/docs/flows/05-report-export-and-download.md`: asynchronous jobs and download surround the writer; no changes required there.
- Installed JasperReports/core/PDF/fonts version: 7.0.7. SVG rendering already works in the existing pipeline.

## Invariants and constraints
Retain snapshot expressions, width distribution, automatic pagination, dates, cancellation, progress, resource cleanup and API contracts. Existing local changes must survive.

## Reference observations
1491x1055 image, approximately A4 landscape: margins 17pt, logo around (46,11) with 80x87pt visible artwork, right title around y42 at 28pt, date around y75 at 16pt, orange rule y109, table y130. Header 37pt, rows 32pt. White/cream alternating rows and gray borders. Page number above a cream full-width 48pt footer with italic slogan and orange/red curved artwork.

## Tests and risks
Existing `JasperDynamicTableConfigurerTest` checks width remainder, fields and stretching. `ReportExportWriterTest` covers PDF pagination, Unicode, empty/null rows, concurrency, cleanup and load. Not yet run in this session. Larger rows necessarily alter page breaks; unequal reference widths conflict with preserving the current equal-width algorithm. Exact font and logo artwork differ from available resources.

## Conclusions
Template and table geometry own the changes. A small footer SVG alongside the existing Jasper assets can reproduce the curved decoration; loading it must use the writer's existing requiredResource method. No persistence or authorization changes.
