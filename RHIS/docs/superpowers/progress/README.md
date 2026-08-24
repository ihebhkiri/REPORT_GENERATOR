# Progress and Context Handoffs

Use a progress note for complex tasks that span sessions, repositories, or natural context boundaries. Create `YYYY-MM-DD-<task-slug>.md` and update it before pausing or moving from research to planning or from planning to implementation.

Replace every angle-bracket field in an active note.

```markdown
# <Task> Progress

**Updated:** <YYYY-MM-DD HH:mm timezone>
**Task slug:** <task-slug>
**Current phase:** Research/Design/Plan/Implementation/Validation/Review

## Intended Outcome

<One paragraph describing the user-visible or operational result.>

## Repository State

| Repository | Branch and checkout | Baseline | Current dirty files |
|---|---|---|---|
| <repo> | <branch and absolute project path> | <commit SHA> | <paths, including pre-existing ownership> |

## Completed

- <Verified outcome with paths or commands>

## Decisions

- <Decision, reason, and human approval if applicable>

## Validation State

| Command or check | Result | Meaning |
|---|---|---|
| `<exact command>` | Pass/Fail/Skipped | <coverage and limitation> |

## Remaining Work

1. <Next concrete action>
2. <Following action>

## Blockers and Risks

- <Blocker, owner, and required resolution, or "None">
- <Known risk that remains after current validation>

## Next Safe Action

<One precise action that can be taken without rediscovering context.>
```

Keep the note compact and factual. It is a handoff contract, not a replacement for the design spec, implementation plan, or Git history.
