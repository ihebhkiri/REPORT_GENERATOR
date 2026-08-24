# Frontend and UI guidelines

Read this document only for Angular, PrimeNG, HTML, SCSS, accessibility or visual-design work. `DESIGN.md` is the source of truth for project-specific visual tokens.

## Angular

- Use the standalone architecture already present in the project.
- Keep HTML, SCSS and TypeScript separated.
- Use Signals for synchronous local UI state.
- Use `computed()` for derived state.
- Use `effect()` only for real side effects, not artificial state synchronization.
- Use RxJS for HTTP flows, cancellation, debounce, asynchronous composition, retries or genuine concurrency.
- Do not turn a Promise into an Observable without a concrete benefit.
- Avoid internal `subscribe()` calls unless they implement a controlled external event or side effect.
- Use typed Reactive Forms for complex forms.
- Prefer `input()`, `output()`, `@if` and `@for` for new code when supported by the installed Angular version.
- Track loops with a stable domain identifier.
- Keep business logic and expensive transformations out of templates.
- Prepare data for PrimeNG components in TypeScript.

## Components and PrimeNG

PrimeNG is the primary component library and PrimeIcons is the icon set.

- Verify the installed version before using an API or property.
- Reuse appropriate PrimeNG components before implementing equivalents manually.
- Do not add Angular Material, Bootstrap, NG-ZORRO, DaisyUI or another component library without explicit approval.
- Do not wrap `p-button`, `p-select`, `p-dialog` or similar components without domain behavior or meaningful reuse.
- Create a component only when it has a clear UI responsibility, real reuse, significant isolated logic, independent testability or when the parent has become genuinely difficult to understand.
- Do not extract a component merely to reduce line count.
- Keep page orchestration in the page component and extract meaningful business sections.

## Visual system

Before adding values, inspect `DESIGN.md` and existing theme variables.

- Reuse existing colors, spacing, typography, radii and shadows.
- Avoid repeated hard-coded colors in components.
- Use primary, success, warning, danger and neutral colors semantically.
- Preserve the existing visual language across screens.
- Use effects, gradients and shadows only when they improve hierarchy.
- Do not introduce a new visual system for one page.

## Spacing and layout

Use a 4 px base and prefer the existing scale:

- 4 px: icon or micro spacing;
- 8 px: tightly related elements;
- 12 px: related control content;
- 16 px: standard component padding;
- 24 px: group separation;
- 32 px: section separation;
- 48–64 px: major page separation.

Recommended content constraints:

- mobile margin: 16 px;
- tablet margin: 24 px;
- desktop margin: 24–32 px;
- maximum content width: 1200–1440 px;
- touch target: at least 44 × 44 px when practical.

Use responsive layout derived from content needs. Do not force a twelve-column grid where a simpler layout is clearer.

## Typography and readability

- Normal body text: 14–16 px.
- Secondary text: 12–14 px.
- Body line height: 1.4–1.6.
- Heading line height: 1.1–1.3.
- Prefer 45–75 characters per prose line.
- Use no more than two font families and a small, consistent weight scale.
- Do not reduce text size merely to fit an overcrowded layout.

## Accessibility

Target WCAG AA:

- normal text contrast: at least 4.5:1;
- large text contrast: at least 3:1;
- controls, icons and focus indicators: at least 3:1 against adjacent colors;
- visible keyboard focus;
- semantic HTML and accessible names;
- keyboard-operable interactions;
- usable content at 200% zoom;
- no more than three flashes per second;
- respect reduced-motion preferences for non-essential animation.

Accessibility is a correctness requirement, not final visual polish.

## Forms

- Input height: generally 40–48 px.
- Label-to-control spacing: 4–8 px.
- Field-to-field spacing: 16–24 px.
- Error messages: clear, associated with the field and placed close to it.
- Validate at the UI boundary for feedback, but never rely on frontend validation for security.
- On mobile, make the principal submit action easy to reach and large enough to activate.
- Split long forms by meaningful business steps, not an arbitrary fixed field count.

## Required states

For asynchronous or data-driven screens, design the relevant:

- loading;
- empty;
- error;
- success;
- disabled;
- hover;
- focus;
- selected;
- destructive confirmation states.

Do not consider a UI task complete merely because its happy path functions.

## UI verification

Before completion, check:

- information hierarchy;
- alignment and spacing;
- responsive behavior at representative widths;
- keyboard navigation and focus;
- loading, empty and error paths;
- consistency with `DESIGN.md` and nearby screens;
- absence of unnecessary frontend complexity.

Prefer a design specific to the business task over a generic dashboard template.
