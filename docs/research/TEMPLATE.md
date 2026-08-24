# Research: <topic>

Date: YYYY-MM-DD  
Status: Draft | Ready for planning  
Related issue: <link or N/A>

## Question

State the concrete question this research must answer. Do not propose implementation before the current behavior is understood.

## Scope

Included:

- <area>

Excluded:

- <area>

## Verified current behavior

Describe the current flow from its entry point to its observable result.

For every important statement, reference repository evidence:

- `path/to/file.ext: SymbolName` — what it proves.

## Data and control flow

Describe:

- inputs;
- transformations and business rules;
- persistence or external calls;
- outputs and side effects;
- authorization and transaction boundaries when relevant.

## Invariants and constraints

List rules that must remain true, including security, data integrity, compatibility and UI constraints.

## Existing tests and validation commands

- `<command>` — coverage or expected result.

If a command was not run, say so.

## Risks and unknowns

| Item | Type | Impact | How to resolve |
|---|---|---|---|
| <item> | Risk / Unknown / Assumption | <impact> | <check> |

## Relevant files and symbols

- `path/to/file.ext` — reason it matters.

## Conclusions for planning

Summarize verified facts that constrain the implementation. Keep proposed choices separate from facts.

## Open questions

List only questions whose answers materially change the plan.
