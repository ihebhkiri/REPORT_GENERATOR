# Générer la présentation technique RHIS

Ce plan vivant suit `.agent/PLANS.md`.

Date: 2026-09-05  
Status: Completed  
Research: `docs/superpowers/specs/2026-09-05-technical-project-presentation-design.md`  
Related issue: N/A

## Purpose and observable outcome

Produire un fichier PowerPoint éditable de 13 slides, calibré pour une présentation technique de 15 minutes, qui décrit fidèlement l'architecture et les flux actuels de RHIS.

## Scope and non-goals

In scope:

- architecture globale, frontend, backend et modèle de données;
- parcours manuel, génération/export asynchrones et assistant XKiro;
- validation backend, sécurité, décisions, difficultés, démo, limites et améliorations;
- captures réelles disponibles et diagrammes PowerPoint éditables;
- validation structurelle, textuelle et visuelle du deck.

Non-goals:

- modifier le produit RHIS;
- inventer un déploiement, une métrique ou une fonctionnalité absente;
- exposer une clé ou une donnée sensible.

## Current behavior

Angular charge le catalogue PostgreSQL exposé, permet de configurer colonnes, filtres et tris, puis appelle la preview ou crée une génération asynchrone. Spring valide la définition, construit le SQL, exécute la lecture par JDBC, écrit un snapshot temporaire et produit PDF/XLSX. L'assistant appelle XKiro via l'adapter OpenAI de Spring AI, puis soumet son structured output au même resolver autoritatif.

## Proposed approach

Générer le deck avec PptxGenJS, sans nouvelle dépendance. Utiliser les logos et captures du dépôt, des diagrammes natifs simples et des notes de présentation. Rendre toutes les slides, corriger les défauts visibles, puis exécuter la validation OOXML du skill.

## Affected files and symbols

- `tools/presentation/generate-rhis-technical-deck.js`
  - Générateur reproductible du deck.
- `docs/presentations/RHIS-presentation-technique.pptx`
  - Livrable final.
- `docs/plans/2026-09-05-technical-project-presentation.md`
  - État d'exécution et preuves de validation.

## Milestone 1: Contenu et assets

Result:

Les messages des 13 slides et les captures utilisables sont établis à partir du dépôt.

Work:

- vérifier les sources citées dans la spécification;
- capturer les écrans accessibles de l'application;
- préparer les diagrammes et les notes orateur.

Validation:

- Command: `rg -n "Mistral|TODO|TBD" tools/presentation/generate-rhis-technical-deck.js`
- Expected observation: aucune ancienne référence provider ni placeholder.

## Milestone 2: Génération PowerPoint

Result:

Un deck 16:9 de 13 slides est produit avec une mise en page cohérente et éditable.

Work:

- écrire le générateur PptxGenJS;
- intégrer logos, captures, diagrammes et notes;
- générer `docs/presentations/RHIS-presentation-technique.pptx`.

Validation:

- Command: `node tools/presentation/generate-rhis-technical-deck.js`
- Expected observation: le fichier PPTX est créé sans erreur.

## Milestone 3: Validation et corrections

Result:

Le deck ne contient ni erreur structurelle, ni contenu manquant, ni défaut visuel manifeste.

Work:

- valider le package OOXML;
- extraire le texte et vérifier l'ordre;
- convertir en PDF puis images;
- inspecter les 13 slides et corriger le générateur si nécessaire.

Validation:

- Command: `python C:/Users/Surface\ Pro/.agents/skills/pptx/scripts/office/validate.py docs/presentations/RHIS-presentation-technique.pptx`
- Expected observation: validation réussie.
- Command: `markitdown docs/presentations/RHIS-presentation-technique.pptx`
- Expected observation: 13 slides complètes, sans placeholder.

## Validation and acceptance

Automated:

- [x] Générateur exécuté avec succès.
- [x] Validation OOXML réussie.
- [x] Extraction textuelle vérifiée.

Manual:

- [ ] Rendu bitmap non exécuté : LibreOffice et les dépendances du renderer ne sont pas disponibles dans le runtime fourni.
- [x] Géométrie des 13 slides vérifiée automatiquement sans objet hors limites.

Acceptance criteria:

- [x] 13 slides adaptées à 15 minutes.
- [x] XKiro apparaît comme provider LLM.
- [x] Les contrôles de confiance backend sont visibles.
- [x] Aucun secret n'apparaît dans le deck.

## Risks and rollback

| Risk | Prevention/Detection | Rollback |
|---|---|---|
| Capture impossible sans backend | Utiliser uniquement les écrans accessibles et les assets existants | Remplacer la capture par un diagramme natif fidèle |
| Texte trop dense | Inspection du rendu en taille réelle | Réduire le texte ou répartir les éléments dans la même slide |
| Information obsolète | Vérification contre le code courant | Corriger le générateur et régénérer |

## Progress

- [x] 2026-09-05 — Structure et direction visuelle approuvées.
- [x] 2026-09-05 — Provider corrigé vers XKiro et spécification approuvée.
- [x] 2026-09-05 — Deck généré avec PptxGenJS.
- [x] 2026-09-05 — Intégrité, contenu et géométrie validés.
- [~] 2026-09-05 — Inspection visuelle native non automatisable dans le runtime courant.

## Surprises & Discoveries

- 2026-09-05 — XKiro expose une API compatible OpenAI; Spring AI utilise donc le starter OpenAI avec `base-url: https://api.xkiro.com/v1`.
- 2026-09-05 — La configuration courante contient une clé XKiro en clair. Le deck signalera le risque sans reproduire le secret.
- 2026-09-05 — Le validator PPTX historique ne démarre pas car `defusedxml` manque. Les validators du runtime Presentations ont validé l'intégrité et la géométrie du package.
- 2026-09-05 — Aucun LibreOffice n'est disponible dans le runtime fourni; la conversion PDF/bitmap n'a pas été exécutée.

## Decision Log

- 2026-09-05 — **Decision:** utiliser une narration architecture puis flux en 13 slides.
  - Reason: meilleure densité pour un entretien technique de 15 minutes.
  - Alternatives rejected: narration demo-first et narration centrée uniquement sur les difficultés.
- 2026-09-05 — **Decision:** utiliser PptxGenJS et les assets déjà présents.
  - Reason: solution la plus courte, reproductible et conforme au skill PPTX.
  - Alternatives rejected: nouvelle dépendance ou illustrations génériques externes.

## Outcomes & Retrospective

- Delivered behavior: présentation éditable de 13 slides, narration architecture puis flux, provider XKiro et notes orateur.
- Commands run and results: génération Node réussie; package integrity `pass`; layout geometry avec `0 finding` et `0 warning`; extraction XML des 13 slides réussie.
- Deviations from the approved plan: aucune capture applicative intégrée car seul l'écran de login était accessible sans backend complet; les diagrammes reposent sur le code vérifié.
- Remaining risks or unverified checks: rendu visuel PowerPoint/LibreOffice non inspecté automatiquement.
- Required follow-up: ouvrir le deck dans PowerPoint et parcourir les 13 slides avant la soutenance.
- Exact next action if incomplete: aucune action de génération restante.
