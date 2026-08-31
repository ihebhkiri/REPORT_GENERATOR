# Preview Table Visual Quality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Améliorer uniquement la lisibilité du tableau de preview sans changer son comportement ni ses données.

**Architecture:** Conserver `PreviewPanelComponent` et déléguer l’alternance, les bordures, la densité et le hover aux inputs natifs de PrimeNG 20.4.0. Ajouter seulement les règles SCSS locales nécessaires à la hiérarchie de l’en-tête, au contraste et aux espacements.

**Tech Stack:** Angular 20.3.26, PrimeNG 20.4.0, SCSS, tokens Aura `--p-*`.

## Global Constraints

- Aucun changement TypeScript, backend, dépendance, donnée, appel HTTP, tri, filtre, pagination ou événement.
- Préserver le scroll horizontal, le responsive, le focus clavier et les états loading, empty et error.
- Utiliser les tokens PrimeNG existants ; aucun `::ng-deep`.
- Ne pas réduire la taille du texte.
- Ne pas créer de commit sans demande explicite.

---

### Task 1: Tableau de preview

**Files:**
- Modify: `src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.html:63-68`
- Modify: `src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.scss:65-77`
- Test: `src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.spec.ts`

**Interfaces:**
- Consumes: inputs PrimeNG `size`, `showGridlines`, `stripedRows`, `rowHover`.
- Produces: le même DOM métier, les mêmes lignes et cellules, avec les classes visuelles natives PrimeNG.

- [ ] **Step 1: Remplacer les classes de compatibilité par les inputs PrimeNG**

```html
<p-table
  [value]="tableRows()"
  [scrollable]="true"
  scrollHeight="flex"
  size="small"
  [showGridlines]="true"
  [stripedRows]="true"
  [rowHover]="true"
>
```

- [ ] **Step 2: Ajouter seulement les ajustements locaux nécessaires**

```scss
.preview-table {
  --p-datatable-border-color: var(--p-surface-300);
  --p-datatable-header-cell-background: var(--p-surface-100);
  --p-datatable-header-cell-color: var(--p-text-color);
  --p-datatable-column-title-font-weight: 600;
  --p-datatable-header-cell-sm-padding: 0.75rem 1rem;
  --p-datatable-body-cell-sm-padding: 0.75rem 1rem;
  --p-datatable-row-color: var(--p-text-color);
  --p-datatable-row-striped-background: var(--p-surface-50);
  --p-datatable-row-hover-background: var(--p-surface-100);
}
```

- [ ] **Step 3: Exécuter le test ciblé**

Run depuis `Frontend/Rhis_report_gen` :

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.spec.ts"
```

Expected: tous les tests de `PreviewPanelComponent` réussissent, sans changement des états ni des valeurs.

- [ ] **Step 4: Exécuter le build**

```powershell
npm.cmd run build
```

Expected: exit 0 ; consigner séparément tout warning de budget préexistant.

- [ ] **Step 5: Vérifier visuellement et contrôler le diff**

Contrôler nominal, loading, empty, error, tableau large, hover, focus clavier, mobile et zoom 200 %. Exécuter `graphify update .` si disponible, puis `git diff --check`, `git status --short` et `git diff` ; restaurer les artefacts Graphify hors périmètre et confirmer que seuls le template, le SCSS et les documents de workflow créés sont modifiés.
