# Implementation Plans

Existing dated plans remain historical records. Create a new plan at `YYYY-MM-DD-<task-slug>.md` for complex tasks and for medium tasks whose sequencing or contracts are not obvious.

Plans must be executable by an engineer who has not read the research session. Use exact paths, symbols, commands, expected outcomes, and explicit dependencies between tasks. Prefer vertical slices that produce independently reviewable behavior.

```markdown
# <Task> Implementation Plan

**Goal:** <single verifiable outcome>

**Architecture:** <two or three sentences describing the chosen approach and boundaries>

**Tech stack:** <relevant project technologies only>

## Global Constraints

- <behavior or contract that every task must preserve>
- <dependency, version, security, or compatibility constraint>

## File Map

| File | Action | Responsibility |
|---|---|---|
| `<exact/path>` | Create/Modify/Test | <one clear responsibility> |

### Task 1: <Independent deliverable>

**Consumes:** <existing contract or earlier task output>

**Produces:** <exact class, method, endpoint, type, or behavior>

- [ ] Add or update the focused test for `<exact behavior>`.
- [ ] Run `<exact targeted command>` and record the expected failure or baseline.
- [ ] Implement the smallest change in `<exact files and symbols>`.
- [ ] Run `<exact targeted command>` and expect `<exact outcome>`.
- [ ] Review the task diff for correctness, security, data integrity, maintainability, performance, and style.

## Integrated Validation

- [ ] Run `<complete relevant suite>`.
- [ ] Run `<build command>`.
- [ ] Perform `<manual or cross-repository check>`.
- [ ] Record skips, pre-existing failures, and remaining risk.

## Rollback

<Files or state to restore and any data compatibility constraint.>

## Completion Criteria

- <Observable acceptance criterion>
```

Do not use vague steps such as “add error handling” or “write tests.” Name the error, the expected response or state, and the focused test. Do not add mandatory commits to a plan unless the user has authorized commits.
