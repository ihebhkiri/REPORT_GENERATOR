# Executable plans

An ExecPlan is a self-contained, living implementation document for complex work. Its purpose is to preserve intent, verified context, decisions, progress and proof outside the conversation.

## When an ExecPlan is required

Create one for changes with significant uncertainty, multiple milestones, cross-module impact, sensitive business rules or meaningful security/data-integrity risk.

Do not create one for a local rename, an obvious one-file correction or another task whose implementation and verification are already clear.

## Required qualities

Every ExecPlan must:

- explain the user-visible outcome before implementation details;
- be understandable without access to an earlier conversation;
- distinguish facts confirmed in the repository from assumptions;
- name relevant repository paths and important symbols;
- describe how the current behavior works;
- state the intended behavior and non-goals;
- divide implementation into independently verifiable milestones;
- specify commands and observable acceptance criteria;
- remain accurate as implementation progresses;
- record decisions and unexpected discoveries;
- finish with actual outcomes, not the original prediction.

Do not paste large source files or raw tool output into a plan. Summarize only what is needed to resume and verify the work.

## Lifecycle

### Research

Start from a document under `docs/research/`. Verify the current system by reading source code, configuration and tests. Use Graphify for scoped navigation when available, but confirm important conclusions against source files.

### Draft

Create the plan under `docs/plans/YYYY-MM-DD-topic.md`. Resolve important ambiguity before implementation. If a decision needs human input, make the options and impact explicit.

### Approval

For complex work, the plan is the review boundary. Production-code changes start only after approval, unless the user explicitly asks for uninterrupted implementation.

### Execution

Implement milestone by milestone. After each milestone:

1. run its checks;
2. update `Progress`;
3. record discoveries;
4. record decisions that changed or clarified the plan;
5. write the exact next action.

A new session must be able to continue using only the working tree and the plan.

### Completion

Run final verification and update `Outcomes & Retrospective` with:

- behavior delivered;
- tests and commands actually run;
- deviations from the plan;
- remaining risks;
- follow-up work that is genuinely required.

## Required sections

Each plan must contain these sections.

### Purpose and observable outcome

Explain what changes for the user or operator and how they can see that it works.

### Scope and non-goals

Define the exact boundary. State important behavior that must remain unchanged.

### Current behavior

Describe the verified flow, entry points and invariants. Link to the research artifact.

### Proposed approach

Describe the smallest design that satisfies the objective. Explain non-obvious choices and trade-offs.

### Affected files and symbols

List only files expected to change and name the relevant classes, functions or configuration keys.

### Milestones

Each milestone must contain:

- intended result;
- exact work;
- validation command;
- observable success criterion.

A milestone should end in a coherent, testable state.

### Validation and acceptance

Include focused tests, broader regression checks when justified and manual verification for behavior that automation cannot prove.

Acceptance criteria must be observable. “The code compiles” is insufficient when a behavior can be exercised.

### Risks and rollback

Cover relevant security, data-integrity, migration, compatibility and operational risks. Describe a practical rollback when the change is risky.

### Progress

Use timestamped checkboxes:

- [ ] Not started
- [x] Completed
- [~] In progress or partially completed

Split partially completed work instead of marking an entire milestone complete.

### Surprises & Discoveries

Record unexpected repository behavior, failed assumptions and evidence.

### Decision Log

For each material decision record:

- decision;
- reason;
- alternatives rejected;
- date.

### Outcomes & Retrospective

At completion, compare the delivered result with the original objective and list verified evidence and remaining gaps.

## Change discipline

The plan is not immutable. Update it when facts change, but preserve the decision trail. Do not silently rewrite history to make implementation appear to have followed the original plan.

When the implementation diverges materially, update the approach, decision log, affected milestones and validation before continuing.
