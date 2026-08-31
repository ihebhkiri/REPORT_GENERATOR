# Graph Report - RHIS  (2026-08-31)

## Corpus Check
- 333 files · ~166,808 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2777 nodes · 5890 edges · 175 communities (152 shown, 23 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 422 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `3e2129bd`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.springframework.data.jpa.repository.JpaRepository
- org.springframework.http.ResponseEntity
- ReportControllerSecurityTest
- ExportComponent
- FileSystemReportArtifactStorage
- org.junit.jupiter.api.Test
- AuthController
- DESIGN.md
- com.fasterxml.jackson.databind.ObjectMapper
- FilterEditorComponent
- DataSetField
- ReportGenerationService
- ReportSqlBuilder
- configuration.component.spec.ts
- Administration des données du Report Builder — spécification Figma
- PdfReportExportWriter
- UserService
- ReportSnapshotStorageTest
- ReportJobProperties
- ReportExportFormat
- DataSetAdministrationService
- org.springframework.stereotype.Component
- DataSetFieldResponse
- TableRelationProjection
- dependencies
- devDependencies
- ReportField
- configuration.component.ts
- ConfigurationComponent
- RHIS — génération asynchrone et export temporaire des rapports
- JwtService
- org.springframework.context.annotation.Bean
- UserEntity
- DataSetRepository
- Administration des tables et champs — spécification UI/UX
- DataSetFieldType
- XlsxReportExportWriter
- app.routes.ts
- Refactoring KISS du workflow Export front et back
- DataSetEntity
- AuthServiceImpl.java
- ReportArtifactStorage
- RapportsComponent
- RHIS — simplification du parcours de configuration des rapports
- ReportPreviewPostgresIntegrationTest.java
- ReportExportService
- Refactoring ciblé de la page Export
- report-generation.model.ts
- UserPrincipal
- 02 — Définition du rapport : fields, filters, sorts et brouillon local
- options
- 04 — Génération complète asynchrone
- 05 — Export PDF/XLSX et téléchargement
- Rapport d'avancement — Projet RHIS
- Rhis_report_gen
- mvnw
- 01 — Sélection des datasets et chargement de la configuration
- production
- @schematics/angular:component
- package.json
- Intégrer un aperçu automatique à la configuration des rapports
- 06 — Authentification par cookies et refresh token
- development
- scripts
- architect
- RhisApplication
- ReportPreviewRequest
- Verified current behavior
- IhebComponent
- InvalidTokenException
- PasswordMismatchException
- TokenExpiredException
- flows/README.md
- 03 — Preview synchrone du rapport
- Correction ciblée du layout de la page Export
- karma-chrome-launcher
- Global Constraints
- ForgotPasswordRequest.java
- ResetPasswordRequest.java
- CreateUserResponse.java
- UserResponse.java
- RHIS.com:RHIS
- org.springframework.transaction.annotation.Transactional
- Report export vertical stepper design
- File Map
- Global Constraints
- Global Constraints
- RhisReportGen
- RHIS — documentation ciblée du flux de rapports
- Étapes détaillées
- Global Constraints
- XLSX Temporal Snapshot Compatibility Design
- Research: configuration des rapports et aperçu existant
- Global Constraints
- Global Constraints
- Global Constraints
- Global Constraints
- Login détaillé
- Independent report formats with PrimeNG implementation plan
- AGENTS.md
- @angular/platform-browser
- @angular/router
- primeicons
- @types/jasmine
- typescript
- Implémenter l'exposition administrative des datasets et de leurs champs
- Required sections
- BotReportRequest
- Codex Project Workflow Design
- lombok.RequiredArgsConstructor
- <Action-oriented plan title>
- RoleEntity
- Research: <topic>
- DatasetExposureComponent
- report-generation.service.ts
- .getUserStats
- Frontend and UI guidelines
- RHIS Frontend Agent Instructions
- RHIS Backend Agent Instructions
- UserService.java
- File Structure
- ReportJobCleanupService
- Bot Report Ponytail Refactor Implementation Plan
- Global Constraints
- Research: refonte master-detail de l’exposition des datasets
- Bot Report Ponytail Refactor Research
- plans/README.md
- progress/README.md
- research/README.md
- report-assistant.component.ts
- rhis_bot — Interface Angular de l'assistant de rapports
- rhis_bot — Rapport généré à partir d'une phrase naturelle (Spring AI + Mistral)
- ReportSnapshotMetadata
- File Map
- RHIS Bot — Rapports multi-datasets reliés
- 07 — Rapport à partir d'une phrase naturelle
- Remplacer les accordéons d’exposition par un master-detail
- RHIS shared header and dataset UI — Implementation Plan
- File Map
- RHIS Bot Multi-Dataset Reports Research
- RHIS Bot — Clarification structurée sans répétition
- export.component.ts
- Bot Report — Refactoring Ponytail chirurgical
- Angular Composer, Preview and Shared Layout Implementation Plan
- File Map
- Administration UI — progression
- rhis_bot — progression
- app.config.ts
- RHIS — Modernisation du composer et repli de l’aperçu
- Milestone 1: Installer l’unique Toast global
- Milestone 2: Introduire l’état master-detail, le feedback fonctionnel et la protection dirty
- Milestone 3: Remplacer l’accordéon par l’interface master-detail accessible et responsive
- Milestone 4: Vérifier la régression, l’accessibilité et documenter le flux réel
- Global Constraints
- Progress — exposition administrative des datasets et champs
- RHIS Bot Related Datasets — Progress
- Administration UI — analyse avant aperçu
- Étapes détaillées
- Dataset exposure Ponytail — plan approuvé
- Dataset exposure Ponytail — progrès
- PasswordResetTokenEntity
- Proposed approach
- Dataset exposure Ponytail — research
- RHIS — direction et maquette de l’administration
- DataSetExposureConfigurationResponse
- RHIS — validation Angular, 28 août 2026
- RHIS Bot Structured Clarification — Progress
- ReportRelatedCardComponent
- Affected files and symbols
- environment.development.ts
- progress/2026-08-30-rhis-bot-angular-interface.md
- ReportConfigurationLoader
- BaseEntity
- build

## God Nodes (most connected - your core abstractions)
1. `DataSetField` - 54 edges
2. `DataSetEntity` - 53 edges
3. `UserEntity` - 43 edges
4. `ReportPreviewRequest` - 42 edges
5. `ReportJobProperties` - 41 edges
6. `ReportDefinitionResolver` - 40 edges
7. `ExportComponent` - 37 edges
8. `FilterEditorComponent` - 34 edges
9. `DataSetFieldType` - 34 edges
10. `ReportExportFormat` - 34 edges

## Surprising Connections (you probably didn't know these)
- `ExportFormatDefinition` --references--> `ReportExportFormat`  [EXTRACTED]
  Frontend/Rhis_report_gen/src/app/features/rapports/pages/export/export.component.ts → Frontend/Rhis_report_gen/src/app/features/rapports/models/report-generation.model.ts
- `supportedOperators()` --references--> `FilterOperator`  [EXTRACTED]
  RHIS/src/main/java/RHIS/com/RHIS/dataset/model/DataSetFieldType.java → RHIS/src/main/java/RHIS/com/RHIS/dataset/model/FilterOperator.java
- `ExposureModeOption` --references--> `DatasetExposureMode`  [EXTRACTED]
  Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.ts → Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.model.ts
- `ReportPreviewColumn` --references--> `DatasetFieldType`  [EXTRACTED]
  Frontend/Rhis_report_gen/src/app/features/rapports/models/report-preview.model.ts → Frontend/Rhis_report_gen/src/app/features/rapports/models/dataset-field.model.ts
- `OperatorOption` --references--> `FilterOperator`  [EXTRACTED]
  Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration/components/filter-editor/filter-editor.component.ts → Frontend/Rhis_report_gen/src/app/features/rapports/models/dataset-field.model.ts

## Import Cycles
- None detected.

## Communities (175 total, 23 thin omitted)

### Community 0 - "org.springframework.data.jpa.repository.JpaRepository"
Cohesion: 0.07
Nodes (32): jakarta.persistence.Entity, jakarta.persistence.MappedSuperclass, jakarta.persistence.PrePersist, jakarta.persistence.Table, lombok.Getter, lombok.NoArgsConstructor, lombok.Setter, org.springframework.boot.context.properties.ConfigurationProperties (+24 more)

### Community 1 - "org.springframework.http.ResponseEntity"
Cohesion: 0.22
Nodes (11): org.springframework.http.converter.HttpMessageNotReadableException, org.springframework.http.HttpStatus, org.springframework.http.ProblemDetail, org.springframework.http.ResponseEntity, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.RestControllerAdvice, org.springframework.web.bind.MethodArgumentNotValidException, BotExceptionHandler (+3 more)

### Community 2 - "ReportControllerSecurityTest"
Cohesion: 0.13
Nodes (18): org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest, org.springframework.context.annotation.Import, org.springframework.scheduling.annotation.EnableAsync, org.springframework.security.authentication.AuthenticationManager, org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration, org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity, org.springframework.security.config.annotation.web.builders.HttpSecurity, org.springframework.security.config.annotation.web.configuration.EnableWebSecurity (+10 more)

### Community 3 - "ExportComponent"
Cohesion: 0.15
Nodes (3): ReportExportFormat, ExportComponent, Component

### Community 4 - "FileSystemReportArtifactStorage"
Cohesion: 0.27
Nodes (5): FileSystemReportArtifactStorage, Override, StorageDeleteException, ArtifactWriter, FunctionalInterface

### Community 5 - "org.junit.jupiter.api.Test"
Cohesion: 0.11
Nodes (7): org.junit.jupiter.api.Test, ReportQueryTimeoutException, BotAiPropertiesTest, DataSetFieldTypeTest, DataSetFieldRepositoryTest, ReportJobConfigurationTest, ReportPreviewExecutorTest

### Community 6 - "AuthController"
Cohesion: 0.14
Nodes (11): org.springframework.http.ResponseCookie, AuthController, GetMapping, PostMapping, RequestMapping, RestController, LoginRequest, LoginResponse (+3 more)

### Community 7 - "DESIGN.md"
Cohesion: 0.08
Nodes (25): Accessibilité, Actions, Affordance, Charge cognitive, Couleurs, Densité, Espacement, Feedback (+17 more)

### Community 8 - "com.fasterxml.jackson.databind.ObjectMapper"
Cohesion: 0.17
Nodes (8): com.fasterxml.jackson.core.type.TypeReference, com.fasterxml.jackson.databind.ObjectMapper, com.lowagie.text.pdf.PdfReader, ReportSnapshotReader, FunctionalInterface, ReportSnapshotWriter, RowSink, RowsSource

### Community 9 - "FilterEditorComponent"
Cohesion: 0.14
Nodes (4): DatasetFieldType, ReportPreviewColumn, FilterEditorComponent, Component

### Community 10 - "DataSetField"
Cohesion: 0.14
Nodes (9): DataSetField, Entity, Table, ReportValidationException, PathStep, RelationEdge, RelationKey, ReportDefinitionResolver (+1 more)

### Community 11 - "ReportGenerationService"
Cohesion: 0.08
Nodes (21): ReportGenerationResponse, Entity, Table, ReportGenerationEntity, ReportDefinitionUnavailableException, ReportExecutionException, ReportResourceNotFoundException, UnavailableReportElement (+13 more)

### Community 12 - "ReportSqlBuilder"
Cohesion: 0.19
Nodes (8): PreparedCountQuery, ResolvedFilter, ResolvedJoin, ResolvedJoinColumn, ResolvedReportDefinition, ResolvedSort, ReportSqlBuilder, ReportSqlBuilderTest

### Community 13 - "configuration.component.spec.ts"
Cohesion: 0.18
Nodes (11): DatasetField, Dataset, TableRelation, DatasetSelectionResult, DATASET_ICONS, DatasetAccordionView, RelatedDatasetAccumulator, RelatedDatasetView (+3 more)

### Community 14 - "Administration des données du Report Builder — spécification Figma"
Cohesion: 0.06
Nodes (34): `01 — Dataset active`, `02 — Dataset inactive`, `03 — Search & filters`, `04 — Unsaved changes`, `05 — Save feedback`, 10. Frames Figma attendues, 11. Composants Figma, 12. Design system et iconographie (+26 more)

### Community 15 - "PdfReportExportWriter"
Cohesion: 0.12
Nodes (9): java.util.function.IntConsumer, net.sf.jasperreports.engine.JasperPrint, net.sf.jasperreports.engine.JRDataSource, net.sf.jasperreports.engine.JRField, Override, PdfReportExportWriter, SnapshotDataSource, Override (+1 more)

### Community 16 - "UserService"
Cohesion: 0.12
Nodes (11): org.springframework.security.access.prepost.PreAuthorize, PatchMapping, BulkStatusRequest, CreateUserRequest, UserDataResponse, DeleteMapping, PostMapping, RequestMapping (+3 more)

### Community 17 - "ReportSnapshotStorageTest"
Cohesion: 0.19
Nodes (6): java.io.FilterInputStream, ObjectMapper, SnapshotConsumer, CloseTrackingInputStream, Override, ReportSnapshotStorageTest

### Community 18 - "ReportJobProperties"
Cohesion: 0.17
Nodes (8): PdfReader, ReportJobProperties, Column, CloseTrackingOutputStream, FunctionalInterface, Override, ReportExportWriterTest, ThrowingRunnable

### Community 19 - "ReportExportFormat"
Cohesion: 0.16
Nodes (14): org.springframework.stereotype.Service, CreateReportExportRequest, Entity, Table, ReportExportEntity, ReportExportFormat, PDF, XLSX (+6 more)

### Community 20 - "DataSetAdministrationService"
Cohesion: 0.24
Nodes (6): DataSetUpdate, FieldUpdate, UpdateDataSetExposureRequest, DataSetConfigurationException, DataSetAdministrationService, DataSetAdministrationServiceTest

### Community 21 - "org.springframework.stereotype.Component"
Cohesion: 0.15
Nodes (14): Connection, java.sql.PreparedStatement, java.sql.ResultSet, org.springframework.jdbc.core.JdbcTemplate, org.springframework.stereotype.Component, ReportPreviewColumnResponse, FunctionalInterface, PreparedStatement (+6 more)

### Community 22 - "DataSetFieldResponse"
Cohesion: 0.15
Nodes (9): DataSetController, GetMapping, RequestMapping, RestController, DataSetFieldResponse, DataSetResponse, JoinInfoProjection, TableRelationResponse (+1 more)

### Community 23 - "TableRelationProjection"
Cohesion: 0.12
Nodes (3): TableRelationProjection, Override, TestTableRelation

### Community 24 - "dependencies"
Cohesion: 0.09
Nodes (23): @angular/animations, @angular/cdk, @angular/common, @angular/compiler, @angular/core, @angular/forms, dependencies, @angular/animations (+15 more)

### Community 25 - "devDependencies"
Cohesion: 0.09
Nodes (23): @angular/build, @angular/cli, @angular/compiler-cli, devDependencies, @angular/build, @angular/cli, @angular/compiler-cli, jasmine-core (+15 more)

### Community 26 - "ReportField"
Cohesion: 0.14
Nodes (13): FilterOperator, ReportFilterRequest, ColumnSelectorComponent, Component, FilterEditorState, FilterFieldOption, FilterFieldOptionGroup, FilterRowForm (+5 more)

### Community 27 - "configuration.component.ts"
Cohesion: 0.09
Nodes (20): ReportPreviewCell, ReportPreviewRequest, ReportPreviewResponse, ReportPreviewRow, ReportSortRequest, SortDirection, PreviewPanelComponent, PreviewStatus (+12 more)

### Community 28 - "ConfigurationComponent"
Cohesion: 0.14
Nodes (3): ConfigurationComponent, Component, ReportConfigurationLoadResult

### Community 29 - "RHIS — génération asynchrone et export temporaire des rapports"
Cohesion: 0.07
Nodes (29): 10. Sécurité et erreurs, 11. Frontend Angular, 12. Tests et critères d’acceptation, 13. Hors périmètre, 14. Ordre de réalisation recommandé, 1. Objectif, 2. Principes retenus, 3.1 Depuis Configuration (+21 more)

### Community 30 - "JwtService"
Cohesion: 0.18
Nodes (9): io.jsonwebtoken.Claims, jakarta.servlet.FilterChain, jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse, javax.crypto.SecretKey, org.springframework.web.filter.OncePerRequestFilter, Override, JwtService (+1 more)

### Community 31 - "org.springframework.context.annotation.Bean"
Cohesion: 0.35
Nodes (6): org.springframework.boot.context.properties.EnableConfigurationProperties, org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration, org.springframework.core.task.TaskExecutor, BotConfig, ReportJobConfiguration

### Community 32 - "UserEntity"
Cohesion: 0.24
Nodes (9): org.springframework.data.domain.Page, org.springframework.data.domain.Pageable, org.springframework.data.jpa.repository.Lock, Entity, Getter, Setter, Table, UserEntity (+1 more)

### Community 33 - "DataSetRepository"
Cohesion: 0.17
Nodes (10): org.junit.jupiter.api.extension.ExtendWith, org.mockito.junit.jupiter.MockitoExtension, org.springframework.data.jpa.repository.EntityGraph, org.springframework.data.jpa.repository.Query, DataSetMapper, DataSetFieldRepository, DataSetRepository, DataSetServiceImpl (+2 more)

### Community 34 - "Administration des tables et champs — spécification UI/UX"
Cohesion: 0.06
Nodes (34): 10. Sauvegarde groupée, 11. États à représenter, 12. Données mockées, 13. Direction visuelle, 14. Responsive, 15. Accessibilité visuelle, 16. Écart technique observé, 17. Hors périmètre (+26 more)

### Community 35 - "DataSetFieldType"
Cohesion: 0.13
Nodes (13): DataSetFieldType, BOOLEAN, DATE, DATE_TIME, DECIMAL, INTEGER, OFFSET_DATE_TIME, TEXT (+5 more)

### Community 36 - "XlsxReportExportWriter"
Cohesion: 0.24
Nodes (5): Cell, CellStyle, Override, XlsxReportExportWriter, Workbook

### Community 37 - "app.routes.ts"
Cohesion: 0.14
Nodes (13): routes, CurrentUser, LoginCredentials, adminGuard(), runGuard(), LoginComponent, Component, AuthService (+5 more)

### Community 38 - "Refactoring KISS du workflow Export front et back"
Cohesion: 0.07
Nodes (27): 10. Validation, 11. Risques, 12. Critère de fin, 1. Objectif, 2. Périmètre, 3. État actuel vérifié, 4.1 Reprise réseau du polling frontend, 4.2 Authentification backend (+19 more)

### Community 39 - "DataSetEntity"
Cohesion: 0.18
Nodes (9): lombok.extern.slf4j.Slf4j, org.springframework.boot.ApplicationArguments, org.springframework.boot.ApplicationRunner, ColumnInfo, DataSetInitializer, Override, DataSetEntity, Entity (+1 more)

### Community 40 - "AuthServiceImpl.java"
Cohesion: 0.08
Nodes (19): org.springframework.security.authentication.UsernamePasswordAuthenticationToken, org.springframework.security.core.userdetails.UserDetails, AllArgsConstructor, Builder, Entity, Getter, NoArgsConstructor, Setter (+11 more)

### Community 41 - "ReportArtifactStorage"
Cohesion: 0.18
Nodes (5): org.slf4j.Logger, ReportExportWriter, ReportExportWorker, ReportArtifactStorage, ReportExportWorkerTest

### Community 43 - "RHIS — simplification du parcours de configuration des rapports"
Cohesion: 0.10
Nodes (20): 10. Impact backend, 1. Présentation des colonnes, 2. Champs filtrables, 3. Opérateurs visibles, 4. Valeurs temporelles PrimeNG, 5. Page de configuration, 6. Page source de données, 7. Flux d’état (+12 more)

### Community 44 - "ReportPreviewPostgresIntegrationTest.java"
Cohesion: 0.10
Nodes (18): org.springframework.boot.test.context.SpringBootTest, org.springframework.test.annotation.DirtiesContext, org.springframework.test.context.ActiveProfiles, org.testcontainers.junit.jupiter.Testcontainers, org.testcontainers.postgresql.PostgreSQLContainer, FilterOperator, BETWEEN, CONTAINS (+10 more)

### Community 45 - "ReportExportService"
Cohesion: 0.24
Nodes (4): ReportExportResponse, ReportConflictException, DownloadPayload, ReportExportService

### Community 46 - "Refactoring ciblé de la page Export"
Cohesion: 0.10
Nodes (19): Changements explicitement rejetés, Design validé, Diagnostic, Mutualisation RxJS, Naming ciblé, Nettoyage SCSS, Objectif, Ordre d’implémentation et risques (+11 more)

### Community 47 - "report-generation.model.ts"
Cohesion: 0.19
Nodes (8): ReportDraft, ReportExportStatus, ReportGenerationPhase, ReportGenerationStatus, UnavailableReportElement, routes, ReportDraftStorageService, Injectable

### Community 48 - "UserPrincipal"
Cohesion: 0.17
Nodes (8): Override, UserPrincipal, DeleteMapping, GetMapping, PostMapping, RequestMapping, RestController, ReportGenerationController

### Community 49 - "02 — Définition du rapport : fields, filters, sorts et brouillon local"
Cohesion: 0.11
Nodes (18): 02 — Définition du rapport : fields, filters, sorts et brouillon local, Actualisation de la preview, Brouillon local et restauration, Cas d'erreur, Chaîne complète, Colonnes, Construction de l'état frontend, Diagramme de séquence (+10 more)

### Community 50 - "options"
Cohesion: 0.25
Nodes (11): options, assets, browser, inlineStyleLanguage, polyfills, styles, tsConfig, options (+3 more)

### Community 51 - "04 — Génération complète asynchrone"
Cohesion: 0.11
Nodes (18): 04 — Génération complète asynchrone, Cas d'erreur, Chaîne complète, Cleanup et expiration, Count, Création transactionnelle, Diagramme de séquence, Déclencheur utilisateur (+10 more)

### Community 52 - "05 — Export PDF/XLSX et téléchargement"
Cohesion: 0.11
Nodes (18): 05 — Export PDF/XLSX et téléchargement, Cas d'erreur, Chaîne complète, Création de l'export, Diagramme de séquence, Dispatch et sélection de l'interface, Déclencheurs utilisateur, En langage métier (+10 more)

### Community 53 - "Rapport d'avancement — Projet RHIS"
Cohesion: 0.11
Nodes (17): 1. Objectif actuel du projet, 2. Travaux réalisés, 3. Problèmes identifiés et corrigés, 4. Vérifications effectuées, 5. État actuel, 6. Prochaines étapes proposées, 7. Résumé, API REST et format de réponse (+9 more)

### Community 54 - "Rhis_report_gen"
Cohesion: 0.20
Nodes (9): newProjectRoot, projects, Rhis_report_gen, prefix, projectType, root, sourceRoot, $schema (+1 more)

### Community 55 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 56 - "01 — Sélection des datasets et chargement de la configuration"
Cohesion: 0.18
Nodes (11): 01 — Sélection des datasets et chargement de la configuration, Cas d'erreur, Chaîne complète, Diagramme de séquence, Déclencheur utilisateur, En langage métier, Participants du flow, Points importants à retenir (+3 more)

### Community 57 - "production"
Cohesion: 0.25
Nodes (8): serve, production, budgets, buildTarget, outputHashing, builder, configurations, defaultConfiguration

### Community 58 - "@schematics/angular:component"
Cohesion: 0.25
Nodes (8): schematics, skipTests, style, type, skipTests, type, @schematics/angular:component, @schematics/angular:service

### Community 59 - "package.json"
Cohesion: 0.25
Nodes (7): name, prettier, overrides, printWidth, singleQuote, private, version

### Community 60 - "Intégrer un aperçu automatique à la configuration des rapports"
Cohesion: 0.07
Nodes (27): Affected files and symbols, Baseline réelle avant toute modification de sources, Chargement, génération, retry et déduplication, Commandes reproductibles, Critères de clôture observables, Current behavior, Decision Log, Exécution manuelle du 2026-08-29 (+19 more)

### Community 61 - "06 — Authentification par cookies et refresh token"
Cohesion: 0.13
Nodes (15): 06 — Authentification par cookies et refresh token, Cas d'erreur, Diagramme de séquence — login, Diagramme de séquence — refresh, En langage métier, Flow login — déclencheur et chaîne, Flow refresh — backend uniquement, `/me` et logout (+7 more)

### Community 62 - "development"
Cohesion: 0.33
Nodes (6): development, buildTarget, extractLicenses, fileReplacements, optimization, sourceMap

### Community 63 - "scripts"
Cohesion: 0.33
Nodes (6): scripts, build, ng, start, test, watch

### Community 64 - "architect"
Cohesion: 0.40
Nodes (5): extract-i18n, test, builder, architect, builder

### Community 65 - "RhisApplication"
Cohesion: 0.60
Nodes (3): org.springframework.boot.autoconfigure.SpringBootApplication, org.springframework.scheduling.annotation.EnableScheduling, RhisApplication

### Community 66 - "ReportPreviewRequest"
Cohesion: 0.32
Nodes (4): ReportFilterRequest, ReportPreviewRequest, ReportPreviewResponse, ReportPreviewServiceTest

### Community 67 - "Verified current behavior"
Cohesion: 0.06
Nodes (31): Approches comparées, Backend et sécurité, Catalogue des relations, Catalogue public des champs, Catalogue public des tables principales, Champs techniques invisibles, Conclusions for planning, Contraintes UI vérifiées (+23 more)

### Community 72 - "flows/README.md"
Cohesion: 0.17
Nodes (6): Architecture transversale, Constats critiques confirmés, Flows fonctionnels et techniques RHIS, Frontière fonctionnelle observée, Vue d'ensemble, Vérification exécutée

### Community 73 - "03 — Preview synchrone du rapport"
Cohesion: 0.17
Nodes (12): 03 — Preview synchrone du rapport, Accès base et cardinalité, Cas d'erreur, Chaîne complète, Chemin retour et erreurs UI, Diagramme de séquence, Déclencheur utilisateur, En langage métier (+4 more)

### Community 74 - "Correction ciblée du layout de la page Export"
Cohesion: 0.18
Nodes (10): Cartes de format, Composition générale, Correction ciblée du layout de la page Export, Densité de l’état READY, Design validé — option A, Diagnostic, Objectif, Périmètre d’implémentation (+2 more)

### Community 76 - "Global Constraints"
Cohesion: 0.18
Nodes (10): Async Report Generation and Export Implementation Plan, Global Constraints, Task 1: Shared definition resolution and SQL variants, Task 2: Job persistence, public contracts, and owner security, Task 3: Bounded execution and streamed snapshot, Task 4: XLSX and PDF export workers, Task 5: Expiration, cancellation, abandoned jobs, and download, Task 6: Angular API, draft persistence, and generation submission (+2 more)

### Community 82 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.14
Nodes (7): org.springframework.transaction.annotation.Transactional, InProcessReportJobDispatcher, Override, ReportGenerationWorker, ReportExportWork, ReportJobStateService, ReportJobStateServiceTest

### Community 83 - "Report export vertical stepper design"
Cohesion: 0.20
Nodes (9): Accessibility, Error and edge states, Goal, Report export vertical stepper design, Responsive behavior, Technical scope, Validated format card — option B, Validated layout (+1 more)

### Community 84 - "File Map"
Cohesion: 0.22
Nodes (8): File Map, Global Constraints, Targeted Report Export Refactoring Implementation Plan, Task 1: Protect the duplicated polling behavior with characterization tests, Task 2: Apply the approved targeted domain naming, Task 3: Centralize the transient polling retry policy locally, Task 4: Simplify repeated template evaluation without extracting a component, Task 5: Run complete regression and production-build verification

### Community 85 - "Global Constraints"
Cohesion: 0.22
Nodes (8): Global Constraints, Report Export Front/Back KISS Refactoring Implementation Plan, Task 1: Corriger la reprise réseau du polling d’export, Task 2: Clarifier le naming HTTP et les dépendances Angular, Task 3: Restaurer la frontière d’authentification du workflow Report/Export, Task 4: Nommer explicitement l’enregistrement de progression backend, Task 5: Simplifier les détails internes backend sans nouvelle abstraction, Task 6: Validation end-to-end et contrôle de périmètre

### Community 86 - "Global Constraints"
Cohesion: 0.22
Nodes (8): Global Constraints, Report Builder Data Administration Figma Implementation Plan, Task 1: Create and inspect the target Figma file, Task 2: Establish visual foundations and the Components area, Task 3: Compose `01 — Dataset active`, Task 4: Compose inactive, search and unsaved states, Task 5: Compose save feedback variants, Task 6: Document responsive behavior and perform final QA

### Community 87 - "RhisReportGen"
Cohesion: 0.25
Nodes (7): Additional Resources, Building, Code scaffolding, Development server, RhisReportGen, Running end-to-end tests, Running unit tests

### Community 88 - "RHIS — documentation ciblée du flux de rapports"
Cohesion: 0.25
Nodes (7): Invariants à rendre explicites, Objectif, Périmètre backend, Périmètre frontend, RHIS — documentation ciblée du flux de rapports, Règles rédactionnelles, Validation

### Community 89 - "Étapes détaillées"
Cohesion: 0.29
Nodes (7): 1. Départ et état Angular, 2. Frontière HTTP et sécurité effective, 3. Controller et validation structurelle, 4. Résolution métier, 5. Construction SQL, 6. Exécution et mapping, Étapes détaillées

### Community 90 - "Global Constraints"
Cohesion: 0.29
Nodes (6): Global Constraints, Task 1: Reproduce legacy temporal values through the XLSX public seam, Task 2: Read canonical and legacy temporal representations, Task 3: Canonicalize all newly-created snapshots to ISO-8601, Task 4: Full verification, XLSX Temporal Snapshot Compatibility Implementation Plan

### Community 91 - "XLSX Temporal Snapshot Compatibility Design"
Cohesion: 0.29
Nodes (6): Context, Decision, Error handling, Scope, Verification, XLSX Temporal Snapshot Compatibility Design

### Community 92 - "Research: configuration des rapports et aperçu existant"
Cohesion: 0.07
Nodes (26): Ancien comportement à remplacer explicitement, Aperçu manuel, Chargement et propriété de l’état, Chargement initial et erreurs de configuration, Commandes réellement exécutées, Conclusions for planning, Couverture et limites, Data and control flow (+18 more)

### Community 93 - "Global Constraints"
Cohesion: 0.33
Nodes (5): Global Constraints, Report Export Vertical Stepper Implementation Plan, Task 1: Derived export workflow state, Task 2: Semantic wizard and progressive timeline, Task 3: Error states and regression verification

### Community 94 - "Global Constraints"
Cohesion: 0.33
Nodes (5): Global Constraints, Report Code Documentation Implementation Plan, Task 1: Document Angular report configuration responsibilities, Task 2: Document backend preview validation, SQL, and JDBC constraints, Task 3: Cross-project documentation-only verification

### Community 95 - "Global Constraints"
Cohesion: 0.33
Nodes (5): Global Constraints, RHIS End-to-End Flow Documentation Implementation Plan, Task 1: Map implemented flows, Task 2: Write flow documents, Task 3: Verify documentation

### Community 96 - "Global Constraints"
Cohesion: 0.40
Nodes (4): Global Constraints, Report Export Layout Correction Implementation Plan, Task 1: Alléger l’état READY et rendre les actions fluides, Task 2: Donner toute la largeur au workflow et contenir les cartes

### Community 97 - "Login détaillé"
Cohesion: 0.40
Nodes (5): 1. Validation frontend, 2. Authentification Spring Security, 3. Création des tokens, 4. Cookies et retour UI, Login détaillé

### Community 98 - "Independent report formats with PrimeNG implementation plan"
Cohesion: 0.50
Nodes (3): Implementation, Independent report formats with PrimeNG implementation plan, Verification

### Community 99 - "AGENTS.md"
Cohesion: 0.13
Nodes (14): 1. Research, 2. Plan, 3. Implement, 4. Verify and review, Angular and UI routing, Code quality, Graphify, Heuristique	Exemple (+6 more)

### Community 105 - "Implémenter l'exposition administrative des datasets et de leurs champs"
Cohesion: 0.07
Nodes (29): 1. API d'administration minimale, 2. Une extraction unique des champs, indépendante du mode de table, 3. Corriger le graphe et centraliser l'enforcement, 4. Preview, génération et exports existants, 5. Page Angular administrative, Affected files and symbols, Backend, Catalogue et résolution actuels (+21 more)

### Community 106 - "Required sections"
Cohesion: 0.08
Nodes (23): Affected files and symbols, Approval, Change discipline, Completion, Current behavior, Decision Log, Draft, Executable plans (+15 more)

### Community 107 - "BotReportRequest"
Cohesion: 0.06
Nodes (27): CallResponseSpec, ChatClientRequestSpec, org.junit.jupiter.api.BeforeEach, org.springframework.ai.chat.client.ChatClient, BotReportPlanner, Builder, BotReportService, ReportFilterRequest (+19 more)

### Community 108 - "Codex Project Workflow Design"
Cohesion: 0.11
Nodes (17): Acceptance Criteria, Backend `AGENTS.md`, Codex Project Workflow Design, Complex or cross-repository task, Constraints, Deliberately Excluded Mechanisms, Files and Responsibilities, Frontend `AGENTS.md` (+9 more)

### Community 109 - "lombok.RequiredArgsConstructor"
Cohesion: 0.15
Nodes (16): InputStreamResource, lombok.RequiredArgsConstructor, org.springframework.core.io.InputStreamResource, org.springframework.web.bind.annotation.DeleteMapping, org.springframework.web.bind.annotation.GetMapping, org.springframework.web.bind.annotation.PostMapping, org.springframework.web.bind.annotation.PutMapping, org.springframework.web.bind.annotation.RequestMapping (+8 more)

### Community 110 - "<Action-oriented plan title>"
Cohesion: 0.13
Nodes (14): <Action-oriented plan title>, Affected files and symbols, Current behavior, Decision Log, Milestone 1: <coherent result>, Milestone 2: <coherent result>, Outcomes & Retrospective, Progress (+6 more)

### Community 111 - "RoleEntity"
Cohesion: 0.13
Nodes (11): org.springframework.security.core.GrantedAuthority, RoleResponse, AllArgsConstructor, Builder, Entity, Getter, NoArgsConstructor, Setter (+3 more)

### Community 112 - "Research: <topic>"
Cohesion: 0.17
Nodes (11): Conclusions for planning, Data and control flow, Existing tests and validation commands, Invariants and constraints, Open questions, Question, Relevant files and symbols, Research: <topic> (+3 more)

### Community 113 - "DatasetExposureComponent"
Cohesion: 0.08
Nodes (17): DatasetExposureComponent, ExposureModeOption, Component, DatasetExposure, DatasetExposureConfiguration, DatasetExposureMode, DatasetExposureUpdate, FieldExposure (+9 more)

### Community 114 - "report-generation.service.ts"
Cohesion: 0.20
Nodes (5): ReportExport, ReportGeneration, ReportGenerationService, Injectable, environment

### Community 116 - "Frontend and UI guidelines"
Cohesion: 0.18
Nodes (10): Accessibility, Angular, Components and PrimeNG, Forms, Frontend and UI guidelines, Required states, Spacing and layout, Typography and readability (+2 more)

### Community 117 - "RHIS Frontend Agent Instructions"
Cohesion: 0.18
Nodes (10): Angular and TypeScript Rules, API and Security, Completion Standard, Operating Rules, Project Map, Proportional Workflow, RHIS Frontend Agent Instructions, Scope (+2 more)

### Community 118 - "RHIS Backend Agent Instructions"
Cohesion: 0.18
Nodes (10): Completion Standard, Java and Spring Rules, Operating Rules, PostgreSQL and SQL Rules, Project Map and Sources of Truth, Proportional Workflow, RHIS Backend Agent Instructions, Scope (+2 more)

### Community 119 - "UserService.java"
Cohesion: 0.15
Nodes (6): org.springframework.security.crypto.password.PasswordEncoder, RoleRepository, UpdateUserRequest, PutMapping, RoleNotFoundException, UserAlreadyExistsException

### Community 120 - "File Structure"
Cohesion: 0.20
Nodes (9): Completion Criteria, Data Administration UI Prototype Implementation Plan, File Structure, Global Constraints, Task 1: Define the administration model and realistic fixtures, Task 2: Implement synchronous page state and draft behavior, Task 3: Build the desktop master-detail interface, Task 4: Complete empty, loading, save feedback and responsive states (+1 more)

### Community 121 - "ReportJobCleanupService"
Cohesion: 0.22
Nodes (3): org.springframework.scheduling.annotation.Scheduled, ReportJobCleanupService, ReportJobDispatcher

### Community 122 - "Bot Report Ponytail Refactor Implementation Plan"
Cohesion: 0.12
Nodes (15): Affected files and symbols, Bot Report Ponytail Refactor Implementation Plan, Current behavior, Decision Log, Global Constraints, Milestone 1: Apply the surgical simplifications, Milestone 2: Repair and run focused tests, Milestone 3: Integrated build and final scope review (+7 more)

### Community 123 - "Global Constraints"
Cohesion: 0.29
Nodes (6): Codex Project Workflow Implementation Plan, Global Constraints, Task 1: Backend root instructions, Task 2: Reusable workflow documents, Task 3: Frontend root instructions, Task 4: Final verification

### Community 124 - "Research: refonte master-detail de l’exposition des datasets"
Cohesion: 0.09
Nodes (22): Conclusions for planning, Contrat et règles métier, Data and control flow, Erreurs et notifications, Invariants and constraints, Lecture et sélection proposées, Modification et navigation interne, Open questions (+14 more)

### Community 125 - "Bot Report Ponytail Refactor Research"
Cohesion: 0.17
Nodes (11): Bot Report Ponytail Refactor Research, Existing Contracts, Facts, Human Review Gate, Options, Question, Recommendation, Relevant Execution Path (+3 more)

### Community 129 - "report-assistant.component.ts"
Cohesion: 0.20
Nodes (10): BotReportService, Injectable, AssistantMessage, ClarificationContext, ReadyGeneration, ReportAssistantComponent, Component, BotReportRequest (+2 more)

### Community 130 - "rhis_bot — Interface Angular de l'assistant de rapports"
Cohesion: 0.09
Nodes (21): Approche retenue, Clarification, Composant, Contraintes du dépôt, Contrats TypeScript et service HTTP, Critères d'acceptation, Envoi initial, Erreurs et états (+13 more)

### Community 131 - "rhis_bot — Rapport généré à partir d'une phrase naturelle (Spring AI + Mistral)"
Cohesion: 0.13
Nodes (14): Approche retenue, Architecture et modules, Comportement actuel vérifié (faits du dépôt au 29/08/2026), Configuration (application.yaml), Contrat API (nouveau, unique endpoint de la phase 1), Design du prompt (system, en français), Fichiers attendus (chemins relatifs au backend `RHIS/`), Journal des décisions (+6 more)

### Community 132 - "ReportSnapshotMetadata"
Cohesion: 0.29
Nodes (5): net.sf.jasperreports.engine.design.JasperDesign, JasperDynamicTableConfigurer, ReportSnapshotMetadata, JasperDynamicTableConfigurerTest, JasperDesign

### Community 133 - "File Map"
Cohesion: 0.14
Nodes (13): Baselines préexistantes (à ne pas « corriger »), Completion Criteria, File Map, Global Constraints, rhis_bot — Plan d'implémentation (phase 1 backend), Rollback, Task 0 : empreintes Ponytail (baseline, aucun commit), Task 1 : dépendances Spring AI + configuration rhis.bot (+5 more)

### Community 134 - "RHIS Bot — Rapports multi-datasets reliés"
Cohesion: 0.15
Nodes (12): Catalogue LLM, Contrats préservés, Erreurs, Hors périmètre, Objectif, Plan LLM et prompt, RHIS Bot — Rapports multi-datasets reliés, Règles métier (+4 more)

### Community 135 - "07 — Rapport à partir d'une phrase naturelle"
Cohesion: 0.17
Nodes (11): 07 — Rapport à partir d'une phrase naturelle, Chaîne complète, Contrat HTTP, Diagramme de séquence, Données envoyées au LLM, Déclencheur utilisateur, En langage métier, Participants du flow (+3 more)

### Community 136 - "Remplacer les accordéons d’exposition par un master-detail"
Cohesion: 0.17
Nodes (11): Current behavior, Decision Log, Global constraints, Outcomes & Retrospective, Progress, Purpose and observable outcome, Remplacer les accordéons d’exposition par un master-detail, Risks and rollback (+3 more)

### Community 137 - "RHIS shared header and dataset UI — Implementation Plan"
Cohesion: 0.17
Nodes (11): 1. Header partagé, 2. Datasets fidèle à la maquette, 3. Integrated Validation, Decision Log, File Map, Global Constraints, Outcomes & Retrospective, Progress (+3 more)

### Community 138 - "File Map"
Cohesion: 0.17
Nodes (11): Completion Criteria, File Map, Global Constraints, Integrated Validation, RHIS Bot Related Datasets Implementation Plan, Rollback, Task 1: Catalogue multi-datasets compact, Task 2: Structured output et validation autoritative du Bot (+3 more)

### Community 139 - "RHIS Bot Multi-Dataset Reports Research"
Cohesion: 0.17
Nodes (11): Existing Contracts, Facts, Human Review Gate, Options, Question, Recommendation, Relevant Execution Path, RHIS Bot Multi-Dataset Reports Research (+3 more)

### Community 140 - "RHIS Bot — Clarification structurée sans répétition"
Cohesion: 0.17
Nodes (11): Backend et prompt, Contraintes de livraison, Contrat HTTP, Fichiers concernés, Frontend, Hors périmètre, Objectif, Problème (+3 more)

### Community 141 - "export.component.ts"
Cohesion: 0.17
Nodes (10): ApiProblem, EXPORT_FORMATS, EXPORT_WORKFLOW_STEPS, ExportFormatDefinition, ExportTagSeverity, ExportWorkflowState, ExportWorkflowStep, ExportWorkflowStepId (+2 more)

### Community 142 - "Bot Report — Refactoring Ponytail chirurgical"
Cohesion: 0.17
Nodes (11): Architecture et flux préservés, Backend, Bot Report — Refactoring Ponytail chirurgical, Contraintes de livraison, Frontend, Hors périmètre, Modifications retenues, Objectif (+3 more)

### Community 143 - "Angular Composer, Preview and Shared Layout Implementation Plan"
Cohesion: 0.20
Nodes (9): Angular Composer, Preview and Shared Layout Implementation Plan, Completion Criteria, File Map, Global Constraints, Integrated Validation, Rollback, Task 1: Composer RHIS Bot et attente assistant, Task 2: Aperçu replié et transition accessible (+1 more)

### Community 144 - "File Map"
Cohesion: 0.20
Nodes (9): Completion Criteria, File Map, Global Constraints, rhis_bot Angular Interface Implementation Plan, Task 0: Baseline Ponytail et état du worktree, Task 1: Contrats bot et service HTTP, Task 2: Composant conversationnel standalone, Task 3: Route `/assistant` et layout partagé (+1 more)

### Community 145 - "Administration UI — progression"
Cohesion: 0.20
Nodes (9): Administration UI — progression, Historique de phase 1, Objectif et limites, Phase 2 — état courant (prioritaire sur l'historique ci-dessous), Prochaine action sûre, Précisions de validation, Travail effectué et décisions, État de validation (+1 more)

### Community 146 - "rhis_bot — progression"
Cohesion: 0.20
Nodes (9): Blockers and Risks, Completed, Decisions, Intended Outcome, Next Safe Action, Remaining Work, Repository State, rhis_bot — progression (+1 more)

### Community 147 - "app.config.ts"
Cohesion: 0.36
Nodes (4): App, appConfig, RhisPreset, Component

### Community 148 - "RHIS — Modernisation du composer et repli de l’aperçu"
Cohesion: 0.22
Nodes (8): Aperçu de configuration, Contraintes, Hors périmètre, Layout partagé de configuration et d’export, Objectif, RHIS Bot, RHIS — Modernisation du composer et repli de l’aperçu, Validation

### Community 149 - "Milestone 1: Installer l’unique Toast global"
Cohesion: 0.25
Nodes (8): Exact changes, Files, Milestone 1: Installer l’unique Toast global, Objective, Preserved behavior, Success criteria, Tests, Validation

### Community 150 - "Milestone 2: Introduire l’état master-detail, le feedback fonctionnel et la protection dirty"
Cohesion: 0.25
Nodes (8): Exact changes, Files, Milestone 2: Introduire l’état master-detail, le feedback fonctionnel et la protection dirty, Objective, Preserved behavior, Success criteria, Tests, Validation

### Community 151 - "Milestone 3: Remplacer l’accordéon par l’interface master-detail accessible et responsive"
Cohesion: 0.25
Nodes (8): Exact changes, Files, Milestone 3: Remplacer l’accordéon par l’interface master-detail accessible et responsive, Objective, Preserved behavior, Success criteria, Tests, Validation

### Community 152 - "Milestone 4: Vérifier la régression, l’accessibilité et documenter le flux réel"
Cohesion: 0.25
Nodes (8): Exact changes, Files, Milestone 4: Vérifier la régression, l’accessibilité et documenter le flux réel, Objective, Preserved behavior, Success criteria, Tests, Validation

### Community 153 - "Global Constraints"
Cohesion: 0.25
Nodes (7): Completion Criteria, Global Constraints, RHIS Bot Structured Clarification Implementation Plan, Task 1: Contrat et prompt backend structurés, Task 2: Validation et garde-fou backend, Task 3: Frontend non récursif, Task 4: Documentation et validation

### Community 154 - "Progress — exposition administrative des datasets et champs"
Cohesion: 0.25
Nodes (7): Blockers, Completed, Current validation state, Decisions, Exact next action, Pre-existing dirty files, Progress — exposition administrative des datasets et champs

### Community 155 - "RHIS Bot Related Datasets — Progress"
Cohesion: 0.25
Nodes (7): Completed, Dirty Files Owned by This Task, Full-suite Failures, Next Action, Preserved Unrelated Changes, RHIS Bot Related Datasets — Progress, Validation

### Community 156 - "Administration UI — analyse avant aperçu"
Cohesion: 0.25
Nodes (7): Administration UI — analyse avant aperçu, Direction proposée, à approuver, Faits observés, Périmètre et sources, Révision après aperçu — 2026-08-28, Validation et limites, Validation humaine requise

### Community 157 - "Étapes détaillées"
Cohesion: 0.29
Nodes (7): 1. Chargement initial parallèle, 2. Lecture des datasets, 3. Découverte des relations, 4. Sélection UI et navigation, 5. Résolution de la route et chargement des fields, 6. Administration de l'exposition, Étapes détaillées

### Community 158 - "Dataset exposure Ponytail — plan approuvé"
Cohesion: 0.29
Nodes (6): Commandes de validation, Contraintes et fichiers, Dataset exposure Ponytail — plan approuvé, Décisions, risques et rollback, Outcomes, Progress

### Community 159 - "Dataset exposure Ponytail — progrès"
Cohesion: 0.29
Nodes (6): Changements livrés, Dataset exposure Ponytail — progrès, Limites et anomalies hors périmètre, Prochaine action, Validation, État et décisions

### Community 160 - "PasswordResetTokenEntity"
Cohesion: 0.20
Nodes (9): AllArgsConstructor, Builder, Entity, Getter, NoArgsConstructor, Setter, Table, PasswordResetTokenEntity (+1 more)

### Community 161 - "Proposed approach"
Cohesion: 0.33
Nodes (6): 1. Une infrastructure Toast globale, sans wrapper, 2. Un état master-detail local dérivé du draft, 3. Un layout CSS master-detail, sans `p-splitter`, 4. Deux protections complémentaires contre la sortie, 5. Réutilisabilité volontairement limitée, Proposed approach

### Community 162 - "Dataset exposure Ponytail — research"
Cohesion: 0.33
Nodes (5): Anomalies préexistantes séparées, Contrats et exclusions, Dataset exposure Ponytail — research, Décision, Faits et flux

### Community 163 - "RHIS — direction et maquette de l’administration"
Cohesion: 0.33
Nodes (5): Accord reçu, Artefact et validation, Design proposé, RHIS — direction et maquette de l’administration, Suite après validation explicite

### Community 164 - "DataSetExposureConfigurationResponse"
Cohesion: 0.42
Nodes (3): DataSetExposure, DataSetExposureConfigurationResponse, FieldExposure

### Community 165 - "RHIS — validation Angular, 28 août 2026"
Cohesion: 0.40
Nodes (4): Commandes et résultats, Livré, Navigateur et limites, RHIS — validation Angular, 28 août 2026

### Community 166 - "RHIS Bot Structured Clarification — Progress"
Cohesion: 0.40
Nodes (4): Completed, Preserved Work, RHIS Bot Structured Clarification — Progress, Validation

### Community 168 - "Affected files and symbols"
Cohesion: 0.50
Nodes (4): Affected files and symbols, Documentation, Feature dataset exposure, Infrastructure globale frontend

### Community 172 - "ReportConfigurationLoader"
Cohesion: 0.46
Nodes (3): SelectedDataset, ReportConfigurationLoader, Injectable

### Community 173 - "BaseEntity"
Cohesion: 0.60
Nodes (3): jakarta.persistence.EntityListeners, org.springframework.data.jpa.domain.support.AuditingEntityListener, BaseEntity

### Community 174 - "build"
Cohesion: 0.50
Nodes (4): build, builder, configurations, defaultConfiguration

## Knowledge Gaps
- **985 isolated node(s):** `$schema`, `version`, `newProjectRoot`, `projectType`, `style` (+980 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **23 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `UserEntity` connect `UserEntity` to `PasswordResetTokenEntity`, `org.springframework.data.jpa.repository.JpaRepository`, `ReportControllerSecurityTest`, `org.junit.jupiter.api.Test`, `DataSetEntity`, `AuthServiceImpl.java`, `BotReportRequest`, `ReportGenerationService`, `BaseEntity`, `ReportPreviewPostgresIntegrationTest.java`, `RoleEntity`, `UserService`, `UserPrincipal`?**
  _High betweenness centrality (0.012) - this node is a cross-community bridge._
- **Why does `DataSetFieldType` connect `DataSetFieldType` to `org.springframework.data.jpa.repository.JpaRepository`, `DataSetRepository`, `ReportPreviewRequest`, `XlsxReportExportWriter`, `ReportSnapshotMetadata`, `DataSetEntity`, `com.fasterxml.jackson.databind.ObjectMapper`, `DataSetField`, `ReportPreviewPostgresIntegrationTest.java`, `ReportSqlBuilder`, `PdfReportExportWriter`, `ReportJobProperties`, `org.springframework.stereotype.Component`, `DataSetFieldResponse`?**
  _High betweenness centrality (0.011) - this node is a cross-community bridge._
- **Why does `DataSetEntity` connect `DataSetEntity` to `org.springframework.data.jpa.repository.JpaRepository`, `DataSetRepository`, `ReportPreviewRequest`, `DataSetExposureConfigurationResponse`, `DataSetField`, `BotReportRequest`, `ReportPreviewPostgresIntegrationTest.java`, `ReportSqlBuilder`, `DataSetAdministrationService`, `DataSetFieldResponse`?**
  _High betweenness centrality (0.011) - this node is a cross-community bridge._
- **What connects `$schema`, `version`, `newProjectRoot` to the rest of the system?**
  _985 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.springframework.data.jpa.repository.JpaRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.0652014652014652 - nodes in this community are weakly interconnected._
- **Should `ReportControllerSecurityTest` be split into smaller, more focused modules?**
  _Cohesion score 0.13445378151260504 - nodes in this community are weakly interconnected._
- **Should `org.junit.jupiter.api.Test` be split into smaller, more focused modules?**
  _Cohesion score 0.10591133004926108 - nodes in this community are weakly interconnected._