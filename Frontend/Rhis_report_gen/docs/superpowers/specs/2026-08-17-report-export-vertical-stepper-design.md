# Report export vertical stepper design

Date: 2026-08-17  
Status: validated

## Goal

Modernize the existing Angular export page without changing its API calls, polling, models, report generation, export creation, download behavior, cleanup, or navigation contracts.

## Validated layout

The page keeps the application-level wizard used by the preceding report pages:

```text
✓ Source de données ── ✓ Configuration ── 3 Export
```

Inside the Export stage, a PrimeNG Timeline represents the local workflow while keeping Formats and Download visible simultaneously:

```text
✓ Préparation      45 lignes prêtes
│
2 Formats          Excel encore disponible
│
3 Téléchargement   PDF en préparation
  └─ PDF icon   Prêt dans quelques secondes…   Exporter PDF (disabled)
```

The workflow is derived independently for each format and is never used as navigation:

- Preparation is complete only when the report generation is `READY`.
- A PDF or XLSX card stays in Formats until its own export action is clicked.
- Selection moves that card synchronously to Download before the create-export request returns.
- Saved export IDs restore only their matching cards into Download after a reload.

PDF and Excel remain independent. Selecting one never moves, disables, or creates the other.

## Validated format card — option B

- Before selection: PrimeNG Card with a PrimeIcon/Avatar and an enabled `Exporter PDF/Excel` button.
- Create request pending: indeterminate ProgressBar, `Prêt dans quelques secondes…`, and the same Export button disabled.
- Server `PENDING/RUNNING`: determinate ProgressBar using server progress and the Export button disabled.
- `READY`: progress reaches 100%, the message becomes `Fichier prêt`, and the action becomes an enabled `Télécharger PDF/Excel` button.
- Create or polling failure: the card stays in Download with a local Message and an enabled `Réessayer PDF/Excel` action.

## Visual system

Core palette:

- Canvas `#F7F9FB`
- Surface `#FFFFFF`
- Ink `#0F172A`
- Muted `#64748B`
- Primary `#2563EB`
- Success `#18794E`

PDF and Excel use the official `pi-file-pdf` and `pi-file-excel` PrimeIcons. Color never communicates status by itself.

Typography stays on the application's existing Inter/system stack. The page uses PrimeNG Stepper, Timeline, Card, Avatar, Button, ProgressBar, Message, and Tag with PrimeIcons 8. Tailwind utilities and Material Symbols are not used in the Export template.

The distinctive element is the nested workflow hierarchy: the horizontal wizard communicates the full report journey while the vertical timeline explains only the asynchronous export work. This is functional structure rather than decoration.

## Responsive behavior

- Desktop and tablet: horizontal wizard labels remain visible; PDF and Excel use two equal columns.
- Narrow mobile: wizard labels remain visible below their markers; format panels stack to prevent clipping.
- The bottom actions participate in normal document flow; there is no fixed action bar.

## Accessibility

- PrimeNG Stepper renders the read-only application wizard and Timeline provides the semantic ordered workflow.
- Timeline markers are descriptive and non-interactive.
- Progress indicators receive format- or generation-specific accessible labels.
- Status is always communicated by text in addition to color and icon.
- Existing alert and network interruption behavior remains available with clearer visual grouping.

## Error and edge states

- `FAILED` generation stays on Preparation and exposes the existing retry/back action.
- A failed PDF or Excel export remains in Download and can be retried independently, including when the last server snapshot was still `PENDING`.
- Network interruption is not rendered as a permanent job failure.
- `EXPIRED` keeps the existing restoration/navigation behavior.

## Technical scope

Modify only the existing export feature plus the PrimeIcons dependency and global CSS import:

- `export.component.ts` for derived presentation state and labels;
- `export.component.html` for the validated semantic structure;
- `export.component.scss` for the responsive visual implementation;
- `export.component.spec.ts` for derived stages and rendered UI states.
- `package.json`, `package-lock.json`, and `styles.scss` for PrimeIcons 8.

Do not add a store, facade, service, mapper, child component, or reusable stepper abstraction. Do not change APIs, models, routes, polling intervals, Blob downloads, cleanup, or navigation.
