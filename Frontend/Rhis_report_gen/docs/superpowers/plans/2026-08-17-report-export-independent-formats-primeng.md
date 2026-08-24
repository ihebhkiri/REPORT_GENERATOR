# Independent report formats with PrimeNG implementation plan

**Goal:** Move only the selected PDF or Excel card from Formats to Download and render the complete workflow with PrimeNG and PrimeIcons.

## Implementation

- Add PrimeIcons 8 and import its stylesheet globally.
- Replace the global export stage with selected, creating, error, available, and download state per `ReportExportFormat`.
- Select a format before its create request, restore selection from saved export IDs, and keep errors/retries local to that format.
- Use PrimeNG Stepper for the report wizard and Timeline, Card, Avatar, Button, ProgressBar, Message, and Tag for the export workflow.
- Implement option B: disabled `Exporter` while preparing, enabled `Télécharger` at `READY`, and enabled `Réessayer` after failure.
- Keep the existing API, models, polling, cleanup, Blob download, routes, and navigation unchanged.

## Verification

- Cover PDF-only, Excel-only, both formats, request failure, polling failure, retry, ready transition, and saved-ID restoration through the component DOM and service seams.
- Run the focused Export spec, the complete Angular test suite, and the production build.
- Keep the component stylesheet under the Angular component-style budget; do not change budget thresholds.
