# Recherche — reconstruction visuelle de la page Configuration

Date : 2026-09-02

## Objet

Établir le comportement et la structure actuels avant d’adapter visuellement le contenu principal de la page Configuration à la capture 1664 × 970.

## Faits vérifiés

- `rapports.routes.ts` charge `SharedPageLayoutComponent` pour `/rapports/configuration/:datasetId`, puis rend `ConfigurationComponent` comme enfant. Le header et la sidebar sont donc fournis par le layout partagé.
- `ConfigurationComponent` orchestre le chargement, la sélection des colonnes, les filtres, les tris, l’aperçu et la génération. Ces comportements sont déjà testés et ne nécessitent aucune modification pour le travail visuel demandé.
- La page compose déjà `ReportStepsComponent`, `ColumnSelectorComponent`, `FilterEditorComponent`, `SortEditorComponent` et `PreviewPanelComponent`.
- Le desktop utilise actuellement une grille de configuration `2/3 + 1/3`, suivie d’une barre d’actions et d’un aperçu. La structure générale correspond déjà à la référence, mais la hiérarchie supérieure, les dimensions et plusieurs détails de densité diffèrent.
- Sous 768 px, la page expose des tabs Configuration/Aperçu avec gestion clavier. Ce comportement doit rester intact.
- Angular 20.3.26, PrimeNG 20.4.0, PrimeIcons 8 et les Material Symbols locaux sont déjà installés. Aucune dépendance supplémentaire n’est nécessaire.

## Écarts visuels principaux

- Le contenu principal ne présente pas le breadcrumb, le titre et le sous-titre visibles sur la référence.
- Le stepper doit être placé dans une card pleine largeur et reprendre une densité proche de la référence.
- Les cards utilisent des titres et paddings plus grands que la référence ; leurs hauteurs, séparations et fonds doivent être harmonisés.
- La barre d’actions doit présenter une information avec icône à gauche et les actions dans l’ordre visuel Précédent puis Suivant à droite.
- Le panneau d’aperçu doit afficher « Aperçu en temps réel », le badge de statut et un état vide compact comparable à la référence.

## Invariants fonctionnels

- conserver les inputs/outputs et handlers des composants enfants ;
- conserver le chargement, les erreurs, les validations, le debounce de preview et la génération ;
- conserver les routes et `SharedPageLayoutComponent` ;
- conserver les tabs mobiles, le focus clavier et `prefers-reduced-motion` ;
- ne pas introduire de nouveau composant, service, modèle ou package.

## Fichiers susceptibles d’être modifiés

- `Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration/configuration.component.html`
- `Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration/configuration.component.scss`
- `Frontend/Rhis_report_gen/src/app/shared/report-steps/report-steps.component.scss`
- templates/styles des composants `column-selector`, `filter-editor`, `sort-editor` et `preview-panel` ;
- les specs correspondantes uniquement si une structure ou un libellé observable change.

## Risques

- Une réduction excessive de la densité peut détériorer les formulaires lorsque des filtres ou tris existent.
- Des styles trop larges pourraient affecter d’autres écrans ; les changements doivent rester locaux.
- Une imitation stricte du desktop peut casser le responsive ou le zoom à 200 % ; les contrôles manuels sont requis.
- Le contrôle nominal dépend d’une session et d’un jeu de données backend accessibles.

## Conclusion

La structure existante est suffisamment proche de la référence. Le plus petit changement fiable consiste à ajuster les templates et styles locaux déjà présents, sans toucher au layout partagé ni à la logique TypeScript.
