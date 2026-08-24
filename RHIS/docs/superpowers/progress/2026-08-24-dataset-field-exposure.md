# Progress — exposition administrative des datasets et champs

Date: 2026-08-24  
Status: Implemented — manual validation pending  
Plan: `docs/plans/2026-08-24-dataset-field-exposure.md`

## Completed

- Research et ExecPlan rédigés puis approuvés.
- Graphify régénéré et documents réalignés.
- Décisions finales intégrées au plan : extraction des champs indépendante des modes, endpoint contextuel annulé, affichage UI exclusivement via `displayName`.
- État initial des deux repositories et diffs préexistants inspectés.
- Extraction des champs alignée sur `dataset.active && field.active && field.visible`, sans rôle main/relation.
- Relation native corrigée : source active, cible active et `displayRelated=true`.
- API admin GET/PUT sécurisée, validée, transactionnelle et sans noms techniques dans son contrat UI.
- Resolver, génération, exports et download revalident l'exposition ; erreurs `REPORT_DEFINITION_UNAVAILABLE` structurées.
- Page Angular `/administration/datasets`, guard admin, quatre modes, recherche `displayName`, sauvegarde delta et états complets.
- Cinq documents `docs/flows/01` à `05` mis à jour et revue cinq axes effectuée.

## Current validation state

- Backend final ciblé : 25 tests réussis, aucun échec.
- Backend complet : 63 réussis, 4 échecs XLSX préexistants, 4 intégrations PostgreSQL ignorées faute de Docker.
- Frontend complet : 108 réussis, 1 échec `p-tag` préexistant et indépendant du diff.
- Build Angular : réussi ; warning budget initial 567,01 kB pour 500 kB.
- `git diff --check` : aucune erreur dans les deux repositories.

## Pre-existing dirty files

Backend :

- `DataSetInitializer.java`
- `DataSetEntity.java`
- `DataSetFieldRepository.java`
- `DataSetRepository.java`
- `DataSetServiceImpl.java`
- `ReportDefinitionResolver.java`
- `ReportPreviewServiceTest.java`
- documents et `AGENTS.md` non suivis issus du travail précédent.

Frontend :

- `AGENTS.md` non suivi au dernier état vérifiable.

## Decisions

- Aucun worktree.
- Aucun subagent sans demande explicite.
- Préserver tous les changements existants et ne pas commit/push.
- Exécuter milestone par milestone avec tests focalisés avant élargissement.

## Blockers

- Git frontend exige `safe.directory` sous l'utilisateur sandbox ; utiliser `git -c safe.directory=<path>` sans modifier la configuration globale.
- Docker indisponible : la requête native de relations n'a pas été exercée par Testcontainers.
- Stack non démarrée : checklist manuelle admin/non-admin, responsive et parcours complet non exécutée.

## Exact next action

Démarrer la stack avec un profil PostgreSQL persistant, puis exécuter la checklist manuelle du plan sans modifier les changements utilisateur hors périmètre.
