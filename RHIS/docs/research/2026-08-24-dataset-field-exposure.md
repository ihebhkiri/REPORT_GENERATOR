# Research: exposition administrative des datasets et de leurs champs

Date: 2026-08-24
Status: Ready for planning
Related issue: N/A

## Question

Comment permettre à un administrateur de configurer les usages `principal` et `relation` d'une table ainsi que la visibilité de ses champs, tout en réutilisant le modèle existant et en empêchant tout contournement par les APIs de preview, génération ou export ?

## Scope

Included:

- métadonnées `DataSetEntity` et `DataSetField` ;
- synchronisation depuis `information_schema` ;
- catalogue public des tables, relations et champs ;
- résolution des sélections, filtres, tris et jointures directes ;
- preview, génération asynchrone, snapshot, export et téléchargement ;
- sécurité Spring et future route Angular d'administration ;
- prototype administratif existant sur la branche frontend `codex/data-admin-ui-prototype` ;
- tests et commandes de validation disponibles.

Excluded:

- modification du code applicatif, des contrats API ou du schéma pendant cette phase ;
- ajout d'agrégations, groupements ou relations multi-niveaux ;
- refonte du shell global, des rapports ou des workers ;
- nouvelle source de vérité pour l'exposition ;
- commit, push, merge ou migration.

## Sources et méthode

- `C:/Users/Surface Pro/Downloads/RHIS/graphify-out/graph.json` — Graphify a ciblé les communautés `DataSetEntity`, `DataSetField`, `DataSetRepository`, `DataSetInitializer`, `ReportDefinitionResolver`, `ReportSqlBuilder`, `RapportsComponent` et `ReportConfigurationLoader`.
- Le graphe a été régénéré le 24 août 2026 depuis la racine commune backend/frontend. Il contient désormais 2 065 nœuds et 4 391 liens. Il ne référence plus `findByActiveTrueAndVisibleTrue()` et contient `findByActiveTrueAndDisplayMainTrue()` dans `DataSetRepository`, avec l'appel de `DataSetServiceImpl.getDataSets()`.
- La réinspection Graphify relie également `findVisibleTableRelations()` à `DataSetServiceImpl.getRelations()`, `ReportDefinitionResolver.resolveJoins()` et aux tests de relations directes, entrantes, multi-niveaux et ambiguës. Elle relie `ReportDefinitionResolver` à la preview, à la génération, aux repositories de métadonnées et aux méthodes de résolution des filtres, tris et jointures. Les prédicats SQL et les valeurs de champs restent vérifiés dans les sources, car le graphe décrit les symboles et dépendances mais ne remplace pas la lecture du corps des requêtes.
- `../DESIGN.md`, `../.agent/PLANS.md`, `../docs/research/TEMPLATE.md`, `../docs/plans/TEMPLATE.md` et `../docs/guidelines/frontend-ui.md` existent au niveau parent commun des dépôts, pas dans le dépôt backend.
- `docs/superpowers/specs/2026-08-21-database-tables-fields-admin-ui-design.md` et la branche frontend `codex/data-admin-ui-prototype` fournissent une exploration UI antérieure. Elles restent des références, mais la présente demande remplace leur sémantique ambiguë de `displayRelated` et exige un mode PrimeNG unique dérivé des deux booléens.

## Verified current behavior

### Modèle des tables

`src/main/java/RHIS/com/RHIS/dataset/entity/DataSetEntity.java: DataSetEntity`

- `active` indique la présence technique de la table dans la synchronisation.
- `displayMain` contrôle l'apparition comme source principale.
- `displayRelated` est destiné à contrôler l'utilisation comme cible secondaire.
- Le constructeur d'une nouvelle table fixe `active=true` et `displayMain=true`.
- `displayRelated` n'est pas assigné par le constructeur ; la valeur Java par défaut est donc `false`.
- Les quatre modes UI sont strictement dérivés ainsi :

| `displayMain` | `displayRelated` | Mode UI |
|---|---|---|
| `false` | `false` | Non exposée |
| `true` | `false` | Principale uniquement |
| `false` | `true` | Relation uniquement |
| `true` | `true` | Principale et relation |

Point de discipline Git : `displayMain` et `displayRelated` sont présents dans le working tree mais pas dans le commit `HEAD` courant. Le diff utilisateur remplace l'ancien booléen `visible`. Cette fonctionnalité doit partir de ce travail en cours sans l'écraser.

### Modèle des champs

`src/main/java/RHIS/com/RHIS/dataset/entity/DataSetField.java: DataSetField`

- `active` représente la présence technique de la colonne.
- `visible` existe déjà, est non nullable et vaut `true` par défaut.
- Le constructeur fixe `active=true` et `visible=true`.
- Aucune nouvelle colonne, entité ou migration de visibilité des champs n'est nécessaire.

### Synchronisation `information_schema`

`src/main/java/RHIS/com/RHIS/dataset/bootstrap/DataSetInitializer.java: synchronizeDataSets, synchronizeFields, synchronizeField`

- Une table existante redécouverte reçoit seulement `active=true` ; ses valeurs `displayMain` et `displayRelated` ne sont pas modifiées.
- Une nouvelle table utilise le constructeur : `displayMain=true`, `displayRelated=false`.
- Une table disparue reçoit `active=false`; ses deux préférences d'exposition sont conservées.
- Un champ existant redécouvert reçoit ses métadonnées techniques et `active=true`; `visible` n'est pas réassigné.
- Un nouveau champ reçoit `visible=true`.
- Un champ disparu reçoit `active=false`; sa valeur `visible` est conservée.

La logique de resynchronisation préserve donc déjà les préférences en mémoire et en base. En revanche, `src/main/resources/application.yaml: spring.jpa.hibernate.ddl-auto=create` recrée le schéma au démarrage. Cette configuration annule toute garantie de persistance entre redémarrages et doit être traitée comme un risque de déploiement distinct, pas masquée par le synchroniseur.

### Catalogue public des tables principales

`src/main/java/RHIS/com/RHIS/dataset/service/DataSetServiceImpl.java: getDataSets`

- Le working tree appelle `DataSetRepository.findByActiveTrueAndDisplayMainTrue()`.
- Une table `displayMain=false` n'apparaît donc pas dans la sélection initiale.
- Le contrôle est appliqué côté backend et ne dépend pas d'Angular.

### Catalogue des relations

`src/main/java/RHIS/com/RHIS/dataset/repository/DataSetRepository.java: findVisibleTableRelations`

- La requête découvre uniquement les clés étrangères directes du schéma PostgreSQL `public`.
- Le sens est `source FK -> cible référencée`; les tests refusent les relations entrantes et multi-niveaux.
- Le working tree exige actuellement `display_related=true` pour la source et la cible.
- Ce contrôle de la source est incorrect : une table `Principale uniquement` doit pouvoir exposer ses relations sortantes vers des cibles autorisées.
- Seule la cible secondaire doit exiger `displayRelated=true`; la source doit seulement être techniquement active, la racine étant contrôlée séparément par `displayMain`.
- Le SQL courant contient en plus `target_dataset.display_r    elated`, une faute de frappe qui rend la requête native invalide à l'exécution PostgreSQL.

### Catalogue public des champs

`src/main/java/RHIS/com/RHIS/dataset/repository/DataSetFieldRepository.java: findVisibleFieldsByDatasetId`

- Le champ doit être `active=true` et `visible=true`.
- Sa table doit être `active=true` et `displayMain=true`.
- Cette requête fonctionne pour une table principale.
- Elle refuse tous les champs d'une table `Relation uniquement`, alors que le frontend utilise actuellement le même endpoint `GET /datasets/{id}/fields` pour la racine et les tables liées.
- L'endpoint ne transporte pas le contexte racine/cible. Un contrat contextualisé est nécessaire pour ne pas rendre tous les champs d'une table `displayRelated=true` accessibles hors d'une relation directe valide.

### Résolution des définitions forgées

`src/main/java/RHIS/com/RHIS/report/service/ReportDefinitionResolver.java: resolve`

- `findVisibleDataSet()` exige correctement une racine active et `displayMain=true`.
- `collectRequestedFieldIds()` réunit les champs sélectionnés, filtrés et triés.
- `validateAccessibleField()` exige déjà `field.active`, `field.visible`, `dataset.active` et un type supporté.
- Il exige aussi `dataset.displayMain` pour tous les champs. Ce prédicat interdit les champs de tables `Relation uniquement`.
- `resolveJoins()` accepte uniquement une cible reliée directement par une clé étrangère sortante depuis la racine et rejette absence, ambiguïté, relation entrante et multi-niveaux.
- `validateReferencedDatasets()` effectue un contrôle défensif final des datasets autorisés.

La bonne frontière d'enforcement existe déjà : toutes les demandes publiques passent par le resolver avant la construction SQL. Il faut séparer la visibilité intrinsèque du champ de son contexte : racine `displayMain`, cible directe `displayRelated`.

### Champs techniques invisibles

`src/main/java/RHIS/com/RHIS/report/service/ReportSqlBuilder.java: appendJoins, appendSelect, appendOrderBy`

- Les colonnes de jointure proviennent des noms fiables issus d'`information_schema`, pas des champs sélectionnés par l'utilisateur.
- Elles sont utilisées seulement dans `JOIN ... ON` et ne sont ajoutées au `SELECT` que si le champ correspondant a été explicitement sélectionné et validé.
- Les clés primaires de la racine sont chargées via `findByDataset_IdAndActiveTrueOrderByPositionAsc()` pour le tri stable, sans exiger `visible=true`.

Le comportement actuel permet donc déjà à un champ invisible de servir de clé technique de jointure ou de tri interne sans le rendre sélectionnable ni le retourner. Le plan doit préserver cette séparation.

### Sélections, filtres, tris, groupements et agrégations

- Sélection, filtre et tri sont tous validés par `ReportDefinitionResolver` avant SQL.
- Un tri est limité aux champs déjà sélectionnés.
- Un champ `visible=false` est déjà rejeté même dans une requête HTTP forgée.
- Aucun contrat de groupement ou d'agrégation n'existe dans `ReportPreviewRequest`, `ReportQueryModel` ou `ReportSqlBuilder`. Il n'y a donc aucun point de production à modifier pour ces fonctions; toute future fonction devra obligatoirement réutiliser le resolver.

### Preview, génération et export

- `ReportPreviewService.preview()` résout la définition avant `buildPreview()`.
- `ReportGenerationService.createNew()` résout la définition avant de persister le job.
- `ReportGenerationWorker.run()` désérialise `definitionJson` puis résout de nouveau la définition avant le `COUNT` et le streaming complet. Une désactivation entre l'enqueue et le worker est donc détectée.
- Le worker transforme actuellement toute erreur en `errorCode=GENERATION_FAILED`; l'indisponibilité d'une définition n'est pas distinguée.
- `ReportExportService.create()` et `openDownload()` ne relisent ni `definitionJson` ni les métadonnées. Les writers consomment seulement le snapshot.
- Un snapshot ou export déjà prêt reste donc exportable/téléchargeable après désactivation d'une table ou d'un champ.

### Rapports existants

- Il n'existe aucune entité persistante de définition de rapport enregistrée.
- Angular conserve un draft versionné dans `sessionStorage` via `ReportDraftStorageService`.
- Le backend conserve une copie JSON de la demande dans `ReportGenerationEntity.definitionJson` jusqu'à expiration du job.
- Une réactivation permet à un draft navigateur conservé de redevenir valide lors d'une nouvelle preview ou génération.
- Un job de génération déjà `FAILED` n'a pas de mécanisme de retry; l'utilisateur doit relancer une génération à partir du draft.

Le critère « rapport enregistré » s'applique donc aujourd'hui au draft local et au JSON de génération, pas à un catalogue durable de rapports. Une future persistance devra appeler le même resolver à chaque preview/exécution/export.

### Backend et sécurité

`src/main/java/RHIS/com/RHIS/auth/SecurityConfig.java: filterChain`

- `/api/v1/admin/**` exige déjà `hasRole("ADMIN")`.
- Les endpoints datasets/reports/générations/exports exigent une authentification.
- `@EnableMethodSecurity` est actif.
- Les controllers d'administration des utilisateurs ajoutent aussi `@PreAuthorize("hasRole('ADMIN')")`, convention à reproduire en défense en profondeur.
- Aucun endpoint d'administration dataset n'existe. `PUT /api/v1/datasets/{id}` est un placeholder qui retourne `null` et ne doit pas servir de contrat final.
- Il n'existe pas de `RestControllerAdvice` générique pour les datasets. `ReportExceptionHandler` ne couvre que les trois controllers report.

### Frontend courant

- `src/app/app.routes.ts` ne contient ni route d'administration ni guard.
- `AuthService` n'expose que `login()` et importe directement `environment.development`, contrairement à la configuration de remplacement Angular.
- Le backend expose déjà `GET /api/v1/auth/me` avec `{email, roles}`; le frontend n'a pas encore le modèle ni l'appel correspondant.
- `DatasetService.getReportSources()` charge tables principales et relations en parallèle.
- `ReportConfigurationLoader` préserve la règle actuelle : relations directes sortantes uniquement.
- Le frontend primaire ne contient pas la feature `data-admin`. La branche `codex/data-admin-ui-prototype` contient un prototype mocké, sans HTTP ni sécurité, avec deux checkboxes de table. Sa structure et ses tests peuvent guider l'implémentation, mais ils ne doivent pas être repris aveuglément : la demande actuelle exige un unique mode PrimeNG dérivé des deux booléens et une section de champs dépliable par table.

### Contraintes UI vérifiées

- `../DESIGN.md` impose CRAP, une hiérarchie claire, une action primaire unique, l'échelle d'espacement 8 px, des états complets, des labels visibles, l'accessibilité clavier et un feedback local.
- `../docs/guidelines/frontend-ui.md` impose Angular standalone, Signals, `computed()`, RxJS pour HTTP, HTML/SCSS/TS séparés, PrimeNG/PrimeIcons et WCAG AA.
- PrimeNG 20 fournit officiellement `AccordionModule`, `SelectModule`, `CheckboxModule`, `MessageModule` et `SkeletonModule`. `p-select` supporte `options`, `optionLabel`, `optionValue`, `disabled` et les formulaires; `p-checkbox` fournit un input natif accessible et un mode binaire; `p-accordion` gère clavier et attributs d'expansion.
- Angular 20 supporte les functional guards avec `canActivate`; un guard améliore l'UX, mais la sécurité reste le matcher Spring `/api/v1/admin/**`.

## Data and control flow

### Lecture administrative proposée

```text
Route Angular admin
  -> guard GET /auth/me
  -> GET /api/v1/admin/dataset-exposure
  -> controller admin
  -> service transaction read-only
  -> DataSetRepository + champs chargés
  -> DTO incluant actifs/inactifs et préférences conservées
  -> Signals baseline/draft
  -> computed modes, compteurs, recherche et dirty state
```

### Sauvegarde administrative proposée

```text
Clic Enregistrer
  -> calcul des tables/champs modifiés
  -> PUT /api/v1/admin/dataset-exposure
  -> validation Bean Validation + doublons
  -> chargement de tous les IDs dans une transaction
  -> validation table existante/active + appartenance de chaque field
  -> aucune mutation avant validation complète
  -> mutation des deux booléens et de field.visible
  -> flush
  -> retour de la configuration réellement persistée
  -> remplacement atomique baseline/draft Angular
```

### Flux report-builder proposé

```text
GET tables principales -> active && displayMain
GET relations -> source active; cible active && displayRelated
GET champs racine -> racine active && displayMain; champ active && visible
GET champs liés -> racine active && displayMain
                     + relation directe sortante
                     + cible active && displayRelated
                     + champ active && visible
Request forgée -> resolver répète exactement ces invariants
```

## Matrice des contrôles actuels

| Point | État actuel | Écart |
|---|---|---|
| Sélection principale | Correct dans le working tree | Aucun pour `displayMain` |
| Relations proposées | Source et cible exigent `displayRelated` | La source ne doit pas l'exiger; typo SQL cible |
| Champs racine | Champ visible + table `displayMain` | Correct pour la racine |
| Champs liés | Même query que la racine | `Relation uniquement` impossible |
| Resolver racine | `active && displayMain` | Correct |
| Resolver champ | `visible && dataset.displayMain` | Contexte cible incorrect |
| Sélection/filtre/tri forgé | Passe par le resolver | Bon point central, prédicat dataset à corriger |
| Jointure technique invisible | Noms `information_schema`, hors `SELECT` | Correct à préserver |
| Preview | Resolver avant SQL | Correct après correction du resolver |
| Génération | Resolver au POST et dans worker | Erreur worker trop générique |
| Création/téléchargement export | Snapshot uniquement | Revalidation absente |
| Administration Spring | Matcher `/admin/**` disponible | Controller/service/DTO absents |
| Administration Angular | Absente du checkout primaire | Route, guard, service et page à créer |
| Synchronisation | Préserve préférences existantes | Persistance inter-redémarrage annulée par `ddl-auto:create` |

## Invariants and constraints

- `displayMain` et `displayRelated` restent l'unique source de vérité table.
- `DataSetField.visible` reste l'unique source de vérité champ.
- `active` reste technique et non administrable.
- Désactiver l'exposition d'une table ne modifie jamais `field.visible`.
- Une cible liée exige `displayRelated`; une racine exige `displayMain`.
- `displayRelated` ne limite pas les relations sortantes d'une racine.
- Seules les relations directes sortantes non ambiguës restent supportées.
- Un champ invisible est refusé comme sélection, filtre ou tri mais peut rester une clé technique non retournée.
- Le backend revalide tous les IDs et l'appartenance field/table avant toute mutation.
- La sauvegarde est atomique; aucune sous-partie valide d'un payload invalide n'est persistée.
- L'API admin retourne aussi les métadonnées inactives en lecture seule afin de préserver et expliquer leur configuration.
- Aucun enum ou colonne de mode d'exposition n'est ajouté au backend.
- Aucun appel HTTP par clic; une sauvegarde explicite envoie seulement les tables modifiées.

## Existing tests and validation commands

- `mvn '-Dtest=ReportPreviewServiceTest,ReportSqlBuilderTest,ReportControllerSecurityTest' test` — exécuté le 2026-08-24 : 25 tests, 0 failure, 0 error, 0 skipped.
- `npm.cmd test -- --watch=false --include="src/app/features/rapports/services/dataset.service.spec.ts" --include="src/app/features/rapports/pages/source_de_donnes/rapports.component.spec.ts" --include="src/app/features/rapports/pages/configuration/report-configuration-loader.service.spec.ts"` — exécuté le 2026-08-24 : 20 tests success.
- `mvn test` — non relancé pour cette recherche. La baseline observée le 2026-08-23 avait 59 tests, 1 failure, 3 errors et 4 skipped; les erreurs principales venaient de `ReportJobProperties.xlsxRowWindow=0` dans des tests construisant directement les propriétés. Les tests PostgreSQL nécessitent Docker.
- `npm.cmd test -- --watch=false` — non relancé pour cette recherche. La baseline observée le 2026-08-23 avait 120/121 tests success; un test `ExportComponent` échouait.
- `npm.cmd run build` — non relancé. Le build du 2026-08-23 réussissait avec des warnings de budgets initial et SCSS.

## Risks and unknowns

| Item | Type | Impact | How to resolve |
|---|---|---|---|
| Les deux booléens table sont un diff utilisateur non commité | Risk | Le feature work peut écraser ou figer une intention incomplète | Stabiliser d'abord ce diff avec tests dédiés, sans réécriture hors périmètre |
| SQL `display_r    elated` invalide | Risk | Les relations échouent sur PostgreSQL réel | Test Testcontainers de la query native avant UI |
| `ddl-auto=create` | Risk | Toute configuration disparaît au redémarrage | Décision de déploiement explicite avant production; migration hors de cette phase documentaire |
| Deux administrateurs modifient simultanément | Assumption | Last-write-wins sur les mêmes propriétés | Envoyer seulement les changements; accepter ce risque initial ou approuver plus tard un `@Version` et sa migration |
| Export déjà prêt après désactivation | Decision required | Des données devenues non exposées restent téléchargeables | Revalider `definitionJson` à la création et au download; bloquer sans supprimer l'artifact |
| Job désactivé entre enqueue et worker | Risk | Le job échoue avec code générique | Mapper l'indisponibilité vers `REPORT_DEFINITION_UNAVAILABLE` |
| Aucun rapport persistant | Fact | Le critère de réactivation concerne draft et génération, pas une entité de rapport | Documenter et réutiliser le resolver lorsqu'un catalogue de rapports sera créé |
| Graphify peut redevenir obsolète après une modification | Mitigated | Le graphe a été régénéré le 24 août et reconnaît les symboles actuels | Exécuter `graphify update .` depuis la racine commune après les changements de code, puis confirmer les prédicats importants dans les sources |
| Prototype admin hors branche active | Risk | Cherry-pick brut introduirait mocks et ancienne sémantique | Réutiliser uniquement la direction visuelle et les tests pertinents |

## Relevant files and symbols

Backend:

- `src/main/java/RHIS/com/RHIS/dataset/entity/DataSetEntity.java` — `active`, `displayMain`, `displayRelated`, defaults.
- `src/main/java/RHIS/com/RHIS/dataset/entity/DataSetField.java` — `active`, `visible`.
- `src/main/java/RHIS/com/RHIS/dataset/bootstrap/DataSetInitializer.java` — synchronisation et defaults.
- `src/main/java/RHIS/com/RHIS/dataset/repository/DataSetRepository.java` — principales, relations, future lecture admin.
- `src/main/java/RHIS/com/RHIS/dataset/repository/DataSetFieldRepository.java` — visibilité des champs.
- `src/main/java/RHIS/com/RHIS/dataset/service/DataSetServiceImpl.java` — catalogue report-builder.
- `src/main/java/RHIS/com/RHIS/dataset/controller/DataSetController.java` — contrats publics actuels.
- `src/main/java/RHIS/com/RHIS/report/service/ReportDefinitionResolver.java` — enforcement central.
- `src/main/java/RHIS/com/RHIS/report/service/ReportSqlBuilder.java` — usage technique des colonnes.
- `src/main/java/RHIS/com/RHIS/report/service/ReportGenerationService.java` et `ReportGenerationWorker.java` — double validation.
- `src/main/java/RHIS/com/RHIS/report/service/ReportExportService.java` — export/download sans revalidation.
- `src/main/java/RHIS/com/RHIS/auth/SecurityConfig.java` — barrière `/api/v1/admin/**`.

Frontend:

- `src/app/app.routes.ts` — route admin absente.
- `src/app/features/auth/services/auth.service.ts` — `me()` absent et import environment incorrect.
- `src/app/features/rapports/services/dataset.service.ts` — endpoints metadata.
- `src/app/features/rapports/pages/source_de_donnes/rapports.component.ts` — tables principales et graphe sortant.
- `src/app/features/rapports/pages/configuration/report-configuration-loader.service.ts` — chargement des champs liés.
- Branche `codex/data-admin-ui-prototype`, `src/app/features/data-admin/**` — prototype mock de référence seulement.

## Conclusions for planning

### Approches comparées

1. **Recommandée — renforcer les frontières existantes.** Ajouter une API admin transactionnelle, contextualiser le chargement des champs et corriger `ReportDefinitionResolver`/la query de relations. Avantages : modèle inchangé, contrôle central contre les requêtes forgées, peu d'abstractions. Coût : modification coordonnée backend/frontend et contrat supplémentaire pour les champs liés.
2. **Ajouter un service générique de policy d'exposition.** Centralise chaque prédicat mais introduit une abstraction transversale avant qu'il existe plusieurs politiques ou implémentations. Rejetée pour YAGNI et risque de divergence avec les repositories.
3. **Filtrer seulement les endpoints de métadonnées.** Plus petit diff apparent, mais une requête forgée contournerait l'UI et les exports existants resteraient accessibles. Rejetée pour sécurité et intégrité fonctionnelle.

L'approche 1 est la plus petite solution production-quality : elle réutilise les booléens, les repositories et le resolver déjà présents. Le mode à quatre valeurs reste strictement frontend et est reconverti en deux booléens avant le PUT.

## Open questions

Décisions proposées pour validation avec l'ExecPlan :

1. Conserver les defaults vérifiés : nouvelle table `Principale uniquement`, nouveau champ exposé.
2. Afficher les tables/champs inactifs dans l'administration, mais désactiver leurs contrôles.
3. Bloquer création et téléchargement d'un export existant si sa définition n'est plus exposée, sans supprimer snapshot ni fichier; la réactivation réautorise l'action.
4. Accepter temporairement le last-write-wins entre administrateurs en envoyant seulement les propriétés modifiées; ne pas ajouter `@Version` dans ce périmètre.
5. Utiliser `GET/PUT /api/v1/admin/dataset-exposure`, conserver `GET /datasets/{id}/fields` pour la racine et ajouter `GET /datasets/{rootId}/relations/{relatedId}/fields` pour une cible liée.
6. Considérer la suppression de `ddl-auto=create` et l'introduction de migrations comme un préalable de production séparé; aucune migration n'est créée dans cette première exécution.
