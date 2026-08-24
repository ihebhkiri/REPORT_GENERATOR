# Codex Project Workflow Design

**Date:** 2026-08-23
**Status:** Approved for implementation
**Scope:** RHIS backend repository and the separate RHIS Angular frontend repository

## Objective

Introduce a lightweight, reviewable Codex workflow that improves context retention, planning, validation, and repository coordination without changing application behavior or adding autonomous agent loops.

## Constraints

- Do not modify backend or frontend application code as part of this setup.
- Preserve the existing `docs/superpowers/specs` and `docs/superpowers/plans` history.
- Keep backend and frontend instructions separate because they are separate Git repositories.
- Do not commit, push, open a pull request, or rewrite existing user changes.
- Do not introduce CI, database migrations, Docker, Kubernetes, or agent automation in this change.
- Prefer a proportional workflow: the documentation burden must match task complexity.

## Repository Boundaries

The backend repository is the canonical location for cross-repository feature documentation because it already contains the complete flow documentation and most existing specs and plans.

The frontend repository owns Angular-only instructions and frontend-only specs or plans. A full-stack task uses one shared task slug in both repositories, while its canonical research, specification, plan, and progress documents live in the backend repository.

Frontend instructions are installed directly in the primary frontend project, never inside the backend repository. Each repository remains responsible for its own branches and dirty state.

## Files and Responsibilities

### Backend `AGENTS.md`

The backend root instructions describe:

- Java 17, Spring Boot, Spring Security, JPA/Hibernate, PostgreSQL, Maven, Apache POI, and JasperReports conventions;
- module boundaries and the existing end-to-end flow documentation;
- commands for targeted tests, the full Maven suite, and packaging;
- deliberate transaction, authorization, schema, SQL-cardinality, and export-resource rules;
- the proportional task workflow and the rule to preserve unrelated dirty changes;
- coordination rules for full-stack work with the separate frontend repository.

### Frontend `AGENTS.md`

The frontend root instructions describe:

- Angular standalone architecture, TypeScript, Signals, RxJS, typed reactive forms, PrimeNG, and SCSS conventions;
- commands for targeted tests, the complete test suite, and production build;
- accessibility, stable tracking, template simplicity, and environment import rules;
- coordination with backend API contracts and shared task slugs;
- the same Git safety and proportional workflow rules.

### Research template

`docs/superpowers/research/README.md` defines a reusable research note containing the task question, known facts, inspected files, relevant runtime paths, risks, uncertainties, validation evidence, and recommendation. It is required for complex tasks and optional for medium tasks.

### Plan template

`docs/superpowers/plans/README.md` documents the expected plan structure without replacing existing dated plans. A plan contains the goal, scope, constraints, exact files, contracts, small implementation steps, validation commands, review gates, rollback considerations, and completion criteria.

### Progress template

`docs/superpowers/progress/README.md` defines a compact handoff record for long tasks: current outcome, completed work, remaining work, decisions, exact validation state, dirty files, blockers, and the next safe action. It supports deliberate context refresh without pretending that automated compaction is a correctness mechanism.

## Proportional Workflow

### Trivial task

Inspect the relevant code, implement the smallest change, run focused validation, and report the result. No research or plan artifact is required.

### Medium task

Confirm scope and contracts, write a compact plan when the task spans several files or layers, implement, run targeted tests plus the relevant build, and review the diff.

### Complex or cross-repository task

1. Create a task slug and record the branch and dirty state of every affected repository.
2. Write a research note grounded in repository evidence.
3. Obtain human review when requirements, architecture, or business behavior are ambiguous.
4. Write a design spec and an implementation plan.
5. Implement vertical slices that remain testable at each checkpoint.
6. Record progress before switching context or pausing.
7. Run targeted tests, complete suites, builds, and manual checks appropriate to the affected flows.
8. Review correctness, security, data integrity, maintainability, performance, and style, in that order.

Fresh contexts are used at natural phase boundaries or after a context becomes noisy. No fixed token-percentage rule is required.

## Validation Strategy

This workflow setup is documentation-only. Validation consists of:

- confirming that only intended documentation and instruction files are added;
- checking all referenced paths and commands against the repositories;
- scanning for placeholders or contradictions;
- verifying that neither repository's pre-existing application changes were altered;
- recording the known backend and frontend test baselines without claiming they pass.

The current baseline is not green: backend tests include failures related to an invalid zero `xlsxRowWindow` in directly constructed `ReportJobProperties`, and the frontend suite has one failing export-component test. PostgreSQL Testcontainers tests also require Docker. These are project findings, not failures introduced by this workflow setup.

## Deliberately Excluded Mechanisms

- unattended multi-agent factories or recursive agent loops;
- fixed context-compaction thresholds;
- mandatory specs for trivial changes;
- new abstraction layers in application code;
- CI/CD, Docker, Kubernetes, database-migration, or secret-management changes;
- automatic commits, pushes, merges, or pull requests.

## Acceptance Criteria

- Both Git repositories have concise root instructions tailored to their stack.
- The backend contains usable research, plan, and progress guidance.
- Existing specs, plans, flow documentation, and dirty application files remain intact.
- The workflow clearly distinguishes trivial, medium, and complex tasks.
- The documentation exposes the known test baseline and never represents it as green.
- No application source, test, runtime configuration, dependency, or generated artifact is changed.
