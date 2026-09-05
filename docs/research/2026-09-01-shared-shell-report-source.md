# Research: shared shell and report source visual reproduction

Date: 2026-09-01  
Status: Ready for planning  
Related issue: N/A

## Question

Where can the supplied reference design be reproduced with the smallest visual-only Angular change while preserving report behavior and sharing the sidebar/header across routes?

## Scope

Included:

- Shared authenticated page shell.
- Report source page, dataset accordions, stepper, summary and actions.
- Existing responsive and accessibility behavior.

Excluded:

- Login page, backend, routes, services, models and business logic.
- Functional destinations not already represented by routes.

## Verified current behavior

- `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.ts: SharedPageLayoutComponent` reads route `data.page`, selects copy from `PAGE_COPY`, gets the current user from `AuthService.me()` and derives administrator visibility.
- `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.html` renders the header, route-aware navigation, page heading and one `router-outlet`; it has no sidebar.
- `Frontend/Rhis_report_gen/src/app/app.routes.ts` and `Frontend/Rhis_report_gen/src/app/features/rapports/rapports.routes.ts` reuse the shared layout for Reports, Assistant, Data administration, Configuration and Export.
- `Frontend/Rhis_report_gen/src/app/features/rapports/pages/source_de_donnes/rapports.component.ts: RapportsComponent` loads datasets and relations, maintains selection with Signals and navigates to configuration.
- `Frontend/Rhis_report_gen/src/app/features/rapports/pages/source_de_donnes/rapports.component.html` renders a shared PrimeNG stepper, loading/error/empty states, PrimeNG accordions, selected-table summary and action buttons.
- `Frontend/Rhis_report_gen/src/app/features/rapports/pages/source_de_donnes/rapports.component.scss` already supplies three/two/one-column breakpoints and selected accordion behavior.
- `Frontend/Rhis_report_gen/package.json` pins Angular 20.3.26, PrimeNG 20.4.0 and PrimeIcons 8; Tailwind and the local Material Symbols font are already configured.

## Data and control flow

- Route metadata selects shared heading and active navigation.
- `AuthService.me()` controls the administrator navigation link; failures degrade to an anonymous/non-admin shell.
- `DatasetService.getReportSources()` supplies datasets and outgoing relations.
- Accordion events update the selected main dataset and clear related selections when the main dataset changes.
- The Next action routes only when a main dataset is selected and preserves sorted related IDs in query parameters.
- This task needs no persistence, authorization or transaction change.

## Invariants and constraints

- Preserve route links, role-based administration visibility and all dataset-selection behavior.
- Keep PrimeNG accordions, stepper and buttons.
- Use existing icons/assets and local styles; add no dependency or abstraction.
- Maintain keyboard focus, semantic landmarks, responsive usability and reduced-motion handling.
- The supplied 1672 × 941 reference image is the desktop visual source of truth.

## Existing tests and validation commands

- `npm test -- --no-watch --browsers=ChromeHeadless` covers the Angular suite, including shared-layout DOM/role behavior and report dataset selection/navigation.
- `npm run build` validates TypeScript, Angular templates and style compilation.
- Commands were not run during research because production code has not changed.

## Risks and unknowns

| Item | Type | Impact | How to resolve |
|---|---|---|---|
| Existing `Logo.svg` is a 150 × 34 horizontal brand while the reference shows a compact emblem | Risk | Sidebar logo may not be identical | Crop visually with a constrained container if the emblem is separable; otherwise retain the official asset and record the residual difference |
| Reference sidebar contains destinations absent from routing | Constraint | Making them links would invent behavior | Render them as non-navigating visual controls; keep only existing routes interactive |
| Dynamic backend data may not match reference labels/order exactly | Risk | Screenshot content can differ | Preserve real data and compare layout using available fixture/backend state |
| Browser may require backend/authentication | Unknown | Automated screenshot may be blocked | Run the frontend and use an accessible local session; otherwise document the unverified visual check |

## Relevant files and symbols

- `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.{ts,html,scss}` — shared shell and route-aware heading.
- `Frontend/Rhis_report_gen/src/app/shared/report-steps/report-steps.component.{html,scss}` — existing report stepper.
- `Frontend/Rhis_report_gen/src/app/features/rapports/pages/source_de_donnes/rapports.component.{html,scss,ts}` — report source UI and preserved behavior.
- `Frontend/Rhis_report_gen/src/app/shared/page-layout/shared-page-layout.component.spec.ts` — layout invariants.
- `Frontend/Rhis_report_gen/src/app/features/rapports/pages/source_de_donnes/rapports.component.spec.ts` — report behavior invariants.

## Conclusions for planning

- Modify the existing shared layout rather than add a shell/sidebar component.
- Keep TypeScript changes limited to minimal display data, if needed; CSS and HTML can deliver the design.
- Restructure only the report page markup needed to group the heading and accordions into the reference panel.
- Verify behavior with existing tests and visual fidelity with a screenshot comparison.

## Open questions

None. The user confirmed that sidebar and header are shared across layouts and approved the design specification.
