# Shared shell and report source visual reproduction

## Objective

Reproduce the supplied reference image as closely as possible while preserving all existing Angular behavior. The sidebar and top header become the shared shell for every route using `SharedPageLayoutComponent`; the report source page receives the reference-specific content layout.

## Verified current state

- `SharedPageLayoutComponent` already owns the header, page heading, route outlet, page copy and active navigation state for reports, configuration, export, assistant and dataset administration.
- The report source page is implemented by `RapportsComponent` and uses PrimeNG accordion and button components plus the shared `ReportStepsComponent`.
- Angular 20.3, PrimeNG 20.4, PrimeIcons 8, Tailwind 4 and the local Material Symbols font are already available.
- Dataset loading, selection, related-dataset selection and navigation are implemented with Signals and existing services. They are outside this visual change.

## Visual design

### Shared shell

`SharedPageLayoutComponent` becomes a two-column application shell: an approximately 80 px dark navy sidebar and a flexible content column. The content column contains an approximately 80 px white top header followed by the route heading and routed page content.

The sidebar uses the existing RHIS logo asset and existing icon libraries. It contains the navigation hierarchy shown in the reference, with the reports entry visibly active on report routes. Decorative or unavailable destinations remain non-navigating controls so no route is invented. Existing report, assistant and administration routes keep their current links.

The top header keeps the existing route-aware primary navigation and authentication-derived user state. It adopts the reference spacing, separators, active underline, notification affordance and compact circular profile presentation. No authentication behavior is changed.

### Page heading

The existing `PAGE_COPY` remains the source for breadcrumb, title and description. The breadcrumb is rendered as separated items with a chevron, followed by the larger dark title and muted description using the reference alignment and vertical rhythm.

### Report source content

The report page uses the available content width rather than the current narrow `max-w-6xl` constraint. The stepper is a wide bordered white panel. The source title, helper copy and dataset accordions are grouped inside one bordered white panel.

Dataset choices use a three-column desktop grid with compact rows, purple gradient icon tiles, dark labels, chevrons and subtle borders. Existing PrimeNG accordions remain responsible for expansion and selection. Selected related datasets keep the current functional expanded content.

The selected-tables summary is a separate compact bordered panel. The actions sit below it, aligned right, with a quiet outlined Cancel button and purple primary Next button. Existing disabled and click behavior is preserved.

### Responsive behavior

- Desktop: fixed-width sidebar, full header, three dataset columns.
- Tablet: compact shell and two dataset columns.
- Mobile: sidebar navigation collapses out of the content flow, header wraps or scrolls only where needed, dataset choices stack, and actions remain reachable at full or near-full width.
- Existing keyboard interaction, visible focus and reduced-motion handling remain intact.

## Files expected to change

- `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.html`
- `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.scss`
- `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.ts` only if minimal view data is needed for navigation/profile rendering
- `Frontend/Rhis_report_gen/src/app/shared/report-steps/report-steps.component.scss`
- `Frontend/Rhis_report_gen/src/app/features/rapports/pages/source_de_donnes/rapports.component.html`
- `Frontend/Rhis_report_gen/src/app/features/rapports/pages/source_de_donnes/rapports.component.scss`
- Focused component specs only if existing DOM expectations require updates

## Constraints and non-goals

- Do not change services, HTTP calls, models, routes, validation, Signals, Observables or dataset-selection behavior.
- Do not add dependencies, a design system, a store, a facade, a UI service or speculative components.
- Do not invent functional routes for reference-only sidebar icons.
- Reuse PrimeNG and existing icon/font assets.
- Keep styles local except for existing global font/theme declarations.

## Verification

- Run the focused shared-layout, report-step and report-source component tests.
- Run the Angular production build to validate TypeScript, templates and styles.
- Run the application and compare a desktop screenshot at the reference aspect ratio against the supplied image.
- Check representative tablet and mobile widths for usability and absence of horizontal page overflow.
- Confirm dataset expansion, selection, disabled Next state and navigation still behave as before.

## Acceptance criteria

- All routes using `SharedPageLayoutComponent` share the new sidebar and header.
- The report source page matches the reference hierarchy, proportions, alignment, spacing, colors, borders, radii, shadows and density as closely as the existing assets and dynamic data allow.
- Existing functional behavior remains unchanged.
- No unrelated refactoring or new abstraction is introduced.
