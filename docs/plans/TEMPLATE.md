# <Action-oriented plan title>

This ExecPlan is a living document governed by `.agent/PLANS.md`. Keep `Progress`, `Surprises & Discoveries`, `Decision Log` and `Outcomes & Retrospective` current throughout implementation.

Date: YYYY-MM-DD  
Status: Draft | Awaiting approval | Approved | In progress | Completed  
Research: `docs/research/YYYY-MM-DD-topic.md`  
Related issue: <link or N/A>

## Purpose and observable outcome

Explain why this work matters and what a user or operator will be able to observe after completion.

## Scope and non-goals

In scope:

- <behavior>

Non-goals:

- <behavior intentionally unchanged>

## Current behavior

Summarize the verified flow and important invariants. Reference the research document and key source symbols.

## Proposed approach

Describe the smallest implementation that meets the objective. Explain important trade-offs and why existing architecture is preserved or changed.

## Affected files and symbols

- `path/to/file.ext`
  - `SymbolName`: intended change.

Do not list speculative files.

## Milestone 1: <coherent result>

Result:

Describe what will exist after this milestone.

Work:

- exact changes.

Validation:

- Command: `<command>`
- Expected observation: <result visible to a developer or user>.

## Milestone 2: <coherent result>

Result:

<description>

Work:

- <exact changes>

Validation:

- Command: `<command>`
- Expected observation: <observable result>.

Add milestones only when independently useful and verifiable.

## Validation and acceptance

Automated:

- [ ] `<focused test command>`
- [ ] `<broader check when justified>`

Manual:

- [ ] <steps and expected result>

Acceptance criteria:

- [ ] <observable behavior>
- [ ] <important behavior preserved>
- [ ] <security or data-integrity criterion when relevant>

## Risks and rollback

| Risk | Prevention/Detection | Rollback |
|---|---|---|
| <risk> | <check> | <recovery> |

## Progress

- [ ] YYYY-MM-DD HH:mm — Plan drafted.
- [ ] Plan approved.
- [ ] Milestone 1 completed and verified.
- [ ] Milestone 2 completed and verified.
- [ ] Final review completed.

Update this list at every stopping point. Split partially completed entries rather than marking them complete.

## Surprises & Discoveries

- YYYY-MM-DD — <discovery and evidence>.

## Decision Log

- YYYY-MM-DD — **Decision:** <decision>.
  - Reason: <why>.
  - Alternatives rejected: <alternatives and reason>.

## Outcomes & Retrospective

Complete after implementation:

- Delivered behavior:
- Commands run and results:
- Deviations from the approved plan:
- Remaining risks or unverified checks:
- Required follow-up:
- Exact next action if incomplete:
