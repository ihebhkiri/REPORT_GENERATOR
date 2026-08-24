# RHIS Frontend Agent Instructions

## Scope

These instructions apply to the RHIS Angular frontend repository. The application uses Angular 20 standalone APIs, TypeScript 5.9, RxJS 7.8, typed reactive forms, PrimeNG 20, Tailwind/PostCSS, HTML, and SCSS.

The Spring Boot backend is a separate Git repository. For a full-stack task, use the same task slug in both repositories and keep the canonical cross-repository research, spec, plan, and progress note in the backend `docs/superpowers` tree.

## Project Map

- `src/app/features/auth`: authentication models, services, and login UI.
- `src/app/features/rapports`: dataset selection, report configuration, preview, generation, and export.
- `src/app/features/data-admin`: dataset administration UI.
- `src/app/shared`: reusable UI with a real cross-feature responsibility.
- `src/environments`: environment-specific configuration selected by Angular build configuration.
- `public`: static assets.
- `docs/superpowers/specs` and `docs/superpowers/plans`: frontend-only design and implementation records.

Read the relevant backend `docs/flows` document before changing a full-stack flow. Verify API behavior in backend code rather than inferring contracts solely from frontend models.

## Operating Rules

- Inspect `git status`, the relevant diff, tests, and recent commits before editing.
- Preserve unrelated and pre-existing changes. Never reset, overwrite, delete, or broadly reformat them.
- Do not commit, push, merge, rebase, or open a pull request unless explicitly asked.
- Keep changes small and feature-oriented. Do not create facades, stores, wrappers, shared components, or generic services without a concrete responsibility or reuse case.
- Ask for human review when UI behavior, API contracts, authorization expectations, destructive actions, or cross-repository responsibilities are ambiguous.
- Do not use unattended agent loops. Use subagents only when explicitly requested and for bounded independent work.

## Proportional Workflow

- **Trivial:** inspect, implement the smallest change, run the focused test, and review the diff.
- **Medium:** confirm UI and API contracts; write a compact plan when several components or services are involved; implement; run targeted tests and the build.
- **Complex or full-stack:** research repository evidence first, obtain human review for material ambiguity, write or update the design and plan, then implement testable vertical slices.

Use fresh context at phase boundaries or when the current context becomes noisy. Before pausing a complex task, update the backend cross-repository progress note or the appropriate frontend-only progress note with decisions, validation state, dirty files, and the next safe action. Do not rely on automatic compaction for correctness.

## Angular and TypeScript Rules

- Preserve standalone Angular architecture and organize code by business feature.
- Use Signals for synchronous local UI state and `computed()` for derived state.
- Use `effect()` only for real side effects, not as a substitute for derived state.
- Use RxJS for HTTP flows, cancellation, debounce, retry policies, polling, and asynchronous composition.
- Prefer typed reactive forms for non-trivial forms. Keep validation rules explicit and test error states.
- Use `input()`, `output()`, `@if`, and `@for` where they fit existing Angular 20 code.
- Track repeated domain items by a stable identifier, not array position.
- Keep business logic, subscriptions, and expensive calculations out of templates.
- Avoid `any`. Model API payloads, UI state, and nullability explicitly.
- Keep TypeScript, HTML, and SCSS separate unless the existing component is genuinely simpler inline.
- Import the neutral environment entry point, not `environment.development` directly.

## UI, Accessibility, and Styling

- Use semantic HTML, associated labels, keyboard-accessible controls, visible focus, and meaningful button names.
- Preserve loading, empty, error, disabled, and success states for asynchronous actions.
- Do not rely on color alone to communicate state.
- Prefer existing PrimeNG components and project tokens before adding custom widgets or global styles.
- Keep component styles scoped and avoid increasing global specificity without a documented need.
- Extract a component only for meaningful responsibility, reuse, isolated logic, or testability.

## API and Security

- Treat backend authorization as authoritative. Route guards and hidden controls improve UX but do not enforce access.
- Preserve cookie credential and refresh semantics deliberately. Do not log tokens, credentials, personal data, or report contents.
- Keep request, response, error, and polling models aligned with verified backend contracts.
- Do not silently convert backend failures into empty success states.

## Validation

Run focused tests first, then widen according to risk.

```powershell
# Focused test in the current Angular workspace
npm.cmd test -- --watch=false --include="src/app/features/<feature>/<file>.spec.ts"

# Complete suite
npm.cmd test -- --watch=false

# Production build
npm.cmd run build
```

Report exact results, including browser-launch issues, skipped coverage, build-budget warnings, and pre-existing failures. Do not claim the frontend is green based only on a successful build.

For full-stack changes, also verify the affected backend tests and the user-visible flow across both applications.

## Completion Standard

1. Review the final diff for unintended changes.
2. Confirm loading, empty, error, disabled, success, and accessibility behavior relevant to the change.
3. Report exact test and build commands with their outcomes.
4. List unresolved risks or manual checks explicitly.
5. Confirm unrelated user changes remain untouched.
