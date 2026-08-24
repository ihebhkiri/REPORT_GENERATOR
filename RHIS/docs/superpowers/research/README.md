# Research Notes

Use a research note for complex, ambiguous, or cross-repository work. It is optional for medium tasks and unnecessary for trivial changes.

Copy the template below to `YYYY-MM-DD-<task-slug>.md`. Replace every angle-bracket field; do not leave unresolved template text in an active note.

```markdown
# <Task> Research

**Date:** <YYYY-MM-DD>
**Task slug:** <task-slug>
**Repositories and branches:** <repo: branch>

## Question

<What must be understood or decided before implementation?>

## Scope

- In: <affected behavior, modules, and flows>
- Out: <explicit exclusions>

## Facts

- <Repository-backed fact with file path, symbol, command, or test evidence>

## Relevant Execution Path

1. <Entry point>
2. <Important call or state transition>
3. <Persistence, external effect, or output>

## Existing Contracts

- API: <request, response, authorization, and error contract>
- Data: <schema, cardinality, nullability, and transaction constraints>
- Frontend: <state, route, form, and user-visible behavior>

## Validation Evidence

| Check | Command or inspection | Result |
|---|---|---|
| <claim tested> | `<exact command or path>` | <pass, fail, skipped, or observation> |

## Risks and Unknowns

- Risk: <impact and evidence>
- Unknown: <question that still needs an answer>

## Options

1. **<Option>** — <trade-off>
2. **<Option>** — <trade-off>

## Recommendation

<Recommended option, why it is the smallest production-quality choice, and what would change the decision.>

## Human Review Gate

<Exact business, API, security, data, or architecture decision requiring approval, or "None".>
```

Research records evidence, not a diary of every command. Separate observed facts from inferences and recommendations.
