# RHIS End-to-End Flow Documentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Document every implemented reporting and authentication flow from Angular interaction to Spring Boot, persistence/infrastructure, and the UI return path.

**Architecture:** Treat the sibling Angular application and this Spring Boot application as one observable system, but cite their files separately. Split the documentation by user-visible flow and keep shared report-definition resolution explicit rather than duplicating it inaccurately.

**Tech Stack:** Angular, TypeScript, Signals, RxJS, Spring Boot, Spring Security, Spring Data JPA, Spring JDBC, PostgreSQL, Apache POI, JasperReports.

## Global Constraints

- Documentation only; do not modify application code or behavior.
- Follow actual references and call sites, not class names or design documents.
- Mark missing flows and contradictions explicitly.
- Preserve all pre-existing changes in both dirty worktrees.

---

### Task 1: Map implemented flows

**Files:**
- Read: `../Frontend/Rhis_report_gen/src/app/**`
- Read: `src/main/java/RHIS/com/RHIS/**`
- Read: `src/test/java/RHIS/com/RHIS/**`

- [x] Inventory routes, UI triggers, Signals/forms, HTTP services, controllers, services, repositories, SQL, workers, storage, security, and exceptions.
- [x] Trace references for catalogue loading, report definition, preview, generation, export/download, login, and refresh.
- [x] Record absent features rather than inventing flows.

### Task 2: Write flow documents

**Files:**
- Create: `docs/flows/README.md`
- Create: `docs/flows/01-dataset-selection-and-configuration-load.md`
- Create: `docs/flows/02-report-definition-fields-filters-sorts.md`
- Create: `docs/flows/03-report-preview.md`
- Create: `docs/flows/04-report-generation.md`
- Create: `docs/flows/05-report-export-and-download.md`
- Create: `docs/flows/06-authentication-and-refresh.md`

- [x] Write exact call chains, participant tables, data transformations, transactions, database access, errors, Mermaid diagrams, business summaries, and points of confusion.
- [x] Link every flow to its production files.

### Task 3: Verify documentation

**Files:**
- Review: `docs/flows/*.md`

- [x] Run the Angular test suite.
- [x] Run the targeted backend report tests.
- [x] Check links, referenced symbols, Mermaid blocks, whitespace, and final Git scope.
