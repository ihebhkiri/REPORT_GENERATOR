# RHIS Backend Agent Instructions

## Scope

These instructions apply to the RHIS backend repository. This is a Java 17, Spring Boot 4.1, Spring Security, JPA/Hibernate, PostgreSQL, Maven, Apache POI, and JasperReports application.

The Angular frontend is a separate Git repository at `../Frontend/Rhis_report_gen`. Do not treat the frontend as a backend subdirectory. For full-stack work, use the same task slug in both repositories and keep the canonical cross-repository spec, plan, and progress note here.

## Project Map and Sources of Truth

- `src/main/java/RHIS/com/RHIS/auth`: authentication, JWT, cookies, and Spring Security.
- `src/main/java/RHIS/com/RHIS/dataset`: dataset metadata, fields, relations, and bootstrap.
- `src/main/java/RHIS/com/RHIS/report`: report definition, preview, generation, and export.
- `src/main/java/RHIS/com/RHIS/workforce`: workforce domain.
- `src/main/java/RHIS/com/RHIS/core`: shared infrastructure and cross-cutting concerns.
- `src/test`: automated backend tests.
- `docs/flows`: current end-to-end behavior; read the relevant flow before changing it.
- `docs/superpowers/specs`: approved design decisions.
- `docs/superpowers/research`: evidence gathered for complex tasks.
- `docs/superpowers/plans`: executable implementation plans.
- `docs/superpowers/progress`: handoff state for long-running work.

Prefer repository evidence and executable behavior over assumptions. If documentation and code disagree, report the mismatch before choosing which behavior to preserve.

## Operating Rules

- Inspect `git status`, the relevant diff, tests, and recent commits before editing.
- Preserve unrelated and pre-existing changes. Never reset, overwrite, delete, or reformat them.
- Do not commit, push, merge, rebase, or open a pull request unless the user explicitly asks.
- Keep the change scoped. Do not add speculative abstractions, dependencies, layers, or refactors.
- Distinguish facts observed in the repository, assumptions, and recommendations.
- Ask for human review when a decision changes business behavior, API contracts, authorization, data integrity, schema semantics, or cross-repository responsibilities.
- Do not use unattended agent loops. Use subagents only when explicitly requested and only for bounded, independent work.

## Proportional Workflow

Classify the task before acting:

- **Trivial:** inspect, make the smallest change, run focused validation, and review the diff. No persistent research or plan document is required.
- **Medium:** confirm scope and contracts; write a compact plan when several files or layers are involved; implement; run targeted tests and the relevant build; review the diff.
- **Complex or cross-repository:** research first, record evidence in `docs/superpowers/research/YYYY-MM-DD-<task-slug>.md`, obtain human review for material ambiguity, write or update a spec and an implementation plan, then deliver testable vertical slices.

Use fresh context at natural boundaries such as research-to-plan or plan-to-implementation, or when the current context becomes noisy. Before pausing or switching context, update `docs/superpowers/progress/YYYY-MM-DD-<task-slug>.md` with completed work, decisions, exact validation state, dirty files, blockers, and the next action. Do not rely on automatic compaction as a correctness mechanism.

Workflow templates live in:

- `docs/superpowers/research/README.md`
- `docs/superpowers/plans/README.md`
- `docs/superpowers/progress/README.md`

## Java and Spring Rules

- Prefer constructor injection and focused classes.
- Keep controllers thin. Validate transport input at the boundary and keep business rules in the appropriate service or domain code.
- Define transaction boundaries deliberately. Avoid remote calls or long export work while holding a database transaction unless required and documented.
- Enforce authorization on the backend. Frontend visibility is never an authorization boundary.
- Avoid exposing JPA entities through APIs when it couples persistence, lazy loading, or serialization to the contract.
- Prevent N+1 queries and uncontrolled lazy loading. Make required fetch behavior explicit and test query semantics when cardinality matters.
- Keep exception handling consistent with existing application conventions. Preserve useful causes without returning sensitive details.
- Close JDBC, stream, workbook, and report resources deterministically, preferably with try-with-resources.
- For asynchronous report generation, make job state transitions, failure recording, cleanup, and retry assumptions explicit.
- Do not add an interface, DTO, mapper, repository, factory, or facade unless it solves a concrete responsibility, duplication, change-isolation, or testability problem.

## PostgreSQL and SQL Rules

- Verify schema qualification, nullability, uniqueness, foreign keys, and multi-schema behavior.
- Explain join cardinality and detect row multiplication. Never add `DISTINCT` merely to hide an incorrect join.
- Parameterize values. Identifiers that cannot be bound must be validated against trusted metadata before SQL assembly.
- Treat correctness and performance as separate questions. Recommend indexes only after identifying the query shape and selectivity.
- Schema changes require an explicit migration strategy. Do not rely on destructive Hibernate schema generation for production behavior.

## Security and Configuration

- Never add or expose real credentials, JWT secrets, tokens, personal data, or generated reports.
- Preserve cookie and CORS semantics deliberately when modifying authentication.
- Validate ownership and authorization for report status, download, preview, and metadata endpoints.
- When recent framework behavior matters, verify it against official documentation for the exact project version.

## Validation

Run the smallest useful check first, then widen validation according to risk.

```powershell
# One test class
mvn -Dtest=ReportPreviewServiceTest test

# Complete backend suite
mvn test

# Build artifact after relevant changes
mvn package
```

PostgreSQL Testcontainers tests require Docker. If Docker is unavailable, report skipped integration coverage explicitly. Never claim a green baseline based only on compilation or a partial suite. For a pre-existing failure, show that it existed before the change or clearly label it as unresolved baseline behavior.

For report or export changes, also verify the affected flow in `docs/flows`, SQL cardinality, generated file cleanup, and format-specific limits or resource handling.

## Completion Standard

Before reporting completion:

1. Review the final diff for unintended changes.
2. Confirm that the requested behavior and relevant error paths are covered.
3. Report the exact commands run and their outcomes, including skips and failures.
4. List remaining risks or manual checks without presenting them as completed.
5. Confirm that unrelated user changes remain untouched.
