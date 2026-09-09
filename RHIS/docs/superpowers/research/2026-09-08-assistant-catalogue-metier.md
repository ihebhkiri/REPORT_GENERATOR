# Research: assistant et catalogue métier

Date: 2026-09-08
Status: Ready for planning
Related issue: N/A

## Question
Comment produire des rapports sans questions techniques, sélectionner les informations autorisées par défaut et enrichir le catalogue de descriptions et alias ?

## Scope
Assistant de rapports, métadonnées et administration du catalogue. Pas de changement de fournisseur IA ni des règles SQL.

## Verified current behavior
- `src/main/java/RHIS/com/RHIS/bot/BotReportPlanner.java: SYSTEM_PROMPT` interdit les questions techniques, mais l'exemple FAILED contient « datasets ». La sélection privilégie les champs explicites et interdit les ajouts non indispensables ; aucune règle ne garantit une liste complète par défaut. Le prompt mentionne des aliases absents du contrat réel.
- `bot/catalog/CatalogDataset.java`, `CatalogField.java`, `ReportCatalogProvider.java` : seuls noms affichés, identifiants, types et opérateurs sont transmis ; aucune description ni alias.
- `dataset/entity/DataSetEntity.java`, `DataSetField.java` : aucune propriété description ou alias persistée.
- `dataset/repository/DataSetFieldRepository.java: findVisibleFieldsByDatasetId` : champs actifs, visibles, dataset actif, ordre par position ; le provider exclut les types non supportés. Ce sont des règles d'exposition du catalogue, pas des permissions individuelles vérifiées ici.
- `bot/BotReportService.java` : validation des appartenances et chemins puis `ReportDefinitionResolver.resolve`, une seule correction autorisée. Les questions techniques sont rejetées ; le filtre lexical ne couvre pas « join » ou « id ». Les résumés techniques sont remplacés et les erreurs finales sont génériques.
- `dataset/service/DataSetAdministrationService.java` et DTO associés : lecture/écriture transactionnelle de l'exposition, sans enrichissement métier.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/` : écran, modèle et service existants à étendre.
- `../Frontend/Rhis_report_gen/src/app/features/report-assistant/report-assistant.component.ts` : affiche les questions et résumés reçus ; les erreurs HTTP ont des messages contrôlés.

## Data and control flow
Demande initiale et éventuelle clarification → catalogue des métadonnées → proposition IA → validation serveur → génération asynchrone existante. Aucun contenu de rapport envoyé au modèle. La modification des métadonnées passe par l'administration existante.

## Invariants and constraints
Ne jamais exposer un champ masqué/inactif ; revalider avant génération ; ne pas ajouter des entités liées non demandées ; préserver les changements utilisateur. `application.yaml` est déjà modifié et configure `ddl-auto: create` : la conservation des métadonnées au redémarrage nécessite une stratégie explicite avant déploiement persistant.

## Existing tests and validation commands
Classes existantes : BotReportPlannerTest, BotReportServiceTest, ReportCatalogProviderTest, BotReportControllerSecurityTest ; tests Angular dataset-exposure et report-assistant. Aucun test exécuté pendant cette recherche en lecture seule.

## Risks and unknowns
- Interprétation proposée : « liste des employés » inclut toutes les informations autorisées des employés, sans ajout automatique de leurs contrats ou autres entités.
- Description et alias proposés pour les entités ET les champs, modifiables par l'administrateur. Alias = synonymes de compréhension ; le libellé existant reste le titre du rapport.
- Les descriptions métier exactes ne sont pas déductibles avec certitude de chaque nom physique ; ne pas inventer de définitions métier.
- Vérifier synchronisation des métadonnées et stratégie de migration avant toute implémentation de persistance.

## Conclusions for planning
Un changement de modèle seul ne garantit pas la sélection exhaustive. Préférer des métadonnées enrichies et une expansion serveur explicite du mode « tous les champs autorisés », avec validation existante conservée.

## Open questions
Approbation du comportement proposé et de l'édition des descriptions/alias dans le catalogue administratif.
