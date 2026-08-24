# XLSX Temporal Snapshot Compatibility Design

## Context

The asynchronous report snapshot currently serializes Java temporal values as
JSON arrays. For example, a `LocalDate` becomes `[2022, 1, 3]`. The XLSX writer
expects an ISO-8601 string and calls `LocalDate.parse(value.toString())`, which
turns the array into the invalid text `"[2022, 1, 3]"`. The export consequently
fails before writing its first data row.

The later `Stream closed` warning emitted by `SXSSFWorkbook` is cleanup noise
caused by the original date parsing exception, not a separate failure.

## Decision

New snapshots will use one canonical representation for temporal values:
ISO-8601 strings. The report Jackson mapper will register Java Time support and
disable timestamp/array serialization for dates.

The XLSX writer will remain backward-compatible with snapshots created before
this correction. Its temporal conversion will accept both:

- ISO-8601 strings;
- the existing numeric arrays for `DATE`, `DATE_TIME`, and `OFFSET_DATE_TIME`.

The conversion remains confined to the XLSX adapter. Report definitions, SQL
generation, snapshot column metadata, REST contracts, and PDF behavior do not
change.

## Error handling

Malformed temporal values still fail the export instead of being silently
written as misleading text. The worker continues to remove the partial XLSX and
records the sanitized public error code `EXPORT_FAILED`; the detailed exception
remains server-side.

## Verification

The public test seam is `ReportExportWriter.write(InputStream, OutputStream,
IntConsumer)` followed by opening the generated workbook through Apache POI.

The regression test will build a snapshot containing the real failing value
`[2022, 1, 3]`, export it, and assert that Excel contains 3 January 2022 as a
typed date cell. A second assertion will verify the canonical ISO representation
produced for new snapshots. Existing typed-cell, PDF, streaming, 50,000-row,
integration, and packaging tests must remain green.

## Scope

No dependency, database migration, endpoint, frontend, or job-state change is
required. Existing snapshots remain readable during their short retention
period, while all newly generated snapshots adopt the canonical ISO format.
