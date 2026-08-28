# Dataset exposure Ponytail — research

Date : 2026-08-29. Périmètre approuvé par l'utilisateur dans cette tâche.

## Faits et flux

- Racine Git réelle : `C:/Users/Surface Pro/Downloads/RHIS`, branche
  `codex/dataset-exposure-master-detail`, HEAD `06259e0c88ae10b6f23a8d7838783f6319b04da8`.
  Les deux applications partagent cette racine, contrairement aux AGENTS.md.
- `/administration/datasets` → adminGuard / SharedPageLayoutComponent →
  DatasetExposureComponent → DatasetExposureService → GET/PUT
  `/api/v1/admin/dataset-exposure` → DataSetAdministrationController →
  DataSetAdministrationService → DataSetRepository / DataSetFieldRepository.
- GET charge tables et champs, inactifs inclus, sans noms techniques. PUT reçoit
  le delta global, valide tout le lot avant mutation, flush dans la transaction,
  puis renvoie la configuration complète. ROLE_ADMIN est exigé à deux niveaux.
- `updateMode` et `updateField` remplacent les objets avec map/spread ; les modèles
  sont readonly et les contrôles utilisent ngModel en lecture puis ngModelChange.
  Les copies profondes baseline/draft sont donc redondantes dans ce flux.
- `rg` sur le frontend confirme que clearTableSearch/clearFieldSearch n'ont aucun
  appelant. Le guard appelle hasUnsavedChanges : cette méthode reste inchangée.
- Les IDs sont actuellement mappés plusieurs fois ; rejectDuplicates précède les
  lectures. Après ce contrôle, convertir encore en Set ne retire aucun doublon.
- Les lectures rapports consomment displayMain, displayRelated et visible via
  DataSetServiceImpl et les repositories ; ReportDefinitionResolver utilise aussi
  findVisibleTableRelations. Aucun changement de ces chemins partagés n'est prévu.

## Contrats et exclusions

Conserver API, tri, cardinalité, schéma, permissions, messages d'erreur, ordre des
validations, transactions, flush, UI, animations, focus et protections de sortie.
Pas de dépendance, abstraction, commit ou modification des changements utilisateur
hors des deux sources et de leurs tests approuvés.

## Anomalies préexistantes séparées

- Le template référence dataset-detail-title, mais le titre sélectionné et sa
  référence detailHeading sont absents. Des tests les attendent encore.
- Des textes d'états vides attendus par les tests ont disparu du template.
- docs/flows/01 décrit certains endpoints comme permitAll ; SecurityConfig exige
  authenticated. Préserver le code et laisser cette documentation hors refactor.

## Décision

Supprimer les deux méthodes inutilisées et cloneDatasets ; réutiliser les listes
d'IDs validées côté backend. Conserver controllers, transport HTTP, DTO, repositories,
guard et styles. Les tests et limites exactes sont consignés dans la note de progrès.
