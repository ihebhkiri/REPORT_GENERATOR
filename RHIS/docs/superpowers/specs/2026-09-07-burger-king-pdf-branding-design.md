# Burger King PDF branding design

## Objective

Replace the RHIS identity in generated PDF reports with Burger King branding and produce a restrained, professional document that remains readable for wide business tables.

## Scope

- Replace the embedded RHIS logo with the existing `Frontend/Rhis_report_gen/public/Burger_King_logo.svg` artwork, packaged as a backend report resource in a JasperReports-compatible format.
- Replace the visible title `Rapport RHIS` with `Rapport Burger King`.
- Replace PDF metadata creator `RHIS` with `Burger King`.
- Apply the logo palette to the report template:
  - Burger King red `#D62300` for column headers and the main title;
  - Burger King orange `#FF8732` for the title separator/accent;
  - Burger King cream `#F5EBDC` as a restrained secondary surface;
  - dark neutral text and white data cells for contrast and readability.
- Preserve report data, landscape format, dynamic column sizing, pagination, dates, export progress and download behavior.

## Design

The title band keeps the current compact structure: logo on the left, report title and generation date aligned on the right, followed by a thin orange separator. The logo retains its proportions and is not stretched.

Column headers use Burger King red with white bold text. Data cells stay neutral and lightly bordered so the brand colors support hierarchy without reducing table readability. Cream is used only where it improves visual grouping; the report remains mostly white to keep dense exports professional and printer-friendly.

## Implementation boundary

Use the existing JasperReports template and `PdfReportExportWriter`. Do not add a dependency, introduce a theme abstraction, change the frontend export workflow or modify XLSX branding. Package one Burger King logo resource beside the report template and update the existing resource/title/metadata constants.

## Error handling

Keep the current fail-fast behavior when a report resource is missing. The PDF export must still fail with `Missing report resource` rather than emit an incomplete document.

## Verification

- Extend the existing PDF writer test to verify successful PDF generation with the Burger King resource and updated metadata/title where the current test utilities expose them.
- Compile the Jasper template through the existing focused backend tests.
- Render or inspect a generated PDF to confirm the logo, palette, title, table readability and page footer.
- Run the focused report export test suite and the backend package/test command appropriate to the module.

## Acceptance criteria

1. A newly generated PDF displays the Burger King logo instead of RHIS.
2. The visible report title is `Rapport Burger King`.
3. PDF creator metadata is `Burger King`.
4. Headers and accents use `#D62300`, `#FF8732` and `#F5EBDC` consistently.
5. Data remains legible, columns remain dynamically sized and multi-page reports retain page numbers.
6. No frontend, XLSX or report-generation contract changes are introduced.
