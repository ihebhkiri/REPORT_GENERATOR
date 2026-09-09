# Architecture RHIS — 9 septembre 2026

État du code local, y compris les modifications non commitées. Ces vues décrivent les responsabilités et les principaux scénarios ; elles ne constituent pas une topologie de déploiement en production.

## Vues interactives

1. [Architecture globale](01-architecture.html) — Angular, sécurité, services, IA, PostgreSQL et fichiers.
2. [Connexion et session](02-session.html) — login, cookies, profil, refresh après 401.
3. [Catalogue et autorisations](03-catalogue.html) — découverte, seeder, administration et exposition.
4. [Assistant métier](04-assistant.html) — catalogue transmis, clarification, validation et correction.
5. [Génération et export](05-rapports.html) — preview, job asynchrone, snapshot, PDF/XLSX et téléchargement.

Chaque HTML est autonome, avec thèmes clair/sombre, zoom et export. Le contenu est français ; l’interface fixe du visualiseur et son attribut de langue utilisent le repli anglais d’Archify.

## Sources vérifiées

- Session : `Frontend/Rhis_report_gen/src/app/features/auth/auth.interceptor.ts`, `services/auth.service.ts`, `RHIS/src/main/java/RHIS/com/RHIS/auth/SecurityConfig.java`, `auth/AuthController.java`, `auth/services/AuthServiceImpl.java`.
- Catalogue : `RHIS/src/main/java/RHIS/com/RHIS/dataset/bootstrap/{DataSetInitializer,CatalogMetadataSeeder}.java`, `dataset/controller/DataSetAdministrationController.java`, `dataset/service/DataSetAdministrationService.java`, `bot/catalog/ReportCatalogProvider.java`.
- Assistant : `RHIS/src/main/java/RHIS/com/RHIS/bot/{BotReportService,BotReportPlanner,BotExceptionHandler}.java` et `bot/controller/BotReportController.java`.
- Rapports : `RHIS/src/main/java/RHIS/com/RHIS/report/service/{ReportGenerationService,ReportGenerationWorker,ReportExportService,ReportExportWorker,ReportDefinitionResolver}.java`, `report/controller/ReportExportController.java`, `report/export/{PdfReportExportWriter,XlsxReportExportWriter}.java`.
- Documentation de contexte : `RHIS/docs/flows/01` à `07`. Le code prévaut lorsque cette documentation est ancienne.

## Précisions de lecture

- Les workers sont internes à la JVM ; la base persiste les états et le disque local conserve snapshots et exports. Aucun broker distribué ni stockage cloud n’est représenté faute de preuve dans cette implémentation.
- Le schéma IA déroule les branches conditionnelles sur une même ligne de temps. La clarification est facultative. Chaque requête autorise au plus deux appels IA ; la réponse à une question constitue une nouvelle requête avec son contexte.
- L’exhaustivité est développée côté serveur lorsque le plan emploie `allFieldsDatasetIds`. Une sélection explicite reste stricte. Le filtre ne sélectionne pas implicitement une colonne de sortie.
- Les exports utilisent le snapshot de la génération ; ils ne relisent pas les données métier pour chaque format. Les endpoints de suivi et téléchargement contrôlent le propriétaire.
- Le refresh automatique existe dans Angular, contrairement à l’ancienne introduction du document `06-authentication-and-refresh.md`. Le logout actuel efface les cookies ; aucune révocation en base n’est appelée par son contrôleur.
- Les descriptions/alias sont préremplis au démarrage lorsqu’ils sont absents. Cette opération ne modifie pas les autorisations.

## Reproduction et preuves

Les fichiers `.json` sont les spécifications éditables. Les reçus `.html.delivery.json` contiennent les empreintes SHA-256 et les neuf contrôles Archify. Les fichiers `.visual-check.json` et captures associées enregistrent séparément les contrôles navigateur aux résolutions 1440×900, 1600×1000, 1920×1080 et 2048×1320.

Commandes : `node <archify>/bin/archify.mjs validate <type> <spec.json> --quality showcase --json`, puis `deliver <type> <spec.json> <vue.html> --quality showcase --json`, et `visual-check <vue.html> --json`.

Résultat final : cinq livraisons, chacune avec 9/9 contrôles, aucune erreur ni avertissement de composition. Contrôles navigateur réussis pour les cinq vues aux quatre résolutions. Captures finales inspectées en clair et sombre ; récapitulatif et empreintes dans verification.json.

