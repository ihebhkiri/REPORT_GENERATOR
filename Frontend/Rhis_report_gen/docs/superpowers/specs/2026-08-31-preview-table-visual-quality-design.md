# Qualité visuelle du tableau d’aperçu

## Objectif

Améliorer uniquement la lisibilité du tableau de l’aperçu en temps réel de configuration des rapports, sans modifier ses données, son comportement, ses événements ni ses états asynchrones.

## Conception validée

Le composant existant `PreviewPanelComponent` reste inchangé dans sa structure et sa logique. Le `p-table` utilise les options natives PrimeNG 20.4.0 `size="small"`, `showGridlines`, `stripedRows` et `rowHover` au lieu de classes de compatibilité.

Le SCSS local complète uniquement ce que le thème Aura ne rend pas assez distinct : texte et en-tête contrastés via les tokens `--p-*`, espacements cohérents de 12 × 16 px et hauteur de ligne lisible. Le wrapper scrollable, le responsive, la troncature, le focus visible et les états loading, empty et error sont conservés.

## Périmètre

- `preview-panel.component.html`
- `preview-panel.component.scss`, seulement si les options PrimeNG restent visuellement insuffisantes

Aucun changement TypeScript, test métier, backend, dépendance, colonne, valeur, tri, filtre, pagination, événement ou flux HTTP.

## Vérification

- tests ciblés de `PreviewPanelComponent` ;
- build Angular ;
- contrôle visuel nominal, loading, empty, error et tableau large ;
- contrôle à 200 % de zoom, focus clavier, contrastes, bordures, alternance et hover ;
- `graphify update .` si disponible, puis examen du diff final.

## Décision Ponytail

Réutiliser PrimeNG et les tokens du thème évite tout composant, wrapper, directive, abstraction, dépendance et duplication globale de styles.
