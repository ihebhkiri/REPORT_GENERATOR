# Rendre les demandes de rapports accessibles aux clients non techniques

Date: 2026-09-08
Status: In progress
Research: ../research/2026-09-08-assistant-catalogue-metier.md

Ce plan suit `.agent/PLANS.md`. Chemins Java ci-dessous relatifs à `RHIS/src/main/java/RHIS/com/RHIS/`.

## Purpose and observable outcome
« Je veux la liste des employés » produit toutes les informations autorisées des employés, sans demander lesquelles choisir. « Je veux le nom et la date d'embauche des employés » produit uniquement ces informations dans cet ordre. Questions éventuelles limitées aux ambiguïtés métier réelles, par exemple la période concernée. Aucun identifiant interne ni vocabulaire technique affiché.

## Scope and non-goals
Descriptions et synonymes pour entités et champs, éditables dans l'administration existante et transmis à l'IA uniquement pour les éléments exposés. Préserver génération, contrôles serveur et noms affichés. Pas de changement de modèle, fine-tuning, nouvelle infrastructure ou ajout automatique de toutes les entités liées.

## Current behavior
Voir la recherche associée : descriptions/alias absents, sélection exhaustive non garantie, protection partielle du langage déjà en place. Les autorisations désignent ici l'exposition existante ; aucune nouvelle permission par utilisateur.

## Proposed approach
Enrichir le catalogue et corriger les consignes, puis faire développer côté serveur une sélection structurée « tous les champs autorisés » pour les entités concernées. Une sélection explicite reste stricte ; ne jamais traiter une liste vide invalide comme une demande exhaustive. Les filtres ne désignent pas automatiquement les informations à afficher. Un champ indisponible ne doit pas être remplacé silencieusement. Les ambiguïtés entre synonymes nécessitent une question métier. Les descriptions et alias sont des données, jamais des instructions à suivre.

Alternatives : prompt seul, plus petit mais exhaustivité non garantie ; changement de modèle, coût et validation supplémentaires sans corriger l'absence de métadonnées. Approche recommandée : catalogue enrichi et règles serveur ciblées.

## Affected files and symbols
- `dataset/entity/DataSetEntity.java`, `DataSetField.java` : descriptions/alias persistés.
- `dataset/controller/dto/UpdateDataSetExposureRequest.java`, `DataSetExposureConfigurationResponse.java` et `dataset/service/DataSetAdministrationService.java` : contrat, validation, sauvegarde et lecture des métadonnées métier.
- `bot/catalog/CatalogDataset.java`, `CatalogField.java`, `ReportCatalogProvider.java` : transmission de l'enrichissement pour les éléments exposés.
- `bot/dto/BotReportPlan.java`, `bot/BotReportPlanner.java`, `bot/BotReportService.java` : intention de sélection explicite/exhaustive, expansion serveur et langage client.
- `Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.{model.ts,component.ts,component.html,component.scss}` : édition dans les contrôles existants, suivi des changements non sauvegardés.
- Tests correspondants Java et Angular ; `RHIS/docs/flows/07-bot-natural-language-report.md` : contrat et comportement actualisés.
- Script de migration additif à localiser selon la convention du dépôt après examen de la synchronisation ; ne pas modifier la configuration utilisateur existante.

## Milestone 1: catalogue enrichi de bout en bout
Vérifier d'abord synchronisation, garde admin, conventions de migration et interfaces complètes. Ajouter des descriptions facultatives et alias bornés, normalisés et sans doublons, conservés lors des resynchronisations. Étendre l'administration accessible sans nouvelle page. Définir migration additive et vérifier la persistance réelle dans un environnement non destructif.

Validation : tests d'administration et ReportCatalogProviderTest ; tests Angular dataset-exposure. Vérifier aller-retour sauvegarde/lecture, données vides, longueurs invalides, absence de fuite de champs masqués.

## Milestone 2: sélection et conversation métier
Introduire l'intention de sélection dans le plan interne ; développer la sélection exhaustive depuis le catalogue ordonné avant validation ; conserver strictement la sélection explicite. Corriger exemples contradictoires et contrôler les réponses client, y compris id/join, sans bloquer les termes métier légitimes. Garder deux tentatives maximum et la validation finale du resolver.

Validation backend depuis RHIS : `mvn "-Dtest=BotReportPlannerTest,BotReportServiceTest,ReportCatalogProviderTest,BotReportControllerSecurityTest" test`.
Validation frontend : `npm.cmd test -- --watch=false --include="src/app/features/administration/dataset-exposure/*.spec.ts"`, puis tests report-assistant et `npm.cmd run build` si interface modifiée.

## Validation and acceptance
- Liste sans précision : ensemble exact des champs exposés, ordre stable, aucun champ lié ajouté implicitement.
- Champs explicites : uniquement ceux demandés et autorisés, ordre demandé respecté.
- Filtre seul : ne réduit pas les informations affichées implicitement.
- Alias reconnu ; alias ambigu : une question compréhensible ; aucune invention métier.
- Champ caché, inexistant, désactivé ou révoqué avant génération : refus serveur.
- Question/résumé contenant identifiant interne ou join : non transmis au client.
- Description/alias édités : présents après relecture et resynchronisation.
- Contrôle manuel de demandes réelles via le fournisseur IA et inspection d'un rapport exporté ; les tests avec doublures ne prouvent pas à eux seuls la qualité du modèle.

## Risks and rollback
Rapports potentiellement larges : vérifier limites et rendu existants, ne pas tronquer silencieusement. Synonymes ambigus : clarification métier. Métadonnées libres : validation et traitement comme données. `ddl-auto=create` compromet la persistance : résoudre la stratégie de déploiement avant application à une base persistante. Retour arrière du code possible en gardant les colonnes additives ; aucun effacement de données pour revenir en arrière.

## Progress
- [x] 2026-09-08 — Recherche et proposition écrites ; aucun code de production modifié.
- [x] 2026-09-08 — Approbation humaine du comportement et du plan.
- [x] 2026-09-08 — Branche `codex/assistant-catalogue-metier` créée ; dépôt Git commun confirmé.
- [x] Jalon 1 implémenté et vérifié.
- [x] Jalon 2 implémenté et vérifié (tests ciblés).
- [ ] Revue finale et contrôle observable.

## Surprises & Discoveries
- 2026-09-08 — Administration et synchronisation existantes réutilisées. Métadonnées facultatives : description 1000 caractères, alias 2000 caractères, un par ligne (20 × 100 max), déduplication insensible à la casse. Valeur omise conserve l'existant ; chaîne vide efface explicitement.
- 2026-09-08 — Pas d'outil de migration existant ; script additif fourni sous `RHIS/docs/migrations/2026-09-08-catalogue-metier.sql`, aucune application à la base utilisateur. `application.yaml` préexistant reste intact.
- 2026-09-08 — Tests initialement bloqués par sandbox Maven/Angular, relancés avec escalade. Première compilation Java : parenthèse surnuméraire dans un nouveau test, corrigée. Angular : 26 succès / 4 échecs ; titre de détail et texte de recherche vide absents dans HEAD, rétablis dans l'écran modifié pour préserver accessibilité et navigation.
- Le prompt cite déjà des aliases que le catalogue ne fournit pas.
- Une protection du langage existe déjà ; la renforcer plutôt que créer un mécanisme parallèle.
- Les commandes git exécutées depuis les sous-dossiers affichent aussi des chemins du dépôt parent ; vérifier les frontières Git réelles avant création de branche.

## Decision Log
- 2026-09-08 — Implémentation de la sélection exhaustive avec `allFieldsDatasetIds` (liste structurée) plutôt qu'un mode global : cela évite d'afficher les champs des entités utilisées uniquement pour filtrer et permet une demande mixte. La sélection explicite est conservée, puis les groupes complets sont ajoutés dans l'ordre fourni ; les doublons d'expansion sont évités, les identifiants explicites invalides restent rejetés.
- 2026-09-08 — Vérification du jalon 1 : 15 tests backend ciblés passent (administration 8, sécurité 4, catalogue 3) et 30 tests Angular passent. Test PostgreSQL de persistance/resynchronisation et migration idempotente en cours sur Testcontainers, sans base utilisateur.
- 2026-09-08 — Proposition : étendre l'administration existante ; synonymes pour compréhension, libellés existants conservés dans le rapport.
- 2026-09-08 — Proposition : expansion serveur explicite pour garantir l'exhaustivité ; aucune déduction « liste vide = tous ».

## Outcomes & Retrospective
Implémentation livrée sur la branche dédiée : métadonnées administrables, catalogue enrichi, sélection exhaustive explicite côté serveur, sélection précise conservée, questions métier renforcées. Migration additive fournie et testée, non appliquée à la base utilisateur.

Vérifications réussies : 46 tests backend ciblés, 2 tests PostgreSQL 16 Testcontainers (persistance/resynchronisation et migration idempotente), 39 tests Angular ciblés, build Angular de production, git diff --check. Contrôle navigateur avec API fictive : édition, sauvegarde, notification et restauration du focus vérifiés. Graphify update terminé.

Commandes : mvn -Dtest=DataSetAdministrationServiceTest,DataSetAdministrationControllerSecurityTest,ReportCatalogProviderTest,BotReportServiceTest,BotReportPlannerTest,BotReportControllerSecurityTest test ; mvn -Dtest=CatalogMetadataPostgresIntegrationTest test ; npm.cmd test -- --watch=false --browsers=ChromeHeadless avec les inclusions catalogue et assistant ; npm.cmd run build.

La suite Angular complète échoue : 8 échecs dans Auth refresh interceptor et PreviewPanelComponent (fichiers non modifiés), puis déconnexion Chrome. Reproduction isolée : 16 tests exécutés, mêmes 8 échecs. Aucun succès global revendiqué ; ces échecs restent hors périmètre.

À terminer pour validation en environnement réel : appliquer la migration avec une configuration de persistance adaptée, renseigner les descriptions/alias métier réels, générer puis inspecter un rapport exporté. Le test fournisseur opt-in utilise uniquement un catalogue fictif. La configuration utilisateur et sa clé ne sont pas modifiées.

Dernier contrôle fournisseur réel : `mvn -Dtest=BotReportPlannerLiveTest -Drhis.bot.live-test=true test -q` compile et appelle le fournisseur. Les assertions liste simple et champs explicites avec alias passent ; le scénario liste filtrée échoue car allFieldsDatasetIds est vide au lieu de [1]. Prochaine action exacte : inspecter le plan retourné pour ce troisième cas, déterminer si la sélection explicite reste exhaustive ou si le fournisseur omet des champs, puis ajuster la consigne et vérifier à nouveau. La validation métier réelle n'est donc pas terminée. Test opt-in conservé pour reproduction, aucune clé dans le test.

Correction ciblée demandée le 2026-09-08 (Ponytail) : reproduction avec diagnostic du plan réel. Le fournisseur renvoie selectedFieldIds=[10,11], le filtre salaire correct et allFieldsDatasetIds=[] : aucune omission de champ observée dans cet exemple, contrairement au risque supposé au précédent bilan. L'échec porte sur le contrat de sélection exhaustive. Correction minimale : interdire l'énumération pour une liste et compléter l'exemple filtré avec un JSON complet, comme l'exemple simple déjà réussi. Pas de déduction heuristique côté serveur ni changement des autorisations. Test live conserve l'assertion stricte et affiche le plan fictif en cas d'échec. Vérification en cours : mvn -Dtest=BotReportPlannerLiveTest,BotReportPlannerTest,BotReportServiceTest -Drhis.bot.live-test=true test -q.

Diagnostic confirmé : le changement de prompt n'a pas modifié le résultat et a été retiré. La liste filtrée contient déjà exactement [10,11], avec racine 1, relation 2 et filtre 20 GREATER_THAN 2000. Correction finale limitée au test live : vérifier la sélection effective (explicite ou développée), son ordre, l'absence d'entité complète étrangère et de doublons, ainsi que le filtre. Ne pas forcer côté serveur une expansion qui pourrait altérer une demande explicite. Les tests unitaires existants couvrent l'expansion serveur. Le précédent diagnostic de fonctionnalité inachevée était trop fort : aucun champ manquant n'a été observé.
Validation finale du correctif : commande ciblée terminée avec code 0 ; 27 tests réussis (1 live couvrant trois demandes, 5 planner, 21 service). Aucun changement de production supplémentaire. Les limites de déploiement et de validation des exports restent celles documentées précédemment.

Correctif endpoint POST /api/v1/bot/reports (2026-09-08) : défaut reproduit par MockMvc, ReportCapacityException remonte sans gestion quand la limite de rapports actifs est atteinte. ReportExceptionHandler est limité aux contrôleurs de rapports, excluant BotReportController. Ajout d'un handler de capacité métier dans BotExceptionHandler, limité explicitement au contrôleur bot pour préserver les autres endpoints. Test HTTP avant correction : 1 erreur Servlet non gérée ; après correction, validation ciblée en cours. Aucun appel à la base utilisateur. Ce défaut est démontré ; faute de requête/réponse fournie par le client, il n'est pas présenté comme explication certaine de son incident précis.
Validation correctif endpoint : mvn -Dtest=BotReportControllerSecurityTest,BotReportServiceTest test -q terminé avec code 0, 27 tests réussis (6 HTTP/sécurité, 21 service), git diff --check réussi. Correction vérifiée par MockMvc ; application locale non redémarrée et incident utilisateur non reproduit avec ses données. Graphify update lancé.

Extension autorisée : seeder des descriptions et alias pour chaque table/champ du catalogue. Recherche : DataSetInitializer découvre public.rhis% et synchronise via JPA ; WorkforceDataSeeder a Order(100). Métadonnées stockées en varchar, alias séparés par sauts de ligne. Choix minimal : DataSetInitializer Order(0), nouveau CatalogMetadataSeeder Order(10), transaction et script SQL PostgreSQL embarqué exécuté via ResourceDatabasePopulator. Script : descriptions françaises et alias pour les sept tables connues et leurs colonnes, repli descriptif neutre pour des éléments supplémentaires ; mise à jour séparée des seules valeurs nulles/vides. Ne touche ni libellés ni exposition. Validation PostgreSQL : toutes les métadonnées remplies dès démarrage, exécution répétée stable, valeurs personnalisées préservées, exemple de synonymes transmis au catalogue. Pas de lancement sur la base locale avec ddl-auto=create.
Seeder livré : CatalogMetadataSeeder + db/catalog-metadata-seed.sql, DataSetInitializer ordonné avant le seeder. Vérification : mvn -Dtest=CatalogMetadataPostgresIntegrationTest test -q, 3 tests PostgreSQL réussis sans skip ; couverture au démarrage des 7 tables et 77 champs, préservation des valeurs personnalisées et visibilité, idempotence. Diff contrôlé. Application utilisateur non redémarrée ; le seeder sera exécuté au prochain démarrage (configuration de persistance à conserver, éviter ddl-auto=create sur une base à préserver).
