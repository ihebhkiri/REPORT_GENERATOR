# Implémenter l'exposition administrative des datasets et de leurs champs

Cet ExecPlan est un document vivant régi par `../.agent/PLANS.md`. Tenir à jour `Progress`, `Surprises & Discoveries`, `Decision Log` et `Outcomes & Retrospective` pendant l'implémentation.

Date: 2026-08-24  
Status: Implemented — manual validation pending  
Research: `docs/research/2026-08-24-dataset-field-exposure.md`  
Related issue: N/A

## Purpose and observable outcome

Un administrateur pourra ouvrir une page dédiée, rechercher une table ou un champ, configurer en une sauvegarde explicite et transactionnelle l'usage de chaque table comme source principale et/ou comme cible de relation, puis exposer ou masquer chaque champ sans perdre ses préférences quand la table est désactivée.

Le report builder ne proposera que les métadonnées autorisées dans le contexte courant. Les mêmes règles seront vérifiées côté backend lors de la preview, de la création et de l'exécution d'une génération, de la création d'un export et du téléchargement. Une requête forgée ne pourra donc pas sélectionner, filtrer, trier ou exporter une table ou un champ masqué. Les clés de jointure et clés primaires masquées pourront rester utilisées uniquement comme détails techniques internes au SQL.

## Scope and non-goals

In scope:

- conserver `DataSetEntity.displayMain` et `DataSetEntity.displayRelated` comme unique source de vérité pour les tables ;
- conserver `DataSetField.visible` comme unique source de vérité pour l'exposition des champs ;
- représenter les quatre modes uniquement dans le frontend par une union TypeScript et deux fonctions de conversion pures ;
- ajouter une API d'administration protégée par `ROLE_ADMIN`, validée et transactionnelle ;
- conserver une récupération unique des champs par dataset actif, indépendante du rôle principal ou relation ;
- appliquer les contrôles dans le catalogue, le resolver de définition, les workers et les exports ;
- préserver les préférences lors de la synchronisation `information_schema` ;
- enrichir les erreurs métier avec la liste des éléments devenus indisponibles, sans supprimer la définition conservée ;
- fournir une page Angular standalone accessible au clavier et conforme à `../DESIGN.md` et `../docs/guidelines/frontend-ui.md` ;
- ajouter les tests backend, frontend et les mises à jour de documentation de flux correspondantes.

Non-goals:

- aucun enum Java, colonne, entité de configuration, Store, Facade ou repository supplémentaire pour représenter le mode ;
- aucune relation entrante ou multi-niveaux : seules les relations directes sortantes déjà supportées restent autorisées ;
- aucun ajout de groupement ou d'agrégation, ces fonctions n'existant pas dans le modèle de rapport actuel ; leur futur point d'entrée devra passer par le même resolver ;
- aucune suppression automatique d'un brouillon, d'une définition de génération, d'un snapshot ou d'un export existant ;
- aucune refonte du shell Angular, du SQL builder ou du cycle de vie général des jobs ;
- aucune migration pour `DataSetField.visible`, qui existe déjà ;
- aucune correction opportuniste de `spring.jpa.hibernate.ddl-auto=create`, des secrets de développement, des tests de base déjà rouges ou d'autres problèmes hors périmètre ;
- aucun commit, push, merge ou worktree sans demande explicite.

## Current behavior

Les faits et preuves détaillés sont dans `docs/research/2026-08-24-dataset-field-exposure.md`. Graphify (`../graphify-out/graph.json`) a servi à cibler les communautés concernées, puis chaque conclusion importante a été vérifiée dans le working tree. Le graphe a été régénéré le 24 août 2026 : ses 2 065 nœuds et 4 391 liens contiennent les renommages actuels et leurs appels. La source reste l'autorité pour les prédicats SQL et les règles métier internes aux méthodes.

### Source de vérité et valeurs par défaut

`DataSetEntity` porte actuellement :

- `displayMain` : autorise la table comme source principale. Le constructeur met cette valeur à `true` ;
- `displayRelated` : autorise la table comme cible secondaire d'une relation directe. Le constructeur ne l'assigne pas explicitement, donc sa valeur Java par défaut est `false` ;
- `active` : indique que la table technique existe encore dans la source synchronisée.

La conversion UI exacte sera :

| `displayMain` | `displayRelated` | Mode UI |
|---:|---:|---|
| `false` | `false` | `NONE` — Non exposée |
| `true` | `false` | `MAIN_ONLY` — Principale uniquement |
| `false` | `true` | `RELATED_ONLY` — Relation uniquement |
| `true` | `true` | `MAIN_AND_RELATED` — Principale et relation |

`DataSetField` porte déjà `active` et `visible`. Son constructeur initialise les deux à `true`. Aucune colonne ni migration d'exposition n'est nécessaire.

### Synchronisation

`DataSetInitializer.synchronizeDataSets(...)` et `synchronizeFields(...)` ont déjà le comportement de préservation attendu : une table ou un champ retrouvé est réactivé sans réécrire ses préférences, un élément disparu devient inactif sans perdre ses préférences et un nouveau champ est visible. La valeur par défaut proposée et documentée pour une nouvelle table est « Principale uniquement » (`displayMain=true`, `displayRelated=false`), afin de préserver le comportement courant tout en n'élargissant pas implicitement le graphe de relations.

Cette persistance est néanmoins neutralisée en environnement utilisant `spring.jpa.hibernate.ddl-auto=create`, car le schéma est recréé au démarrage. La correction de cette configuration et l'introduction éventuelle d'un outil de migration constituent un prérequis de déploiement persistant séparé, pas une migration de cette fonctionnalité.

### Catalogue et résolution actuels

- `DataSetServiceImpl.getDataSets()` utilise déjà `findByActiveTrueAndDisplayMainTrue()` : la sélection initiale respecte `displayMain`.
- `DataSetRepository.findVisibleTableRelations()` impose actuellement `displayRelated` à la source et à la cible. C'est incorrect : une source « Principale uniquement » doit conserver ses relations sortantes. La requête contient aussi un fragment invalide `display_r    elated`.
- `DataSetFieldRepository.findVisibleFieldsByDatasetId()` exige actuellement `DataSetField.visible` et `DataSetEntity.displayMain`. Le prédicat `displayMain` est incorrect à ce niveau : l'extraction intrinsèque d'un champ doit être identique que la table soit principale, liée, les deux ou non exposée. Seuls `DataSetEntity.active=true`, `DataSetField.active=true` et `DataSetField.visible=true` doivent participer à cette requête.
- `ReportDefinitionResolver` valide correctement la racine par `displayMain`, mais exige ensuite `displayMain` sur tous les datasets des champs. Une cible relation-only est donc rejetée. Le resolver est déjà le point central partagé par sélection, filtres et tris, et ne permet que les jointures directes sortantes.
- `ReportSqlBuilder` n'ajoute une clé de jointure technique à aucune projection par lui-même. La clé peut donc rester masquée tout en apparaissant dans le `JOIN ... ON`. La clé primaire utilisée pour l'ordre stable est également un détail interne.
- la preview appelle le resolver ; la création d'une génération le valide avant persistance et `ReportGenerationWorker` le résout à nouveau ;
- `ReportExportService.create()` et `openDownload()` ne revalident pas actuellement la définition. `ReportExportWorker` consomme seulement le snapshot et ne reçoit pas `definitionJson` ;
- il n'existe pas d'entité de « rapport enregistré ». Le brouillon est conservé en `sessionStorage` côté Angular et la définition des jobs est conservée dans `ReportGenerationEntity.definitionJson`.

### Sécurité et frontend

`SecurityConfig` protège déjà `/api/v1/admin/**` par le rôle `ADMIN` et active la method security. Les contrôleurs administratifs existants doublent cette règle avec `@PreAuthorize("hasRole('ADMIN')")`. Aucun endpoint administratif de dataset n'existe ; le `PUT /api/v1/datasets/{id}` incomplet qui retourne `null` ne doit pas devenir le contrat final.

Le frontend ne possède actuellement ni page d'administration des datasets, ni chargement de `/auth/me`, ni guard de rôle. `AuthService` importe aussi directement `environment.development` au lieu du fichier neutre `environment`. Le prototype sur `codex/data-admin-ui-prototype` est uniquement une maquette locale et ne respecte pas le mode unique demandé ; il ne sera pas fusionné aveuglément.

## Proposed approach

### 1. API d'administration minimale

Créer `GET /api/v1/admin/dataset-exposure` et `PUT /api/v1/admin/dataset-exposure`. Les deux endpoints seront sous la règle URL `ADMIN` et annotés avec `@PreAuthorize("hasRole('ADMIN')")` pour rendre la contrainte explicite au niveau du cas d'usage.

Le GET retournera toutes les tables et tous leurs champs, y compris les éléments `active=false`, avec les identifiants, `displayName`, états `active`, les deux booléens de table, `visible` pour les champs et le nombre de champs actifs visibles. La page affichera exclusivement `displayName` pour les tables et les champs ; `sourceName` et les autres noms techniques resteront internes et ne feront pas partie de l'affichage administratif. Les éléments inactifs seront consultables mais non modifiables dans l'UI.

Le PUT recevra uniquement les tables modifiées. Chaque entrée de table portera son identifiant, les deux booléens complets et la liste éventuellement vide des champs modifiés avec `fieldId` et `visible`. Des `Boolean` annotés `@NotNull` seront utilisés aux frontières pour distinguer une valeur `false` d'une propriété absente. Le service devra, avant toute mutation :

1. rejeter les doublons de table ou de champ dans le payload ;
2. charger toutes les tables et tous les champs demandés ;
3. rejeter tout identifiant inconnu ou inactif ;
4. vérifier que chaque champ appartient à la table annoncée ;
5. ne muter qu'après validation complète ;
6. appeler `flush()` dans la transaction afin de détecter une erreur de persistance avant de mapper la réponse ;
7. retourner la configuration complète réellement enregistrée.

La concurrence utilisera dans cette première version une politique last-write-wins documentée. Aucun `@Version` ne sera ajouté sans exigence de résolution de conflits concurrents. Le frontend calculera et enverra seulement les différences avec son baseline, ce qui réduit les écrasements non liés.

Un service concret `DataSetAdministrationService` suffit : aucune interface ou couche de mapping générique ne sera créée. Les DTO seront dédiés afin de ne pas sérialiser les entités JPA. `DataSetRepository.findAllWithFields()` fera un `left join fetch` pour éviter le N+1 ; l'ordre final des tables et champs sera stabilisé lors du mapping.

### 2. Une extraction unique des champs, indépendante du mode de table

Modifier l'extraction de `DataSetFieldRepository` afin qu'elle applique exactement la condition suivante, sans aucun prédicat `displayMain` ou `displayRelated` :

```text
dataset.active = true
AND field.active = true
AND field.visible = true
```

Cette requête représente uniquement la disponibilité intrinsèque des champs. Pour un même dataset actif, elle retourne donc les mêmes champs que son mode soit « Non exposée », « Principale uniquement », « Relation uniquement » ou « Principale et relation ».

Conserver uniquement `GET /api/v1/datasets/{dataSetId}/fields`. Cet endpoint appellera l'extraction générique décrite ci-dessus sans vérifier `displayMain` ou `displayRelated`. Le frontend continuera à l'appeler uniquement pour les datasets déjà retenus par le catalogue des tables principales et des relations autorisées.

Ne pas créer `GET /api/v1/datasets/{rootDataSetId}/relations/{relatedDataSetId}/fields`. Le rôle principal ou relation ne fait donc pas partie du contrat de récupération des champs.

Conséquence acceptée : un client qui connaît l'identifiant d'une table active mais non exposée peut obtenir les métadonnées de ses champs actifs et visibles via cet endpoint générique. Cela ne lui permet pas de les utiliser dans un rapport : `ReportDefinitionResolver` reste l'autorité obligatoire et rejette une racine sans `displayMain`, ainsi qu'une cible sans `displayRelated` ou sans relation directe autorisée. Le plan privilégie ici l'indépendance demandée de l'extraction des champs plutôt que la confidentialité des métadonnées de colonnes.

### 3. Corriger le graphe et centraliser l'enforcement

Corriger `DataSetRepository.findVisibleTableRelations()` pour exiger : source active, cible active et cible `displayRelated=true`, sans exiger `displayRelated` sur la source. Le comportement direct sortant, le schéma `public` et les règles d'ambiguïté existantes resteront inchangés.

Faire de `ReportDefinitionResolver` l'autorité métier pour toute définition :

- la racine doit être active et `displayMain=true` ;
- chaque champ demandé pour sélection, filtre ou tri doit être actif et visible ;
- un champ de racine doit appartenir à la racine ;
- un champ lié doit appartenir à une cible active, `displayRelated=true`, accessible par une relation directe sortante autorisée ;
- les identifiants forgés, les champs d'une table non jointe et les relations non autorisées sont rejetés avant construction SQL ;
- les clés techniques nécessaires au `JOIN` et à l'ordre stable restent résolues séparément, sans condition `visible` et sans entrer dans la projection.

Ajouter une exception métier `ReportDefinitionUnavailableException` contenant une liste structurée et non sensible d'éléments indisponibles (`kind`, `id`, `displayName` si encore connu, `reason`). Le resolver agrégera les violations d'exposition au lieu de s'arrêter au premier identifiant, tandis que les erreurs de forme déjà existantes conserveront leur traitement actuel. `ReportExceptionHandler` exposera cette liste dans le `ProblemDetail` des requêtes synchrones.

### 4. Preview, génération et exports existants

La preview et la création d'une génération continueront à passer par le resolver. Le worker de génération gardera sa seconde validation pour fermer la fenêtre entre enqueue et exécution. Une indisponibilité à ce stade produira le code stable `REPORT_DEFINITION_UNAVAILABLE`, distinct de `GENERATION_FAILED`.

Pour les exports :

- `ReportExportService.create()` revalidera `generation.definitionJson` avant de créer ou retourner un export ;
- `ReportExportService.openDownload()` revalidera la même définition avant d'ouvrir un fichier déjà prêt ;
- `ReportJobStateService.ReportExportWork` transportera aussi `definitionJson` ;
- `ReportExportWorker.run()` désérialisera et résoudra la définition juste avant l'écriture, afin de couvrir une désactivation entre la requête et le démarrage du worker ;
- une indisponibilité asynchrone utilisera `REPORT_DEFINITION_UNAVAILABLE`, sans supprimer le snapshot ni la définition.

Les réponses de statut `ReportGenerationResponse` et `ReportExportResponse` recevront un champ optionnel `unavailableElements`, reconstruit depuis `definitionJson` lorsque le code d'erreur indique une définition indisponible. Cela donne une erreur exploitable sans nouvelle colonne ni altération de la définition. Si les métadonnées sont réactivées, le brouillon conservé peut à nouveau lancer une preview ou une nouvelle génération ; un snapshot et un export déjà prêts redeviennent téléchargeables après revalidation. Aucun endpoint implicite de retry ne sera inventé.

### 5. Page Angular administrative

Ajouter une route lazy `/administration/datasets` protégée par un `adminGuard`. `AuthService.me()` chargera `/api/v1/auth/me`, avec un modèle typé `CurrentUser`; le guard vérifiera `ROLE_ADMIN`. Cette protection améliore l'UX mais ne remplace jamais les 403 backend.

La page standalone utilisera :

- Signals pour `baseline`, `draft`, `searchTerm`, `loading`, `saving`, `loadError` et `successMessage` ;
- `computed()` pour la liste filtrée, le nombre de modifications, l'état dirty et les compteurs visibles ;
- RxJS uniquement dans le service HTTP et les abonnements bornés par `takeUntilDestroyed()` ;
- `p-select` pour le mode, `p-checkbox` pour les champs, `p-accordion` pour le dépliage, plus les composants PrimeNG existants de message, skeleton et bouton ;
- une recherche globale basée uniquement sur `displayName`, qui conserve une table si son `displayName` ou celui d'au moins un de ses champs correspond ;
- aucun nom technique de table ou de champ dans les accordéons, les résultats de recherche, les compteurs ou les messages métier ;
- une seule action primaire « Enregistrer », désactivée sans modification ou pendant la sauvegarde ;
- des états loading, empty, error et success explicites ;
- un indicateur textuel du nombre de modifications non enregistrées ;
- des labels associés, une hiérarchie de titres correcte, focus visible, navigation clavier et contraste WCAG AA.

Passer une table à `NONE` désactivera visuellement ses checkboxes sans changer les valeurs `visible` du draft. Les éléments techniques `active=false` seront marqués « Indisponible » et entièrement read-only. Après succès, la réponse serveur deviendra le nouveau baseline ; après erreur, le draft restera intact.

Les fonctions pures `toExposureMode(displayMain, displayRelated)` et `fromExposureMode(mode)` seront exhaustives et testées. Le mode UI ne sera jamais envoyé comme source de vérité au backend.

## Affected files and symbols

Les nouveaux fichiers sont marqués « nouveau ». La liste pourra être resserrée si un symbole existant couvre exactement le besoin, mais aucune couche supplémentaire ne devra être créée sans mettre à jour le Decision Log.

### Backend

- `src/main/java/RHIS/com/RHIS/dataset/entity/DataSetEntity.java`
  - constructeur et propriétés `displayMain`, `displayRelated`: documenter/tester les valeurs par défaut sans changer la source de vérité.
- `src/main/java/RHIS/com/RHIS/dataset/entity/DataSetField.java`
  - propriété `visible`: réutiliser telle quelle ; aucune colonne supplémentaire.
- `src/main/java/RHIS/com/RHIS/dataset/bootstrap/DataSetInitializer.java`
  - `synchronizeDataSets`, `synchronizeFields`: préserver les préférences et rendre les defaults des nouveaux éléments explicites.
- `src/main/java/RHIS/com/RHIS/dataset/repository/DataSetRepository.java`
  - `findByActiveTrueAndDisplayMainTrue`, `findVisibleTableRelations`: corriger la sémantique source/cible et la requête invalide.
  - `findAllWithFields` (nouveau): charger la configuration admin sans N+1.
- `src/main/java/RHIS/com/RHIS/dataset/repository/DataSetFieldRepository.java`
  - modifier `findVisibleFieldsByDatasetId` pour appliquer exclusivement `dataset.active=true AND field.active=true AND field.visible=true`, sans dépendance à `displayMain` ou `displayRelated` ; les règles d'utilisation restent dans le catalogue et le resolver.
- `src/main/java/RHIS/com/RHIS/dataset/controller/DataSetController.java`
  - conserver l'unique endpoint `getDataSetsFieldsByDataSetId` ; supprimer le placeholder `activeOrInactiveDataSet` sans contrat utilisable.
- `src/main/java/RHIS/com/RHIS/dataset/controller/dto/DataSetExposureConfigurationResponse.java` (nouveau)
  - records de réponse table/champ, sans entités JPA.
- `src/main/java/RHIS/com/RHIS/dataset/controller/dto/UpdateDataSetExposureRequest.java` (nouveau)
  - records validés table/champ et booléens non nuls.
- `src/main/java/RHIS/com/RHIS/dataset/controller/DataSetAdministrationController.java` (nouveau)
  - GET/PUT `/api/v1/admin/dataset-exposure`, `@PreAuthorize`.
- `src/main/java/RHIS/com/RHIS/dataset/service/DataSetAdministrationService.java` (nouveau)
  - lecture complète, validation sans mutation partielle, mise à jour transactionnelle et mapping.
- `src/main/java/RHIS/com/RHIS/dataset/exception/DataSetConfigurationException.java` (nouveau)
  - erreur métier de configuration invalide.
- `src/main/java/RHIS/com/RHIS/dataset/controller/DataSetAdministrationExceptionHandler.java` (nouveau)
  - réponse `ProblemDetail` cohérente et bornée aux contrôleurs admin dataset.
- `src/main/java/RHIS/com/RHIS/report/service/ReportDefinitionResolver.java`
  - validation contextuelle racine/cibles/champs et agrégation des éléments indisponibles.
- `src/main/java/RHIS/com/RHIS/report/exception/ReportDefinitionUnavailableException.java` (nouveau)
  - liste structurée des éléments devenus indisponibles.
- `src/main/java/RHIS/com/RHIS/report/controller/ReportExceptionHandler.java`
  - sérialiser l'erreur métier dans `ProblemDetail`.
- `src/main/java/RHIS/com/RHIS/report/service/ReportGenerationWorker.java`
  - distinguer `REPORT_DEFINITION_UNAVAILABLE` des erreurs techniques.
- `src/main/java/RHIS/com/RHIS/report/service/ReportGenerationService.java`
  - reconstruire le détail d'indisponibilité dans les réponses de statut.
- `src/main/java/RHIS/com/RHIS/report/service/ReportExportService.java`
  - revalider avant création et téléchargement ; enrichir le statut.
- `src/main/java/RHIS/com/RHIS/report/service/ReportJobStateService.java`
  - ajouter `definitionJson` à `ReportExportWork`.
- `src/main/java/RHIS/com/RHIS/report/service/ReportExportWorker.java`
  - revalidation juste avant écriture et code d'erreur métier.
- `src/main/java/RHIS/com/RHIS/report/controller/dto/ReportGenerationResponse.java`
  - champ optionnel `unavailableElements`.
- `src/main/java/RHIS/com/RHIS/report/controller/dto/ReportExportResponse.java`
  - champ optionnel `unavailableElements`.
- `src/test/java/RHIS/com/RHIS/dataset/bootstrap/DataSetInitializerTest.java` (nouveau)
  - préservation et valeurs par défaut.
- `src/test/java/RHIS/com/RHIS/dataset/service/DataSetAdministrationServiceTest.java` (nouveau)
  - validation atomique, appartenance, inactifs et retour réel.
- `src/test/java/RHIS/com/RHIS/dataset/controller/DataSetAdministrationControllerSecurityTest.java` (nouveau)
  - anonyme/non-admin/admin et validation HTTP.
- `src/test/java/RHIS/com/RHIS/dataset/repository/DataSetFieldRepositoryTest.java` (nouveau)
  - extraction indépendante des quatre modes de table, avec contrôles `active` et `visible`.
- `src/test/java/RHIS/com/RHIS/report/service/ReportPreviewServiceTest.java`
  - requêtes forgées et quatre combinaisons.
- `src/test/java/RHIS/com/RHIS/report/service/ReportDefinitionResolverTest.java` (nouveau si les scénarios ne restent pas lisibles dans le test de preview)
  - sélection/filtre/tri, relation-only et clés techniques invisibles.
- `src/test/java/RHIS/com/RHIS/report/service/ReportGenerationWorkerTest.java`
  - désactivation après enqueue et code métier.
- `src/test/java/RHIS/com/RHIS/report/service/ReportExportServiceTest.java`
  - blocage création/téléchargement puis réactivation.
- `src/test/java/RHIS/com/RHIS/report/service/ReportExportWorkerTest.java`
  - désactivation dans la fenêtre asynchrone et conservation du snapshot.
- `src/test/java/RHIS/com/RHIS/report/service/ReportJobStateServiceTest.java`
  - transport de `definitionJson`.
- `src/test/java/RHIS/com/RHIS/report/controller/ReportControllerSecurityTest.java`
  - non-régression d'authentification et détail `ProblemDetail`.

### Frontend

- `../Frontend/Rhis_report_gen/src/app/features/auth/auth.model.ts`
  - `CurrentUser` et rôles typés.
- `../Frontend/Rhis_report_gen/src/app/features/auth/services/auth.service.ts`
  - import `environment` neutre et `me()`.
- `../Frontend/Rhis_report_gen/src/app/features/auth/services/auth.service.spec.ts`
  - contrat `/auth/me` et credentials.
- `../Frontend/Rhis_report_gen/src/app/features/auth/guards/admin.guard.ts` (nouveau)
  - autorisation de navigation basée sur `ROLE_ADMIN`.
- `../Frontend/Rhis_report_gen/src/app/features/auth/guards/admin.guard.spec.ts` (nouveau)
  - admin autorisé, non-admin redirigé, erreur refusée.
- `../Frontend/Rhis_report_gen/src/app/app.routes.ts`
  - route lazy `/administration/datasets`.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.model.ts` (nouveau)
  - contrats API, union `DatasetExposureMode` et conversions pures.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.service.ts` (nouveau)
  - GET/PUT groupés avec credentials.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.service.spec.ts` (nouveau)
  - URLs, payload delta et credentials.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.ts` (nouveau)
  - Signals, computed, recherche, draft/baseline et sauvegarde.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.html` (nouveau)
  - accordéon accessible affichant uniquement les `displayName` des tables et champs, select de mode, checkboxes, états et action primaire.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.scss` (nouveau)
  - échelle 8 px, responsive, focus/contraste et états sans nouvelle couleur arbitraire.
- `../Frontend/Rhis_report_gen/src/app/features/administration/dataset-exposure/dataset-exposure.component.spec.ts` (nouveau)
  - quatre modes, conservation des champs, recherche, états et payload.
- `../Frontend/Rhis_report_gen/src/app/features/rapports/pages/source_de_donnes/rapports.component.spec.ts`
  - source main-only avec relations sortantes et cible relation-only.
- `../Frontend/Rhis_report_gen/src/app/features/rapports/models/report-generation.model.ts`
  - détail optionnel des éléments indisponibles.
- `../Frontend/Rhis_report_gen/src/app/features/rapports/pages/export/export.component.ts`
  - message métier lisible pour génération/export indisponible.
- `../Frontend/Rhis_report_gen/src/app/features/rapports/pages/export/export.component.spec.ts`
  - rendu du message et non-régression des autres erreurs.

### Documentation

- `docs/flows/01-dataset-selection-and-configuration-load.md`
  - distinguer source principale et cible liée.
- `docs/flows/03-report-preview.md`
  - contrôles d'exposition centralisés.
- `docs/flows/04-report-generation.md`
  - seconde validation et erreur métier.
- `docs/flows/05-report-export-and-download.md`
  - revalidation création/worker/téléchargement.
- `docs/flows/07-dataset-exposure-administration.md` (nouveau)
  - lecture, édition, validation transactionnelle et sécurité.
- `docs/flows/README.md`
  - indexer le nouveau flux.
- `docs/plans/2026-08-24-dataset-field-exposure.md`
  - mise à jour continue de l'exécution et des preuves.

## Milestone 1: Stabiliser les invariants de métadonnées et du catalogue

Result:

Les defaults et la resynchronisation sont prouvés par tests. Le catalogue différencie correctement les racines des cibles directes, sans modifier la profondeur des relations.

Work:

- ajouter les tests de matrice pour `displayMain`/`displayRelated`, `visible` et `active` ;
- expliciter les defaults des nouveaux datasets/champs sans réinitialiser les lignes existantes ;
- corriger `findVisibleTableRelations()` et son fragment SQL invalide ;
- modifier et tester l'extraction générique des champs avec l'unique condition `dataset.active && field.active && field.visible`, pour les quatre combinaisons main/relation ;
- conserver un seul endpoint de champs par `datasetId` et le chargement Angular actuel pour les tables principales et liées ;
- garder les contrôles de rôle main/relation dans le catalogue et le resolver, hors de l'extraction des champs.

Validation:

- Command backend: `mvn "-Dtest=DataSetInitializerTest,DataSetFieldRepositoryTest,ReportPreviewServiceTest" test`
- Command frontend: `npm.cmd test -- --watch=false --include="src/app/features/rapports/services/dataset.service.spec.ts" --include="src/app/features/rapports/pages/source_de_donnes/rapports.component.spec.ts" --include="src/app/features/rapports/pages/configuration/report-configuration-loader.service.spec.ts"`
- Expected observation: l'endpoint de champs retourne les mêmes champs actifs et visibles pour les quatre modes d'une table active ; une source `true/false` conserve ses relations sortantes ; une cible `false/true` est utilisable uniquement via une relation directe lors de la résolution d'un rapport ; `false/false` reste rejeté par le resolver.

## Milestone 2: Livrer l'API administrative atomique et sécurisée

Result:

Un admin peut lire toute la configuration et enregistrer un lot valide ; une requête invalide ne produit aucune mise à jour partielle et un non-admin reçoit 403.

Work:

- créer les DTO, le contrôleur, le service concret et le handler d'erreurs ;
- charger tables + champs sans N+1 ;
- valider doublons, existence, activité et appartenance avant mutation ;
- sauvegarder et flush dans une transaction ;
- retourner la configuration complète réellement persistée ;
- couvrir les deux niveaux d'autorisation URL et méthode.

Validation:

- Command: `mvn "-Dtest=DataSetAdministrationServiceTest,DataSetAdministrationControllerSecurityTest" test`
- Expected observation: GET admin contient aussi les éléments inactifs, PUT valide retourne les valeurs persistées, PUT partiellement invalide ne modifie rien, anonymous reçoit 401/403 selon le mécanisme actuel et un utilisateur authentifié non-admin reçoit 403.

## Milestone 3: Fermer les contournements preview, génération et export

Result:

Le resolver applique l'exposition à toute donnée contrôlée par l'utilisateur, les clés techniques restent internes et toutes les fenêtres asynchrones sont revalidées.

Work:

- corriger la validation contextuelle de `ReportDefinitionResolver` ;
- agréger et exposer les éléments indisponibles ;
- prouver sélection, filtre, tri et jointure forgés ;
- revalider dans le worker de génération avec un code métier stable ;
- revalider export create, worker et download ;
- transporter `definitionJson` vers le worker d'export ;
- enrichir les réponses de statut sans ajouter de colonne ;
- préserver définition, snapshot et fichier lors d'une indisponibilité métier.

Validation:

- Command: `mvn "-Dtest=ReportDefinitionResolverTest,ReportPreviewServiceTest,ReportGenerationWorkerTest,ReportExportServiceTest,ReportExportWorkerTest,ReportJobStateServiceTest,ReportControllerSecurityTest,ReportSqlBuilderTest" test`
- Expected observation: tout ID masqué forgé échoue avant SQL avec les éléments concernés ; une clé invisible peut servir dans `JOIN ... ON` sans être sélectionnée ; une désactivation après enqueue produit `REPORT_DEFINITION_UNAVAILABLE`; une réactivation permet une nouvelle exécution ou le téléchargement autorisé sans perte de définition/artifact.

## Milestone 4: Livrer la page Angular d'administration

Result:

La route admin affiche et édite les quatre modes et les champs avec sauvegarde explicite, états complets et interactions accessibles.

Work:

- typer `/auth/me`, corriger l'import d'environnement et ajouter le guard ;
- créer modèles, conversions, service HTTP et page standalone ;
- implémenter la recherche sur les `displayName` des tables/champs, les accordéons, select, checkboxes et compteurs ;
- préserver les valeurs de champs lorsque le mode devient `NONE` ;
- envoyer uniquement le delta dans un PUT unique ;
- intégrer loading, empty, error, success et dirty ;
- vérifier clavier, focus, labels, contraste, responsive et échelle d'espacement.

Validation:

- Command: `npm.cmd test -- --watch=false --include="src/app/features/auth/services/auth.service.spec.ts" --include="src/app/features/auth/guards/admin.guard.spec.ts" --include="src/app/features/administration/dataset-exposure/dataset-exposure.service.spec.ts" --include="src/app/features/administration/dataset-exposure/dataset-exposure.component.spec.ts"`
- Command build: `npm.cmd run build`
- Expected observation: les quatre conversions sont exhaustives, aucun HTTP n'est émis avant « Enregistrer », le delta est exact, les valeurs de champs survivent à `NONE`, les états sont rendus et le build réussit sans nouvelle erreur.

## Milestone 5: Intégration, documentation et régression

Result:

Le flux complet est vérifié depuis l'administration jusqu'à la preview/génération/export, les documents reflètent le comportement livré et les écarts de baseline sont distingués des régressions.

Work:

- mettre à jour les cinq documents de flux ;
- exécuter les suites focalisées et les suites complètes ;
- faire une vérification manuelle admin/non-admin et du parcours de rapport ;
- effectuer la revue correctness, security, data integrity, maintainability, performance puis style ;
- mettre à jour toutes les sections vivantes de cet ExecPlan.

Validation:

- Command backend: `mvn test`
- Command frontend: `npm.cmd test -- --watch=false`
- Command frontend build: `npm.cmd run build`
- Expected observation: aucun échec nouveau par rapport aux baselines documentées ; tous les scénarios d'acceptation ci-dessous sont observables. Les échecs préexistants éventuels sont consignés avec le test exact et ne sont pas masqués.

## Validation and acceptance

Automated:

- [ ] les quatre combinaisons des booléens ont un test backend et un test de conversion frontend ;
- [ ] le repository extrait les mêmes champs actifs et visibles pour les quatre modes d'une table active et n'extrait rien si la table ou le champ est inactif, ou si le champ est invisible ;
- [ ] les tests repositories/services prouvent la différence source/cible ;
- [ ] les tests admin prouvent validation atomique et HTTP 403 ;
- [ ] les tests resolver couvrent sélection, filtre, tri et clés techniques invisibles ;
- [ ] les tests workers couvrent une désactivation entre enqueue et exécution ;
- [ ] les tests export couvrent create, worker et download ;
- [ ] les tests de synchronisation prouvent préservation et defaults ;
- [ ] les suites complètes et le build Angular sont exécutés.

Manual:

- [ ] connecté admin, ouvrir `/administration/datasets`, rechercher par `displayName` de table puis par `displayName` de champ et vérifier le filtrage des accordéons ;
- [ ] vérifier qu'aucun `sourceName` ou autre nom technique de table/champ n'est affiché dans la page ou ses messages métier ;
- [ ] changer plusieurs modes et champs, vérifier l'indicateur dirty, l'absence d'appel par clic, puis une seule sauvegarde et le message de succès ;
- [ ] mettre une table à « Non exposée », vérifier que ses champs deviennent non modifiables mais conservent leur valeur après réactivation ;
- [ ] connecté non-admin, vérifier la redirection frontend et HTTP 403 sur GET et PUT admin directs ;
- [ ] configurer une source principale-only reliée à une cible relation-only : la source apparaît au départ, la cible uniquement comme relation ;
- [ ] masquer un champ puis vérifier son absence de l'UI et le rejet d'une preview forgée qui le sélectionne, filtre ou trie ;
- [ ] masquer une clé de jointure et vérifier qu'une preview valide continue à joindre sans retourner cette clé ;
- [ ] désactiver un élément utilisé par un brouillon/job, vérifier l'erreur listée, réactiver puis relancer avec succès ;
- [ ] contrôler l'écran à 320 px, 768 px et desktop, puis au clavier sans souris.

Acceptance criteria:

- [ ] `false/false` n'est utilisable ni comme racine, ni comme cible, même si l'endpoint générique peut retourner les métadonnées de ses champs actifs et visibles ;
- [ ] l'extraction repository d'un champ ne consulte jamais `displayMain` ou `displayRelated`; le blocage de `false/false` est prouvé au niveau service/resolver ;
- [ ] `true/false` est une racine valide, garde ses relations sortantes mais ne peut pas être cible ;
- [ ] `false/true` n'apparaît pas comme racine mais peut être cible d'une relation directe valide ;
- [ ] `true/true` fonctionne dans les deux rôles ;
- [ ] seules les cibles actives avec `displayRelated=true` apparaissent dans le graphe public ;
- [ ] un champ `visible=false` n'est jamais proposé ni accepté pour sélection, filtre, tri ou export ;
- [ ] un champ invisible utilisé comme clé technique n'apparaît pas dans les colonnes retournées ;
- [ ] un payload admin inconnu, dupliqué, inactif ou avec mauvaise appartenance est rejeté entièrement ;
- [ ] la sécurité backend refuse tout non-admin indépendamment du route guard ;
- [ ] la synchronisation ne réinitialise aucune préférence existante ;
- [ ] une définition devenue indisponible est conservée et l'erreur nomme les éléments concernés ;
- [ ] la réactivation permet une nouvelle exécution et, lorsque l'artifact existe encore, le téléchargement ;
- [ ] les relations restent directes sortantes et aucun comportement hors périmètre ne change.

## Risks and rollback

| Risk | Prevention/Detection | Rollback |
|---|---|---|
| Les changements non commités actuels autour de `displayMain`/`displayRelated` sont écrasés. | Repartir du working tree, inspecter `git diff` avant chaque modification et limiter les patches aux symboles ciblés. | Revenir uniquement sur les hunks de cette fonctionnalité à partir du diff consigné ; ne jamais reset le dépôt. |
| Une source main-only perd ses relations sortantes. | Test explicite source `true/false` -> cible `*/true`; ne filtrer `displayRelated` que sur la cible. | Restaurer la requête précédente puis désactiver temporairement l'affichage des relations, sans toucher aux préférences. |
| L'endpoint générique révèle les noms/types des champs d'une table active non exposée. | Décision métier explicitement acceptée ; aucune donnée métier n'est retournée et le resolver bloque toute utilisation non autorisée. Test forgé obligatoire sur preview/génération/export. | Si ces métadonnées deviennent sensibles, réintroduire un contrat contextuel dans un plan approuvé avant déploiement. |
| Mise à jour administrative partielle. | Validation complète avant mutation, transaction et `flush`, tests d'échec en milieu de payload. | Rollback transactionnel automatique ; aucune opération compensatoire attendue. |
| Deux admins s'écrasent mutuellement. | Payload delta, réponse serveur comme nouveau baseline, politique last-write-wins explicitée. | Recharger la configuration ; ajouter ultérieurement `@Version` seulement si le besoin de concurrence est confirmé. |
| N+1 ou volume élevé sur l'écran admin. | `left join fetch`, DTO compact, tri en mémoire mesuré ; recherche frontend. | Revenir à une pagination serveur dans un plan séparé si le volume réel le justifie. |
| Désactivation entre validation et worker/export. | Validation synchronisée aux frontières et revalidation dans chaque worker. | Bloquer temporairement la création/téléchargement d'exports plutôt que contourner le resolver. |
| Les détails d'erreur recalculés changent après réactivation. | Conserver le code stable et présenter les détails seulement tant qu'ils sont vérifiables contre les métadonnées courantes. | Retourner le code générique ; une persistance historique détaillée nécessiterait une migration séparée. |
| `ddl-auto=create` fait perdre la configuration au redémarrage. | Critère de readiness de déploiement : profil persistant avec stratégie de schéma explicite. | Ne pas déployer la fonctionnalité dans ce profil ; exporter/réappliquer manuellement la configuration n'est pas considéré comme solution durable. |
| Rupture de contrat des statuts de jobs par ajout de détails. | Champ additionnel optionnel, tests de sérialisation et adaptation frontend simultanée. | Retirer le champ optionnel tout en conservant le `ProblemDetail` synchrone et le code stable. |
| Nouvelle régression masquée par les baselines déjà rouges. | Comparer test par test avec les résultats du 23 août et documenter toute différence. | Revenir au dernier milestone vert, sans corriger opportunément les tests hors périmètre. |

## Progress

- [x] 2026-08-24 — Research Graphify puis vérification source terminées dans `docs/research/2026-08-24-dataset-field-exposure.md`.
- [x] 2026-08-24 — `DESIGN.md`, `graphify-out/graph.json`, `.agent/PLANS.md`, les templates et la guideline frontend relus.
- [x] 2026-08-24 — Graphe régénéré et réinspecté : ancien symbole absent, `findByActiveTrueAndDisplayMainTrue()` et ses dépendances actuelles présents ; research et plan réalignés.
- [x] 2026-08-24 — ExecPlan rédigé ; aucun code applicatif, contrat API ou migration modifié.
- [x] 2026-08-24 — Plan approuvé par l'utilisateur et exécution autorisée.
- [x] 2026-08-24 — Milestone 1 implémenté ; extraction générique, relations et resolver vérifiés par tests ciblés.
- [x] 2026-08-24 — Milestone 2 implémenté ; API admin transactionnelle et sécurité vérifiées par 7 tests ciblés.
- [x] 2026-08-24 — Milestone 3 implémenté ; preview/génération/export revalidés et erreurs structurées exposées.
- [x] 2026-08-24 — Milestone 4 implémenté ; route admin, quatre modes, delta explicite et états UI vérifiés.
- [x] 2026-08-24 — Milestone 5 automatisé et revue finale terminés ; vérification manuelle avec backend/PostgreSQL réel encore à effectuer.

Exact next action: démarrer backend et frontend avec PostgreSQL persistant, puis exécuter la checklist manuelle d'acceptation.

## Surprises & Discoveries

- 2026-08-24 — La première inspection utilisait un graphe du 21 août qui référençait encore `findByActiveTrueAndVisibleTrue()`. Après `graphify update .` depuis la racine commune, le graphe du 24 août contient `findByActiveTrueAndDisplayMainTrue()` et son appel depuis `DataSetServiceImpl.getDataSets()` ; les documents ont été corrigés sans réécrire cette chronologie.
- 2026-08-24 — Le graphe actualisé confirme les dépendances de `findVisibleTableRelations()` vers le service, le resolver et les tests, mais n'expose pas `displayRelated` comme nœud autonome. Le corps de la requête native reste donc la preuve nécessaire pour sa sémantique source/cible et sa faute de colonne.
- 2026-08-24 — les documents projet obligatoires existent au niveau parent commun `../`, et non dans chacun des deux dépôts.
- 2026-08-24 — `DataSetField.visible` existe déjà et la synchronisation préserve déjà sa valeur ; aucune migration d'exposition de champ n'est requise.
- 2026-08-24 — `findVisibleTableRelations()` filtre incorrectement la source par `displayRelated` et contient un fragment de nom de colonne invalide ; les tests focalisés actuels ne l'exercent pas contre PostgreSQL.
- 2026-08-24 — le même endpoint de champs est actuellement utilisé pour la racine et les tables liées, ce qui rend impossible la sémantique relation-only sans contexte supplémentaire.
- 2026-08-24 — il n'existe pas de rapport sauvegardé autonome ; seuls un brouillon `sessionStorage`, `definitionJson`, les snapshots et les exports matérialisent la continuité d'une définition.
- 2026-08-24 — les exports ne revalident actuellement aucune métadonnée et le worker ne reçoit pas la définition, contrairement au worker de génération.
- 2026-08-24 — le profil actuel `ddl-auto=create` annule la persistance inter-redémarrages, indépendamment de la justesse du synchroniseur.
- 2026-08-24 — les tests focalisés exécutés pendant la recherche sont verts : backend 25/25 et frontend 20/20. Les suites complètes précédentes ont des échecs préexistants documentés dans la recherche.
- 2026-08-24 — après implémentation, les tests backend ciblés sont verts (25/25 sur le lot final). La suite complète compte 63 succès, 4 échecs XLSX préexistants liés à une fenêtre SXSSF configurée à zéro et 4 tests Testcontainers ignorés faute de Docker.
- 2026-08-24 — la suite Angular complète compte 108 succès et un échec préexistant : `ExportComponent keeps the Option B action disabled until the selected file is ready` attend un `p-tag` absent du template de `HEAD`. Le build production réussit avec l'avertissement de budget initial déjà présent (+67,01 kB).

## Decision Log

- 2026-08-24 — **Decision:** conserver deux booléens backend et dériver un mode uniquement dans l'UI.
  - Reason: ce sont les propriétés existantes et la source de vérité imposée ; un enum persistant créerait une duplication et des états divergents.
  - Alternatives rejected: enum Java, colonne `exposure_mode`, entité de configuration.
- 2026-08-24 — **Decision:** valeur par défaut des nouvelles tables = principale uniquement ; nouveaux champs = visibles.
  - Reason: préserver l'accès historique aux nouvelles tables sans élargir silencieusement le graphe des relations.
  - Alternatives rejected: tout exposer, qui augmente l'accès implicite ; tout masquer, qui rompt le comportement existant.
- 2026-08-24 — **Decision:** `displayRelated` est une permission de cible, pas une permission de source.
  - Reason: c'est la sémantique métier explicitée et elle permet à une racine main-only d'avoir des relations sortantes.
  - Alternatives rejected: filtre symétrique source/cible, actuellement incorrect.
- 2026-08-24 — **Decision superseded:** créer un endpoint de champs lié contextualisé par `rootId` et `relatedId`.
  - Initial reason: prouver la relation directe avant de retourner les métadonnées.
  - Superseded by: la décision suivante impose une extraction unique indépendante du rôle de table.
- 2026-08-24 — **Decision:** rendre l'extraction repository des champs indépendante des modes main/relation.
  - Reason: la disponibilité intrinsèque d'un champ est définie uniquement par `dataset.active=true`, `field.active=true` et `field.visible=true`; le rôle de la table est une règle contextuelle du catalogue et du resolver.
  - Alternatives rejected: conserver `dataset.displayMain` ou ajouter `dataset.displayRelated` dans la requête, ce qui recouplerait l'extraction au rôle de la table.
- 2026-08-24 — **Decision:** annuler l'endpoint contextuel `/{rootDataSetId}/relations/{relatedDataSetId}/fields` et conserver uniquement `/{dataSetId}/fields`.
  - Reason: le choix utilisateur est de rendre tout le flux d'extraction des champs indépendant du rôle main/relation, pas seulement le repository.
  - Trade-off accepted: les métadonnées des champs actifs/visibles d'une table active non exposée peuvent être lues directement ; leur utilisation reste bloquée centralement par le resolver.
  - Alternatives rejected: endpoint contextuel racine/cible ; paramètre de rôle sur l'endpoint générique.
- 2026-08-24 — **Decision:** afficher uniquement `displayName` pour les tables et les champs dans l'interface administrative.
  - Reason: l'administration doit utiliser les libellés destinés aux utilisateurs plutôt que les identifiants techniques de la source de données.
  - Alternatives rejected: afficher `sourceName` seul ; afficher simultanément `displayName` et le nom technique.
- 2026-08-24 — **Decision:** afficher les éléments inactifs dans l'administration mais les rendre read-only.
  - Reason: l'administrateur doit comprendre l'état conservé sans pouvoir configurer un objet technique absent.
  - Alternatives rejected: les masquer, qui rend les préférences fantômes incompréhensibles ; les rendre éditables, sans effet métier vérifiable.
- 2026-08-24 — **Decision:** sauvegarde explicite par delta, transactionnelle et last-write-wins.
  - Reason: satisfait l'atomicité et évite un HTTP par clic sans introduire une gestion de version non demandée.
  - Alternatives rejected: PATCH par contrôle ; ajout immédiat de `@Version`.
- 2026-08-24 — **Decision:** revalider un export à la création, dans le worker et au téléchargement.
  - Reason: chaque frontière couvre une fenêtre de concurrence différente et empêche qu'un snapshot contourne une désactivation.
  - Alternatives rejected: validation uniquement à la génération, insuffisante après changement administratif ; suppression des artifacts, contraire à la réactivation.
- 2026-08-24 — **Decision:** conserver un code d'erreur asynchrone stable et reconstruire les détails depuis `definitionJson`.
  - Reason: nommer les éléments indisponibles sans nouvelle colonne ni destruction de la définition.
  - Alternatives rejected: concaténer des IDs dans `errorCode`, fragile ; ajouter une colonne de détail avant preuve d'un besoin historique immuable.
- 2026-08-24 — **Decision:** utiliser un accordéon par table sur la page admin.
  - Reason: la demande actuelle impose une section dépliable par table ; cela suit le progressive disclosure de `DESIGN.md`.
  - Alternatives rejected: master-detail du prototype, moins fidèle à la demande actuelle.

## Outcomes & Retrospective

- Delivered behavior: extraction des champs indépendante du mode de table ; relations source/cible corrigées ; API admin `ROLE_ADMIN` atomique ; resolver et workers revalidés ; statuts détaillés ; page Angular admin avec `displayName` uniquement et sauvegarde explicite.
- Commands run and results: tests backend ciblés 25/25 ; suite backend 63 succès, 4 échecs de baseline, 4 skips Docker ; suite Angular 108/109 avec l'échec de baseline identifié ; build Angular réussi avec warning budget ; `git diff --check` sans erreur.
- Deviations from the approved plan: aucun endpoint contextuel ajouté, aucune migration, aucun worktree, aucun commit. Les contrôles manuels n'ont pas été exécutés faute d'application et PostgreSQL démarrés dans cette session.
- Remaining risks or unverified checks: requête native PostgreSQL non exécutée faute de Docker ; persistance neutralisée tant que `ddl-auto=create` est utilisé ; endpoint admin volontairement non paginé ; checklist responsive/clavier et parcours admin réel à valider manuellement.
- Required follow-up: exécuter les scénarios manuels avec un profil PostgreSQL persistant puis corriger séparément les baselines XLSX et `p-tag` si souhaité.
- Exact next action if incomplete: démarrer la stack et suivre les critères manuels lignes 428–437.
