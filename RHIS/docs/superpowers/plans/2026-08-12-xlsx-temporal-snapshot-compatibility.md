# XLSX Temporal Snapshot Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore XLSX exports containing Java date/time fields while keeping already-created array/timestamp snapshots readable.

**Architecture:** Canonicalize new snapshot temporal values as ISO-8601 through the report `ObjectMapper`. At the XLSX boundary, convert both canonical strings and legacy Jackson timestamp representations into typed Apache POI cells.

**Tech Stack:** Java 17, Spring Boot 4.1, Jackson 2 Java Time module, Apache POI 5.5.1, JUnit 5, AssertJ.

## Global Constraints

- Do not change REST, SQL, database, frontend, PDF, or job-state contracts.
- Malformed temporal values must still fail the export; never silently write misleading text.
- Preserve support for snapshots created before the correction.
- Do not stage or commit because the worktree already contains unrelated user changes.

---

### Task 1: Reproduce legacy temporal values through the XLSX public seam

**Files:**
- Modify: `src/test/java/RHIS/com/RHIS/report/export/ReportExportWriterTest.java`

**Interfaces:**
- Consumes: `ReportSnapshotWriter.write(OutputStream, ReportSnapshotMetadata, RowsSource)`.
- Tests: `ReportExportWriter.write(InputStream, OutputStream, IntConsumer)` and the resulting workbook.

- [ ] **Step 1: Write the failing regression test**

Add `writesLegacyTemporalRepresentationsAsTypedXlsxCells`. Build a snapshot with
`LocalDate.of(2022, 1, 3)`, `LocalDateTime.of(2022, 1, 3, 14, 30, 15)`, and
`OffsetDateTime.parse("2022-01-03T14:30:15+02:00")` using the current mapper.
Export it through `XlsxReportExportWriter`, open it with `XSSFWorkbook`, and
assert the typed values are respectively `2022-01-03`,
`2022-01-03T14:30:15`, and `2022-01-03T12:30:15` (UTC normalization).

- [ ] **Step 2: Run the test and verify the production failure**

Run:

```powershell
mvn test "-Dtest=ReportExportWriterTest#writesLegacyTemporalRepresentationsAsTypedXlsxCells"
```

Expected: FAIL with `DateTimeParseException` for text `[2022, 1, 3]`.

---

### Task 2: Read canonical and legacy temporal representations

**Files:**
- Modify: `src/main/java/RHIS/com/RHIS/report/export/XlsxReportExportWriter.java`
- Test: `src/test/java/RHIS/com/RHIS/report/export/ReportExportWriterTest.java`

**Interfaces:**
- Produces private converters `toLocalDate(Object)`, `toLocalDateTime(Object)`, and `toUtcLocalDateTime(Object)` used only by `writeCell`.
- Accepts ISO strings, legacy numeric component lists, and the legacy numeric epoch representation emitted for `OffsetDateTime`.

- [ ] **Step 1: Implement the minimal converters**

For `DATE`, convert a three-number list with `LocalDate.of(year, month, day)`;
otherwise parse ISO text. For `DATE_TIME`, convert the legacy component list,
using zero for omitted seconds/nanoseconds; otherwise parse ISO text. For
`OFFSET_DATE_TIME`, parse ISO text, convert a numeric epoch value through
`Instant`, and support a component list with an optional offset-seconds item;
return the UTC `LocalDateTime` expected by Excel.

- [ ] **Step 2: Use the converters from `writeCell`**

Replace the direct `LocalDate.parse`, `LocalDateTime.parse`, and
`OffsetDateTime.parse` calls without changing styles or other cell types.

- [ ] **Step 3: Verify the regression test is green**

Run the Task 1 command. Expected: PASS and no `Stream closed` warning.

---

### Task 3: Canonicalize all newly-created snapshots to ISO-8601

**Files:**
- Modify: `src/main/java/RHIS/com/RHIS/report/config/ReportJobConfiguration.java`
- Create: `src/test/java/RHIS/com/RHIS/report/config/ReportJobConfigurationTest.java`

**Interfaces:**
- Produces: Spring `ObjectMapper` bean with discovered modules and `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` disabled.

- [ ] **Step 1: Write the failing configuration test**

Instantiate `ReportJobConfiguration`, obtain `reportObjectMapper()`, and assert:

```java
assertThat(mapper.writeValueAsString(LocalDate.of(2022, 1, 3)))
        .isEqualTo("\"2022-01-03\"");
```

- [ ] **Step 2: Run it red**

Run:

```powershell
mvn test "-Dtest=ReportJobConfigurationTest"
```

Expected: FAIL because the current output is `[2022,1,3]`.

- [ ] **Step 3: Disable timestamp serialization**

Change the bean to:

```java
return new ObjectMapper()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

- [ ] **Step 4: Run both focused test classes**

```powershell
mvn test "-Dtest=ReportJobConfigurationTest,ReportExportWriterTest"
```

Expected: all tests pass, including the 50,000-row export test.

---

### Task 4: Full verification

**Files:**
- No additional production files.

- [ ] **Step 1: Run the complete backend test suite**

```powershell
mvn test
```

Expected: all tests pass.

- [ ] **Step 2: Build the executable artifact**

```powershell
mvn package
```

Expected: `BUILD SUCCESS` and `target/RHIS-0.0.1-SNAPSHOT.jar` produced.

- [ ] **Step 3: Check the diff**

```powershell
git diff --check
```

Expected: no whitespace error. Existing line-ending warnings may remain.
