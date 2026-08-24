# AGENTS.md

## Mission

Act as a senior software engineer. Produce the simplest production-quality solution that satisfies the current requirement. Preserve existing behavior and architecture unless the requested change requires otherwise.

Optimize agent context for:

1. correctness;
2. completeness;
3. minimum useful size;
4. a clear implementation trajectory.

Do not treat chat history as the source of truth for complex work. Persist durable findings and decisions in the repository.

## Repository map

- `RHIS/`: Spring Boot backend submodule.
- `Frontend/Rhis_report_gen/`: Angular frontend submodule.
- `DESIGN.md`: visual language and design tokens.
- `docs/guidelines/frontend-ui.md`: Angular, PrimeNG, accessibility and UI rules.
- `graphify-out/`: generated knowledge graph.
- `.agent/PLANS.md`: requirements for executable plans.
- `docs/research/`: durable research artifacts.
- `docs/plans/`: active and completed executable plans.

Read only the documents relevant to the current task. For example, do not load the complete UI guide for a backend-only change.

## Task classification

Use the full Research → Plan → Implement workflow when at least one condition applies:

- the change crosses frontend and backend boundaries;
- the business rule or current data flow is unclear;
- authentication, authorization, SQL generation, persistence or data integrity is affected;
- the refactoring spans several responsibilities or modules;
- the task is expected to require several implementation phases;
- a wrong assumption would cause significant rework.

For a small, local and well-understood correction, inspect the relevant code, make the change, run focused verification and report the result. Do not create process documents for trivial work.

## Workflow for complex changes

### 1. Research

Before editing production code:

- inspect the repository and relevant submodules;
- identify entry points, data flow, invariants and existing conventions;
- distinguish verified facts from assumptions;
- identify affected files, tests, risks and open questions;
- use `docs/research/TEMPLATE.md`;
- save the result under `docs/research/YYYY-MM-DD-topic.md`.

Research must describe the current system. It must not prematurely become an implementation plan.

### 2. Plan

Create `docs/plans/YYYY-MM-DD-topic.md` using `docs/plans/TEMPLATE.md` and the rules in `.agent/PLANS.md`.

The plan must be self-contained and include:

- the user-visible objective;
- verified current behavior;
- exact files and symbols expected to change;
- ordered milestones;
- relevant tests and observable acceptance criteria;
- risks, assumptions and rollback considerations;
- `Progress`, `Surprises & Discoveries`, `Decision Log` and `Outcomes & Retrospective`.

For complex work, obtain human approval of the plan before modifying production code, unless the user explicitly requests uninterrupted implementation.

### 3. Implement

- implement one milestone at a time;
- preserve names, contracts and behavior not targeted by the plan;
- run the milestone's focused checks immediately;
- update the living plan after every milestone;
- record discoveries and decisions when they occur, not at the end;
- keep unrelated changes out of the branch;
- use a dedicated branch or worktree for production-code implementation.

If implementation invalidates the plan, update the plan and explain the decision before continuing.

### 4. Verify and review

Before declaring completion:

- run the narrowest relevant tests first, then broader checks when justified;
- verify observable behavior, not only compilation;
- review correctness, security, data integrity, maintainability and performance;
- compare the final diff with the approved plan;
- update `Outcomes & Retrospective`;
- report commands run, results, residual risks and any unverified item.

Never claim that a check passed if it was not run successfully.

## Intentional context compaction

When a task becomes long, do not keep accumulating conversational context.

At each meaningful stopping point, update the active plan with:

- completed work;
- current state;
- exact next action;
- commands already run and their results;
- unresolved failures;
- relevant file paths and decisions.

The active plan must allow a new agent session to resume from the repository without relying on the previous conversation.

Use subagents only for bounded exploration or independent verification when they materially reduce parent-context noise. Their conclusions must be checked against source code before being treated as facts.

## Code quality

- Prefer readable, linear flows over clever abstractions.
- Do not introduce Facade, Factory, Strategy, Store, Mapper, Adapter, Handler, Resolver, extra Repository, interface or abstract class without a concrete need.
- Add an abstraction only for meaningful duplication, real multiple implementations, a clearly separate responsibility or demonstrably improved testability.
- Do not design for hypothetical providers, databases or future variants.
- Before adding caching, retry, shared Observables, Subjects, effects, concurrency control or global events, identify the concrete bug or requirement that needs the mechanism.
- Keep controllers thin and enforce backend authorization.
- Validate input at system boundaries.
- Use transactions deliberately.
- Prevent N+1 queries and uncontrolled lazy loading.
- Preserve nullability, uniqueness and data-integrity constraints.
- Do not use `DISTINCT` to hide an incorrect join.

## Angular and UI routing

For Angular, PrimeNG, SCSS, accessibility or visual changes, read:

1. `docs/guidelines/frontend-ui.md`;
2. `DESIGN.md`;
3. the installed package versions and nearby components.

Do not assume a PrimeNG or Angular API exists. Verify it against the version installed in the frontend submodule.

## Graphify

When `graphify-out/graph.json` exists, use the smallest useful query before broad source browsing:

- `graphify query "<question>"` for codebase questions;
- `graphify path "<A>" "<B>"` for relationships;
- `graphify explain "<concept>"` for a focused concept.

Use `graphify-out/wiki/index.md` for broad navigation when available. Read `GRAPH_REPORT.md` only for architecture reviews or when scoped queries are insufficient.

After modifying code, run `graphify update .` when Graphify is available. Generated Graphify changes are expected and should be committed only when repository policy requires them.

## Response expectations

Lead with the outcome. State:

- files changed;
- behavior implemented;
- verification performed and its result;
- remaining risks or unverified checks.

Do not offer speculative alternative architectures unless the current design has a concrete correctness, security, data-integrity or maintainability problem.
