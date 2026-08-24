# Report Export Front/Back KISS Refactoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corriger les deux défauts de correctness identifiés et clarifier le workflow Export Angular/Spring Boot par des renommages et simplifications strictement locaux.

**Architecture:** Le composant Angular reste l’orchestrateur unique de la page et le backend conserve ses contrôleurs, services, workers et frontières de stockage actuels. Les changements portent uniquement sur la reprise réseau, l’authentification du workflow, le naming des opérations existantes et deux simplifications internes sans modifier les contrats HTTP ou le modèle de données.

**Tech Stack:** Angular 20.3, TypeScript 5.9, Signals, RxJS 7.8, Jasmine/Karma, PrimeNG 20 ; Java, Spring Boot, Spring Security, JPA/Hibernate, JUnit 5, Mockito, PostgreSQL/Testcontainers, Maven.

## Global Constraints

- Ne créer aucun composant, service, facade, store, mapper, factory, strategy, interface ou couche supplémentaire.
- Ne modifier aucun endpoint, payload, DTO, modèle Angular, entité JPA, schéma, intervalle de polling, comportement de téléchargement, clé de session storage ou rendu UI.
- Ne modifier aucun fichier HTML ou SCSS.
- Conserver `ReportExportWriter`, `ReportArtifactStorage` et `ReportJobDispatcher`.
- Conserver les méthodes backend publiques `create`, `get` et `delete` lorsque le type du service fournit déjà le contexte métier.
- La seule évolution observable autorisée est `401 Unauthorized` pour les routes Report/Export anonymes et la disparition du bandeau réseau après une reprise de polling réussie.
- Préserver tous les changements locaux hors périmètre. Plusieurs fichiers Report sont non suivis par Git et `SecurityConfig.java` est déjà modifié par l’utilisateur : ne créer aucun commit applicatif qui engloberait leur contenu préexistant sans baseline fiable.
- Utiliser `apply_patch` pour chaque modification manuelle.
- Le warning global préexistant du bundle Angular initial peut rester ; aucun nouveau warning de budget propre au composant Export n’est accepté.

---

### Task 1: Corriger la reprise réseau du polling d’export

**Files:**
- Modify: frontend `src/app/features/rapports/pages/export/export.component.spec.ts:120`
- Modify: frontend `src/app/features/rapports/pages/export/export.component.ts:400`

**Interfaces:**
- Consumes: `networkInterrupted: WritableSignal<boolean>` et `pollExport(exportId: string, format: ReportExportFormat): void`.
- Produces: le polling d’export remet `networkInterrupted` à `false` dès sa première réponse HTTP réussie.

- [ ] **Step 1: Ajouter l’assertion de régression au test existant**

Dans `retries export polling after a transient server failure`, compléter les assertions après `tick(2_000)` :

```typescript
expect(reportService.export).toHaveBeenCalledTimes(2);
expect(component.networkInterrupted()).toBeFalse();
expect(component.pdfExport()).toEqual(readyPdfExport);
```

- [ ] **Step 2: Exécuter la spec Export et constater l’échec**

Run depuis le frontend :

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/features/rapports/pages/export/export.component.spec.ts
```

Expected: FAIL sur `Expected true to be false` dans le scénario de retry du polling d’export.

- [ ] **Step 3: Réinitialiser le signal dans le chemin de succès**

Modifier uniquement le callback `next` de `pollExport` :

```typescript
next: (reportExport) => {
  this.networkInterrupted.set(false);
  this.setFormatError(format, null);
  this.exportSignal(format).set(reportExport);
},
```

- [ ] **Step 4: Réexécuter la spec Export**

Run :

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/features/rapports/pages/export/export.component.spec.ts
```

Expected: PASS pour toute la spec Export.

- [ ] **Step 5: Vérifier le diff ciblé sans commit applicatif**

Run :

```powershell
git diff --check -- src/app/features/rapports/pages/export/export.component.ts src/app/features/rapports/pages/export/export.component.spec.ts
git diff --stat -- src/app/features/rapports/pages/export/export.component.ts src/app/features/rapports/pages/export/export.component.spec.ts
```

Expected: seulement une assertion de test et une remise à `false`; aucun changement HTML/SCSS.

---

### Task 2: Clarifier le naming HTTP et les dépendances Angular

**Files:**
- Modify: frontend `src/app/features/rapports/services/report-generation.service.spec.ts`
- Modify: frontend `src/app/features/rapports/services/report-generation.service.ts`
- Modify: frontend `src/app/features/rapports/services/report-draft-storage.service.ts`
- Modify: frontend `src/app/features/rapports/pages/configuration/configuration.component.spec.ts`
- Modify: frontend `src/app/features/rapports/pages/configuration/configuration.component.ts:72`
- Modify: frontend `src/app/features/rapports/pages/export/export.component.spec.ts`
- Modify: frontend `src/app/features/rapports/pages/export/export.component.ts:97`

**Interfaces:**
- Consumes: les mêmes URLs, payloads, options `withCredentials` et types `Observable` du service HTTP actuel.
- Produces:
  - `startReportGeneration(definition: ReportPreviewRequest, idempotencyKey: string): Observable<ReportGeneration>`;
  - `getReportGeneration(generationId: string): Observable<ReportGeneration>`;
  - `deleteGeneration(generationId: string): Observable<void>`;
  - `startReportExport(generationId: string, format: ReportExportFormat): Observable<ReportExport>`;
  - `getReportExport(exportId: string): Observable<ReportExport>`;
  - `downloadExportFile(exportId: string): Observable<HttpResponse<Blob>>`.

- [ ] **Step 1: Renommer les appels dans les doubles de tests**

Dans la spec Configuration, remplacer le double par :

```typescript
const reportGenerationService = {
  startReportGeneration: jasmine.createSpy().and.returnValue(of({ generationId: 'generation-id' })),
};
```

Réinitialiser et configurer `reportGenerationService.startReportGeneration` dans `beforeEach`, puis adapter toutes les assertions qui visaient `.create`.

Dans la spec Export, renommer également les doubles pour refléter les dépendances injectées, puis remplacer exactement leurs propriétés :

```typescript
const reportGenerationService = {
  getReportGeneration: jasmine.createSpy(),
  startReportExport: jasmine.createSpy(),
  getReportExport: jasmine.createSpy(),
  downloadExportFile: jasmine.createSpy(),
  deleteGeneration: jasmine.createSpy(),
};

const reportDraftStorage = {
  loadExportIds: jasmine.createSpy(),
  saveExportId: jasmine.createSpy(),
  clearExportIds: jasmine.createSpy(),
  load: jasmine.createSpy(),
};
```

Adapter les resets, `callFake`, `returnValue`, providers et assertions aux nouveaux noms `reportGenerationService` et `reportDraftStorage`.

Dans la spec du service HTTP, appeler les six signatures produites ci-dessus sans changer les URLs ni les assertions de requête.

- [ ] **Step 2: Exécuter les specs ciblées et constater les erreurs de compilation**

Run depuis le frontend :

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/features/rapports/services/report-generation.service.spec.ts --include=src/app/features/rapports/pages/configuration/configuration.component.spec.ts --include=src/app/features/rapports/pages/export/export.component.spec.ts
```

Expected: FAIL de compilation car les nouvelles méthodes ne sont pas encore déclarées dans `ReportGenerationService` et les composants utilisent encore les anciens noms.

- [ ] **Step 3: Renommer les méthodes du service sans toucher aux contrats HTTP**

Le corps public du service doit exposer ces signatures et conserver les implémentations actuelles :

```typescript
startReportGeneration(
  definition: ReportPreviewRequest,
  idempotencyKey: string,
): Observable<ReportGeneration> {
  return this.http.post<ReportGeneration>(this.generationsUrl, definition, {
    headers: { 'Idempotency-Key': idempotencyKey },
    withCredentials: true,
  });
}

getReportGeneration(generationId: string): Observable<ReportGeneration> {
  return this.http.get<ReportGeneration>(`${this.generationsUrl}/${generationId}`, {
    withCredentials: true,
  });
}

startReportExport(
  generationId: string,
  format: ReportExportFormat,
): Observable<ReportExport> {
  return this.http.post<ReportExport>(
    `${this.generationsUrl}/${generationId}/exports`,
    { format },
    { withCredentials: true },
  );
}

getReportExport(exportId: string): Observable<ReportExport> {
  return this.http.get<ReportExport>(`${this.exportsUrl}/${exportId}`, {
    withCredentials: true,
  });
}

downloadExportFile(exportId: string): Observable<HttpResponse<Blob>> {
  return this.http.get(`${this.exportsUrl}/${exportId}/file`, {
    observe: 'response',
    responseType: 'blob',
    withCredentials: true,
  });
}
```

Conserver `deleteGeneration()` sans changement.

- [ ] **Step 4: Adapter les points d’appel et les dépendances injectées**

Dans Configuration, remplacer uniquement :

```typescript
this.reportGenerationService
  .startReportGeneration(definition, globalThis.crypto.randomUUID())
```

Dans Export, renommer les champs injectés :

```typescript
private readonly reportGenerationService = inject(ReportGenerationService);
private readonly reportDraftStorage = inject(ReportDraftStorageService);
```

Puis appliquer ces correspondances à tous leurs usages dans le composant :

```text
this.reportService.createExport     -> this.reportGenerationService.startReportExport
this.reportService.generation       -> this.reportGenerationService.getReportGeneration
this.reportService.export           -> this.reportGenerationService.getReportExport
this.reportService.download         -> this.reportGenerationService.downloadExportFile
this.reportService.deleteGeneration -> this.reportGenerationService.deleteGeneration
this.draftStorage                    -> this.reportDraftStorage
```

- [ ] **Step 5: Clarifier uniquement les variables locales du stockage**

Appliquer exactement les renommages suivants dans `ReportDraftStorageService` :

```text
load(): value                    -> parsedDraft
saveExportId(): current          -> savedExportIds
loadExportIds(): value           -> parsedExportIds
```

Les méthodes publiques `save`, `load`, `saveExportId`, `loadExportIds` et `clearExportIds` restent inchangées.

- [ ] **Step 6: Vérifier l’absence des anciens noms ambigus**

Run depuis le frontend :

```powershell
rg -n "reportService|draftStorage|\.createExport\(|\.generation\(|\.export\(|\.download\(|\.create\(definition" src/app/features/rapports/pages src/app/features/rapports/services -g "*.ts"
```

Expected: aucune occurrence liée à `ReportGenerationService`; les noms métier `createExport()` du composant et les usages sans rapport avec ce service peuvent rester.

- [ ] **Step 7: Exécuter les specs Angular concernées**

Run :

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/features/rapports/services/report-generation.service.spec.ts --include=src/app/features/rapports/services/report-draft-storage.service.spec.ts --include=src/app/features/rapports/pages/configuration/configuration.component.spec.ts --include=src/app/features/rapports/pages/export/export.component.spec.ts
```

Expected: PASS pour le service HTTP, le stockage, Configuration et Export.

- [ ] **Step 8: Vérifier le diff ciblé sans commit applicatif**

Run :

```powershell
git diff --check -- src/app/features/rapports/pages/configuration/configuration.component.ts src/app/features/rapports/pages/configuration/configuration.component.spec.ts src/app/features/rapports/pages/export/export.component.ts src/app/features/rapports/pages/export/export.component.spec.ts
git status --short -- src/app/features/rapports/services/report-generation.service.ts src/app/features/rapports/services/report-generation.service.spec.ts src/app/features/rapports/services/report-draft-storage.service.ts
```

Expected: changements limités au naming TypeScript; les fichiers de service restent non suivis tant qu’aucune baseline utilisateur n’est commitée.

---

### Task 3: Restaurer la frontière d’authentification du workflow Report/Export

**Files:**
- Modify: backend `src/test/java/RHIS/com/RHIS/report/controller/ReportControllerSecurityTest.java`
- Modify: backend `src/main/java/RHIS/com/RHIS/auth/SecurityConfig.java:57`

**Interfaces:**
- Consumes: `SecurityFilterChain`, `JwtCookieFilter`, `ReportPreviewService`, `ReportGenerationService`, `ReportExportService`.
- Produces: `401 Unauthorized` avant l’entrée dans les contrôleurs pour tout appel anonyme vers Dataset, Report, ReportGeneration ou ReportExport.

- [ ] **Step 1: Charger les trois contrôleurs dans le slice MVC**

Ajouter les imports des deux services et remplacer l’annotation :

```java
import RHIS.com.RHIS.report.service.ReportExportService;
import RHIS.com.RHIS.report.service.ReportGenerationService;

@WebMvcTest({
        ReportController.class,
        ReportGenerationController.class,
        ReportExportController.class
})
```

Ajouter les mocks nécessaires au chargement des contrôleurs :

```java
@MockitoBean
private ReportGenerationService reportGenerationService;

@MockitoBean
private ReportExportService reportExportService;
```

- [ ] **Step 2: Couvrir une route d’export réellement mappée**

Ajouter le test :

```java
@Test
void rejectsUnauthenticatedExportPolling() throws Exception {
    mockMvc.perform(get("/api/v1/report-exports/8574b46f-a936-46e7-92a9-cc49ec41b652"))
            .andExpect(status().isUnauthorized());
}
```

Conserver le test de génération existant ; il devient désormais une vérification d’une route réellement déclarée dans le slice.

- [ ] **Step 3: Exécuter le test de sécurité et constater l’échec**

Run depuis le backend :

```powershell
mvn -Dtest=ReportControllerSecurityTest test
```

Expected: FAIL, les routes anonymes n’étant pas encore protégées par le matcher Report/Export.

- [ ] **Step 4: Exiger l’authentification et activer l’entry point 401**

Dans `SecurityConfig`, conserver les quatre routes du matcher et remplacer uniquement sa décision :

```java
.requestMatchers(
        "/api/v1/datasets/**",
        "/api/v1/reports/**",
        "/api/v1/report-generations/**",
        "/api/v1/report-exports/**"
)
.authenticated()
```

Activer ensuite la gestion d’erreur déjà préparée :

```java
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
.exceptionHandling(exception -> exception
        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
.addFilterBefore(
        jwtCookieFilter,
        UsernamePasswordAuthenticationFilter.class
);
```

Ne modifier aucune autre règle, origine CORS ou configuration CSRF.

- [ ] **Step 5: Réexécuter le test de sécurité**

Run :

```powershell
mvn -Dtest=ReportControllerSecurityTest test
```

Expected: PASS pour les appels anonymes et les scénarios authentifiés existants.

- [ ] **Step 6: Vérifier le diff sans absorber d’autres changements utilisateur**

Run :

```powershell
git diff --check -- src/main/java/RHIS/com/RHIS/auth/SecurityConfig.java
git diff -- src/main/java/RHIS/com/RHIS/auth/SecurityConfig.java
git status --short -- src/test/java/RHIS/com/RHIS/report/controller/ReportControllerSecurityTest.java
```

Expected: la configuration suivie ne change que sur le bloc Report/Export et l’entry point ; le test reste non suivi et n’est pas commit tant qu’aucune baseline utilisateur n’existe.

---

### Task 4: Nommer explicitement l’enregistrement de progression backend

**Files:**
- Modify: backend `src/test/java/RHIS/com/RHIS/report/service/ReportJobStateServiceTest.java`
- Modify: backend `src/test/java/RHIS/com/RHIS/report/service/ReportExportWorkerTest.java`
- Modify: backend `src/main/java/RHIS/com/RHIS/report/service/ReportJobStateService.java:53`
- Modify: backend `src/main/java/RHIS/com/RHIS/report/service/ReportGenerationWorker.java:61`
- Modify: backend `src/main/java/RHIS/com/RHIS/report/service/ReportExportWorker.java:49`

**Interfaces:**
- Consumes: les mêmes identifiants UUID, phases, progressions, row counts et transactions `REQUIRES_NEW`.
- Produces:
  - `recordGenerationProgress(UUID id, ReportGenerationPhase phase, int progress, long processedRows, Long totalRows): void`;
  - `recordExportProgress(UUID id, int progress): void`.

- [ ] **Step 1: Renommer les attentes des tests**

Dans `ReportJobStateServiceTest` :

```java
stateService.recordExportProgress(exportId, 70);
stateService.recordExportProgress(exportId, 20);
stateService.recordExportProgress(exportId, 120);
```

Dans `ReportExportWorkerTest` :

```java
}).when(stateService).recordExportProgress(eq(exportId), anyInt());
```

- [ ] **Step 2: Compiler les tests ciblés et constater l’absence des nouvelles méthodes**

Run depuis le backend :

```powershell
mvn "-Dtest=ReportJobStateServiceTest,ReportExportWorkerTest" test
```

Expected: FAIL de compilation sur `recordExportProgress`.

- [ ] **Step 3: Renommer les deux méthodes du service d’état**

Conserver annotations, paramètres et corps ; changer uniquement les déclarations :

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void recordGenerationProgress(
        UUID id,
        ReportGenerationPhase phase,
        int progress,
        long processedRows,
        Long totalRows
) {
```

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void recordExportProgress(UUID id, int progress) {
```

- [ ] **Step 4: Adapter les workers aux nouvelles signatures**

Dans `ReportGenerationWorker`, remplacer les quatre appels à `stateService.updateGeneration` par `stateService.recordGenerationProgress` sans changer leurs arguments.

Dans `ReportExportWorker`, remplacer la callback par :

```java
writer.write(
        snapshot,
        output,
        progress -> stateService.recordExportProgress(exportId, progress)
);
```

- [ ] **Step 5: Vérifier la disparition des anciens noms**

Run :

```powershell
rg -n "updateGeneration|updateExport" src/main/java/RHIS/com/RHIS/report src/test/java/RHIS/com/RHIS/report -g "*.java"
```

Expected: aucune occurrence.

- [ ] **Step 6: Réexécuter les tests ciblés**

Run :

```powershell
mvn "-Dtest=ReportJobStateServiceTest,ReportExportWorkerTest" test
```

Expected: PASS ; la progression reste monotone, bornée à 99 et transmise dans le même ordre.

---

### Task 5: Simplifier les détails internes backend sans nouvelle abstraction

**Files:**
- Modify: backend `src/main/java/RHIS/com/RHIS/report/service/ReportGenerationService.java:39`
- Modify: backend `src/main/java/RHIS/com/RHIS/report/service/ReportExportService.java:47`
- Modify: backend `src/main/java/RHIS/com/RHIS/report/export/XlsxReportExportWriter.java:23`

**Interfaces:**
- Consumes: repositories actuels, contrôle propriétaire `findOwned`, `ReportArtifactStorage.open`, consumer séquentiel du snapshot.
- Produces: les mêmes méthodes publiques backend et les mêmes fichiers XLSX ; aucune nouvelle interface.

- [ ] **Step 1: Exécuter les tests de caractérisation avant simplification**

Run depuis le backend :

```powershell
mvn "-Dtest=ReportExportWriterTest,ReportPreviewPostgresIntegrationTest" test
```

Expected: PASS avant modification, notamment pour XLSX typé, téléchargement propriétaire et refus d’accès par un autre utilisateur.

- [ ] **Step 2: Clarifier le repository de génération**

Dans `ReportGenerationService`, renommer le champ :

```java
private final ReportGenerationRepository generationRepository;
```

Remplacer les huit usages de `repository` dans la classe par `generationRepository`. Ne renommer aucun autre champ ou méthode.

- [ ] **Step 3: Supprimer les indirections sans règle autonome du téléchargement**

Dans `ReportExportService.create`, construire directement l’exception :

```java
ReportGenerationEntity generation = generationRepository
        .findByIdAndOwnerIdForUpdate(generationId, ownerId)
        .orElseThrow(() -> new ReportResourceNotFoundException(
                "La génération demandée est introuvable."
        ));
```

Remplacer `openDownload` par :

```java
@Transactional(readOnly = true)
public DownloadPayload openDownload(Long ownerId, UUID exportId) {
    ReportExportEntity export = findOwned(ownerId, exportId);
    if (export.getStatus() != ReportExportStatus.READY || export.getFileLocation() == null) {
        throw new ReportConflictException("Le fichier n'est pas encore prêt.");
    }
    try {
        InputStream input = storage.open(export.getFileLocation());
        String fileName = "rapport-" + export.getId() + '.' + export.getFormat().extension();
        return new DownloadPayload(input, export.getFormat().contentType(), fileName);
    } catch (IOException exception) {
        throw new ReportExecutionException("Le fichier exporté ne peut pas être ouvert.", exception);
    }
}
```

Dans `findOwned`, construire directement `ReportResourceNotFoundException`. Supprimer ensuite `findReadyForDownload()` et `notFound(String)` ; aucun appel ne doit rester.

- [ ] **Step 4: Remplacer l’AtomicLong local du writer XLSX**

Supprimer l’import `java.util.concurrent.atomic.AtomicLong` et la variable locale `AtomicLong rowIndex`.

Déclarer l’index dans le consumer séquentiel :

```java
snapshotReader.read(snapshot, new ReportSnapshotReader.SnapshotConsumer() {
    private ReportSnapshotMetadata metadata;
    private long rowIndex;

    @Override
    public void start(ReportSnapshotMetadata value) {
        metadata = value;
        Row header = sheet.createRow(0);
        for (int index = 0; index < metadata.columns().size(); index++) {
            Cell cell = header.createCell(index, CellType.STRING);
            cell.setCellValue(metadata.columns().get(index).displayName());
            cell.setCellStyle(headerStyle);
            int width = Math.min(
                    40,
                    Math.max(12, metadata.columns().get(index).displayName().length() + 2)
            );
            sheet.setColumnWidth(index, width * 256);
        }
        sheet.createFreezePane(0, 1);
    }

    @Override
    public void row(Map<String, Object> values) {
        long current = ++rowIndex;
        Row row = sheet.createRow(Math.toIntExact(current));
        for (int index = 0; index < metadata.columns().size(); index++) {
            ReportSnapshotMetadata.Column column = metadata.columns().get(index);
            writeCell(
                    row.createCell(index),
                    values.get(column.key()),
                    column.dataType(),
                    dateStyle,
                    dateTimeStyle
            );
        }
        progress.accept(exportProgress(current, metadata.rowCount()));
    }
});
```

- [ ] **Step 5: Vérifier les suppressions et les renommages**

Run :

```powershell
rg -n "findReadyForDownload|notFound\(|AtomicLong" src/main/java/RHIS/com/RHIS/report/service/ReportExportService.java src/main/java/RHIS/com/RHIS/report/export/XlsxReportExportWriter.java
rg -n "repository\.(find|count|delete|save)" src/main/java/RHIS/com/RHIS/report/service/ReportGenerationService.java
```

Expected: aucune occurrence. L’`AtomicLong` de `ReportGenerationWorker` n’est pas inclus dans cette recherche et reste inchangé.

- [ ] **Step 6: Réexécuter les tests de caractérisation**

Run :

```powershell
mvn "-Dtest=ReportExportWriterTest,ReportPreviewPostgresIntegrationTest" test
```

Expected: PASS avec les mêmes assertions de contenu, progression, propriété et téléchargement.

---

### Task 6: Validation end-to-end et contrôle de périmètre

**Files:**
- Verify only: tous les fichiers modifiés dans les Tasks 1 à 5.
- Do not modify: HTML, SCSS, modèles, DTO, entités, repositories, migrations, endpoints ou dépendances.

**Interfaces:**
- Consumes: le workflow refactorisé complet.
- Produces: preuve que les corrections et renommages n’ont introduit aucune régression front/back.

- [ ] **Step 1: Exécuter toute la suite Angular**

Run depuis le frontend :

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless
```

Expected: 100 tests ou davantage, tous PASS.

- [ ] **Step 2: Construire le frontend de production**

Run :

```powershell
npm.cmd run build
```

Expected: build réussi ; le warning global préexistant du bundle initial peut rester, sans warning propre au composant Export.

- [ ] **Step 3: Exécuter toute la suite backend**

Run depuis le backend :

```powershell
mvn test
```

Expected: tous les tests PASS, y compris `ReportControllerSecurityTest` et `ReportPreviewPostgresIntegrationTest`.

- [ ] **Step 4: Contrôler les signatures et anciens noms**

Run depuis `C:\Users\Surface Pro\Downloads\RHIS` :

```powershell
rg -n "updateGeneration|updateExport|findReadyForDownload|reportService|draftStorage" RHIS/src Frontend/Rhis_report_gen/src -g "*.java" -g "*.ts"
```

Expected: aucune occurrence appartenant au workflow refactorisé.

- [ ] **Step 5: Contrôler le périmètre des fichiers modifiés**

Run séparément dans chaque dépôt :

```powershell
git status --short
git diff --check
```

Expected: aucun HTML/SCSS, modèle, DTO, entité, repository, migration ou dépendance modifié par ce refactoring. Les changements préexistants hors périmètre restent intacts et ne sont ni nettoyés ni commités.

- [ ] **Step 6: Produire le compte rendu final**

Le compte rendu doit indiquer exactement :

```text
- correction frontend validée : bandeau réseau masqué après reprise réussie ;
- correction backend validée : routes Report/Export anonymes refusées en 401 ;
- renommages front/back appliqués sans changement de contrat ;
- suites Angular et Maven : nombre de tests et résultat ;
- build Angular : résultat et warnings préexistants éventuels ;
- commits applicatifs : aucun, afin de ne pas absorber les fichiers utilisateur non suivis ou déjà modifiés.
```
