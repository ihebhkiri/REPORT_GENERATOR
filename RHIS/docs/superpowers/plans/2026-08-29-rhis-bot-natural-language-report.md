# rhis_bot — Plan d'implémentation (phase 1 backend)

> **Mode d'exécution : Ponytail.** Exécution inline dans cette session, un commit par
> milestone validé (autorisé par l'utilisateur le 29/08/2026), préservation stricte du
> travail préexistant, empreintes SHA256 des fichiers sales, rollback par hunks.
> Les étapes utilisent la syntaxe checkbox `- [ ]`.

**Goal :** `POST /api/v1/bot/reports` transforme une phrase française en définition de rapport
validée, puis crée une génération via le pipeline existant (202 + generationId + format).

**Architecture :** nouveau module `RHIS.com.RHIS.bot` : catalogue compact → planneur LLM
(Spring AI ChatClient + starter Mistral, structured output) → mapping vers
`ReportPreviewRequest` → `ReportDefinitionResolver` (garde-fou existant) →
`ReportGenerationService.create()` (existant). Deux garde-fous : `NEEDS_CLARIFICATION`
et une seule auto-correction. Aucune modification du pipeline report/dataset existant.

**Tech stack :** Java 17, Spring Boot 4.1.0, Spring AI 2.0.x (BOM) + `spring-ai-starter-model-mistralai`,
Mistral `mistral-small-latest` (température 0.0), JUnit 5 + Mockito + MockMvc, PostgreSQL existant.

**Accord Ponytail :** « je valide le spec », 29/08/2026 ; commits par milestone autorisés ;
exécution locale, sans délégation ni push ; spec : `RHIS/docs/superpowers/specs/2026-08-29-rhis-bot-natural-language-report-design.md`.

## Global Constraints

- Java 17, Spring Boot 4.1.0 — ne pas monter de version du parent.
- Jamais de données métier vers le LLM : uniquement catalogue (displayName/IDs/types/opérateurs)
  + phrase utilisateur + date du jour.
- Le LLM ne produit que des IDs/opérateurs/valeurs texte ; seul `ReportSqlBuilder` écrit du SQL.
- Opérateurs autorisés = enum Java `FilterOperator` exact : EQUALS, CONTAINS, GREATER_THAN,
  GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, BETWEEN. Tris = `SortDirection` ASC|DESC.
- Clé API Mistral : variable d'environnement `MISTRAL_API_KEY` uniquement, jamais dans le dépôt.
- `/api/v1/bot/**` doit être `authenticated()` (le `anyRequest().permitAll()` final du
  `SecurityConfig` couvrirait sinon l'endpoint).
- Préserver tout le travail préexistant (worktree sale) — voir Baselines.
- Ne jamais push, merge, rebase ; commits locaux sur `rhis_bot` uniquement, un par milestone.
- Max 2 appels LLM par requête bot (1 + 1 auto-correction).

## Baselines préexistantes (à ne pas « corriger »)

- Worktree sale au 29/08/2026 : renames preview-dialog→preview-panel (4, staged),
  configuration.component.html/scss/spec/ts (M), `RHIS/docs/flows/02-*.md` + `03-*.md` (M),
  `docs/plans/` + `docs/research/2026-08-24-configuration-live-preview.md` (non suivis).
- Suite backend non verte (antérieure) : `ReportExportWriterTest` 1 failure + 3 errors
  (xlsxRowWindow=0), `ReportPreviewPostgresIntegrationTest` 4 errors de contexte
  (propriétés du pool absentes du profil test). Comparer toujours aux mêmes ensembles.
- Couverture Testcontainers requiert Docker ; si absent, le signaler explicitement.

## File Map

| Fichier | Action | Responsabilité |
|---|---|---|
| `RHIS/pom.xml` | Modifier | BOM spring-ai-bom + starter mistralai |
| `RHIS/src/main/resources/application.yaml` | Modifier | `spring.ai.mistralai.*` + `rhis.bot.*` |
| `RHIS/src/main/java/RHIS/com/RHIS/bot/config/BotAiProperties.java` | Créer | propriétés rhis.bot |
| `RHIS/src/main/java/RHIS/com/RHIS/bot/config/BotConfig.java` | Créer | activation des propriétés |
| `RHIS/src/main/java/RHIS/com/RHIS/bot/catalog/CatalogDataset.java` | Créer | record catalogue |
| `RHIS/src/main/java/RHIS/com/RHIS/bot/catalog/CatalogField.java` | Créer | record champ catalogue |
| `RHIS/src/main/java/RHIS/com/RHIS/bot/catalog/ReportCatalogProvider.java` | Créer | catalogue depuis les repos |
| `RHIS/src/main/java/RHIS/com/RHIS/bot/dto/BotReportPlan.java` | Créer | cible structured output |
| `RHIS/src/main/java/RHIS/com/RHIS/bot/BotReportPlanner.java` | Créer | appel LLM + timeout |
| `RHIS/src/main/java/RHIS/com/RHIS/bot/exception/BotLlmException.java` | Créer | échec/timeout LLM |
| `RHIS/src/main/java/RHIS/com/RHIS/bot/exception/BotRequestException.java` | Créer | requête invalide |
| `RHIS/src/main/java/RHIS/com/RHIS/bot/controller/dto/BotReportRequest.java` | Créer | { message, format? } |
| `RHIS/src/main/java/RHIS/com/RHIS/bot/controller/dto/BotReportResponse.java` | Créer | READY / CLARIFICATION / FAILED |
| `RHIS/src/main/java/RHIS/com/RHIS/bot/BotReportService.java` | Créer | orchestration + auto-correction |
| `RHIS/src/main/java/RHIS/com/RHIS/bot/BotReportController.java` | Créer | endpoint + 202/200/422 |
| `RHIS/src/main/java/RHIS/com/RHIS/bot/BotExceptionHandler.java` | Créer | 400 / 502 ProblemDetail |
| `RHIS/src/main/java/RHIS/com/RHIS/auth/SecurityConfig.java` | Modifier | 1 règle authenticated() |
| `RHIS/src/test/java/RHIS/com/RHIS/bot/**` | Créer | tests unitaires + slice |
| `RHIS/docs/flows/07-bot-natural-language-report.md` | Créer | doc de flux E2E |
| `RHIS/docs/superpowers/progress/2026-08-29-rhis-bot-natural-language-report.md` | Créer | progrès Ponytail |

### Task 0 : empreintes Ponytail (baseline, aucun commit)

**Files:**
- Create: `RHIS/target/ponytail-rhis-bot/baseline-dirty-files.json` (artefact local, `target/` est ignoré par git)

- [ ] **Step 1 : vérifier la branche et l'état**

Depuis `C:\Users\Surface Pro\Downloads\RHIS` :

```powershell
git branch --show-current   # attendu : rhis_bot
git status --porcelain      # attendu : exactement la liste des baselines du plan, rien de plus
```

- [ ] **Step 2 : figer les empreintes SHA256 des fichiers mentionnés par git status**

```powershell
$out = "RHIS\target\ponytail-rhis-bot"
New-Item -ItemType Directory -Force $out | Out-Null
$files = git status --porcelain | ForEach-Object {
  (($_ -replace '^\s*\S+\s+','') -replace '\s+->\s+',"`n") -split "`n" |
    ForEach-Object { $_.Trim().Trim('"') }
} | Where-Object { $_ -and (Test-Path -LiteralPath $_ -PathType Leaf) } |
  Sort-Object -Unique
$files | ForEach-Object {
  [PSCustomObject]@{ path = $_; sha256 = (Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash }
} | ConvertTo-Json | Set-Content -LiteralPath "$out\baseline-dirty-files.json" -Encoding utf8
```

Attendu : le JSON liste les 4 renames (nouveau chemin) + les 8 fichiers modifiés, chacun
avec son SHA256. Ce fichier est la référence du contrôle « empreintes inchangées » final.

- [ ] **Step 3 : baseline de compilation backend**

Depuis `RHIS` :

```powershell
mvn -q compile
```

Attendu : BUILD SUCCESS. Si échec, stopper et rapporter : la baseline serait cassée
avant toute modification.

### Task 1 : dépendances Spring AI + configuration rhis.bot

**Files:**
- Modify: `RHIS/pom.xml` (dependencyManagement + 1 dépendance)
- Modify: `RHIS/src/main/resources/application.yaml` (blocs `spring.ai` et `rhis.bot`)
- Create: `RHIS/src/main/java/RHIS/com/RHIS/bot/config/BotAiProperties.java`
- Create: `RHIS/src/main/java/RHIS/com/RHIS/bot/config/BotConfig.java`
- Test: `RHIS/src/test/java/RHIS/com/RHIS/bot/config/BotAiPropertiesTest.java`

**Interfaces:**
- Produces: `BotAiProperties` (`getMaxMessageLength()` : int, `getLlmTimeoutSeconds()` : int),
  bean enregistré par `BotConfig` — consommé par Tasks 3 et 4.

- [ ] **Step 1 : écrire le test qui échoue**

```java
package RHIS.com.RHIS.bot.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BotAiPropertiesTest {

    @Test
    void bindsConfigurationValues() {
        var source = new MapConfigurationPropertySource(Map.of(
                "rhis.bot.max-message-length", "1500",
                "rhis.bot.llm-timeout-seconds", "12"));
        BotAiProperties properties = new Binder(source)
                .bind("rhis.bot", Bindable.of(BotAiProperties.class))
                .get();
        assertEquals(1500, properties.getMaxMessageLength());
        assertEquals(12, properties.getLlmTimeoutSeconds());
    }

    @Test
    void providesDefaults() {
        BotAiProperties properties = new BotAiProperties();
        assertEquals(2000, properties.getMaxMessageLength());
        assertEquals(30, properties.getLlmTimeoutSeconds());
    }
}
```

- [ ] **Step 2 : exécuter, attendre l'échec de compilation**

Depuis `RHIS` : `mvn '-Dtest=BotAiPropertiesTest' test` — attendu : échec,
`BotAiProperties` n'existe pas encore.

- [ ] **Step 3 : pom.xml — BOM + starter**

Dans `RHIS/pom.xml`, insérer avant `<dependencies>` :

```xml
	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.ai</groupId>
				<artifactId>spring-ai-bom</artifactId>
				<version>2.0.1</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>
```

Puis dans `<dependencies>` :

```xml
		<dependency>
			<groupId>org.springframework.ai</groupId>
			<artifactId>spring-ai-starter-model-mistralai</artifactId>
		</dependency>
```

- [ ] **Step 4 : application.yaml**

Sous la clé `spring:` existante, ajouter au même niveau que `datasource:` :

```yaml
  ai:
    mistralai:
      api-key: ${MISTRAL_API_KEY:}
      chat:
        options:
          model: mistral-small-latest
          temperature: 0.0
```

Sous la clé `rhis:` existante (qui contient déjà `report:`), ajouter au même niveau :

```yaml
  bot:
    max-message-length: 2000
    llm-timeout-seconds: 30
```

Remarques : la valeur par défaut vide `${MISTRAL_API_KEY:}` évite une erreur de
placeholder non résolu au démarrage ; les tests unitaires ne chargent jamais
l'auto-configuration Mistral ; l'E2E (Task 6) exige la variable réelle.

- [ ] **Step 5 : implémentation minimale**

`RHIS/src/main/java/RHIS/com/RHIS/bot/config/BotAiProperties.java` :

```java
package RHIS.com.RHIS.bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("rhis.bot")
public class BotAiProperties {
    private int maxMessageLength = 2000;
    private int llmTimeoutSeconds = 30;
}
```

`RHIS/src/main/java/RHIS/com/RHIS/bot/config/BotConfig.java` :

```java
package RHIS.com.RHIS.bot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BotAiProperties.class)
public class BotConfig {
}
```

- [ ] **Step 6 : exécuter test + compilation**

Depuis `RHIS` :

```powershell
mvn '-Dtest=BotAiPropertiesTest' test
mvn -q compile
```

Attendu : 2/2 PASS et BUILD SUCCESS. Si le BOM/starter est introuvable :
`mvn dependency:tree '-Dincludes=org.springframework.ai'` pour diagnostiquer ;
repli documenté : version `2.0.0` (les deux stables d'après les docs Spring AI).

- [ ] **Step 7 : commit du milestone**

Depuis la racine du dépôt :

```powershell
git add RHIS/pom.xml RHIS/src/main/resources/application.yaml RHIS/src/main/java/RHIS/com/RHIS/bot/config/ RHIS/src/test/java/RHIS/com/RHIS/bot/config/
git commit -m "feat(bot): dependances Spring AI Mistral et configuration rhis.bot"
```

### Task 2 : ReportCatalogProvider (catalogue compact pour le prompt)

**Files:**
- Create: `RHIS/src/main/java/RHIS/com/RHIS/bot/catalog/CatalogDataset.java`
- Create: `RHIS/src/main/java/RHIS/com/RHIS/bot/catalog/CatalogField.java`
- Create: `RHIS/src/main/java/RHIS/com/RHIS/bot/catalog/ReportCatalogProvider.java`
- Test: `RHIS/src/test/java/RHIS/com/RHIS/bot/catalog/ReportCatalogProviderTest.java`

**Interfaces:**
- Consumes: `DataSetRepository.findByActiveTrueAndDisplayMainTrue()` (EntityGraph sur
  `dataSetFieldSet`), `DataSetFieldRepository.findVisibleFieldsByDatasetId(Long)`,
  `DataSetField.getDataType().isSupported()` / `.supportedOperators()` / `.name()`.
- Produces: `List<CatalogDataset>` via `buildCatalog()` — consommé par Task 4
  (sérialisé en JSON pour le prompt). `CatalogField(Long fieldId, String displayName,
  String type, List<String> operators)`.

- [ ] **Step 1 : écrire le test qui échoue**

```java
package RHIS.com.RHIS.bot.catalog;

import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.model.DataSetFieldType;
import RHIS.com.RHIS.dataset.repository.DataSetFieldRepository;
import RHIS.com.RHIS.dataset.repository.DataSetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportCatalogProviderTest {

    @Mock
    private DataSetRepository dataSetRepository;
    @Mock
    private DataSetFieldRepository dataSetFieldRepository;
    @InjectMocks
    private ReportCatalogProvider provider;

    @Test
    void buildsCatalogFromActiveMainDatasetsAndVisibleFields() {
        DataSetEntity dataset = new DataSetEntity("Employés", "employees");
        dataset.setId(1L);
        when(dataSetRepository.findByActiveTrueAndDisplayMainTrue()).thenReturn(List.of(dataset));
        DataSetField field = new DataSetField("Date d'embauche", "hire_date", 0, dataset);
        field.setId(10L);
        field.setDataType(DataSetFieldType.DATE);
        when(dataSetFieldRepository.findVisibleFieldsByDatasetId(1L)).thenReturn(List.of(field));

        List<CatalogDataset> catalog = provider.buildCatalog();

        assertEquals(1, catalog.size());
        assertEquals(1L, catalog.get(0).datasetId());
        assertEquals("Employés", catalog.get(0).displayName());
        CatalogField catalogField = catalog.get(0).fields().get(0);
        assertEquals(10L, catalogField.fieldId());
        assertEquals("DATE", catalogField.type());
        assertTrue(catalogField.operators().contains("BETWEEN"));
    }

    @Test
    void excludesUnsupportedFieldTypes() {
        DataSetEntity dataset = new DataSetEntity("Employés", "employees");
        dataset.setId(1L);
        when(dataSetRepository.findByActiveTrueAndDisplayMainTrue()).thenReturn(List.of(dataset));
        DataSetField unsupported = new DataSetField("Colonne binaire", "payload", 0, dataset);
        unsupported.setDataType(DataSetFieldType.UNSUPPORTED);
        when(dataSetFieldRepository.findVisibleFieldsByDatasetId(1L))
                .thenReturn(List.of(unsupported));

        List<CatalogDataset> catalog = provider.buildCatalog();

        assertTrue(catalog.get(0).fields().isEmpty());
    }
}
```

- [ ] **Step 2 : exécuter, attendre l'échec de compilation**

Depuis `RHIS` : `mvn '-Dtest=ReportCatalogProviderTest' test` — attendu : échec,
les classes du package `bot.catalog` n'existent pas encore.

- [ ] **Step 3 : implémentation minimale**

`RHIS/src/main/java/RHIS/com/RHIS/bot/catalog/CatalogDataset.java` :

```java
package RHIS.com.RHIS.bot.catalog;

import java.util.List;

public record CatalogDataset(Long datasetId, String displayName, List<CatalogField> fields) {
}
```

`RHIS/src/main/java/RHIS/com/RHIS/bot/catalog/CatalogField.java` :

```java
package RHIS.com.RHIS.bot.catalog;

import java.util.List;

public record CatalogField(Long fieldId, String displayName, String type, List<String> operators) {
}
```

`RHIS/src/main/java/RHIS/com/RHIS/bot/catalog/ReportCatalogProvider.java` :

```java
package RHIS.com.RHIS.bot.catalog;

import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.repository.DataSetFieldRepository;
import RHIS.com.RHIS.dataset.repository.DataSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportCatalogProvider {

    private final DataSetRepository dataSetRepository;
    private final DataSetFieldRepository dataSetFieldRepository;

    @Transactional(readOnly = true)
    public List<CatalogDataset> buildCatalog() {
        return dataSetRepository.findByActiveTrueAndDisplayMainTrue().stream()
                .map(dataset -> new CatalogDataset(
                        dataset.getId(),
                        dataset.getDisplayName(),
                        visibleFields(dataset.getId())))
                .toList();
    }

    private List<CatalogField> visibleFields(Long datasetId) {
        return dataSetFieldRepository.findVisibleFieldsByDatasetId(datasetId).stream()
                .filter(field -> field.getDataType().isSupported())
                .map(this::toCatalogField)
                .toList();
    }

    private CatalogField toCatalogField(DataSetField field) {
        return new CatalogField(
                field.getId(),
                field.getDisplayName(),
                field.getDataType().name(),
                field.getDataType().supportedOperators().stream().map(Enum::name).toList());
    }
}
```

- [ ] **Step 4 : exécuter le test**

Depuis `RHIS` : `mvn '-Dtest=ReportCatalogProviderTest' test` — attendu : 2/2 PASS.

- [ ] **Step 5 : commit du milestone**

Depuis la racine du dépôt :

```powershell
git add RHIS/src/main/java/RHIS/com/RHIS/bot/catalog/ RHIS/src/test/java/RHIS/com/RHIS/bot/catalog/
git commit -m "feat(bot): catalogue compact des datasets pour le prompt"
```

### Task 3 : BotReportPlan + BotReportPlanner (structured output Mistral)

**Files:**
- Create: `RHIS/src/main/java/RHIS/com/RHIS/bot/dto/BotReportPlan.java`
- Create: `RHIS/src/main/java/RHIS/com/RHIS/bot/exception/BotLlmException.java`
- Create: `RHIS/src/main/java/RHIS/com/RHIS/bot/BotReportPlanner.java`
- Test: `RHIS/src/test/java/RHIS/com/RHIS/bot/BotReportPlannerTest.java`

**Interfaces:**
- Consumes: `ChatClient.Builder` (auto-configuré par le starter mistralai), `BotAiProperties`
  (Task 1), catalogue sérialisé JSON (Task 2).
- Produces: record `BotReportPlan(String status, String question, String summary,
  Long rootDatasetId, List<Long> selectedFieldIds, List<PlanFilter> filters,
  List<PlanSort> sorts)` + `isReady()` / `needsClarification()` ; sous-records
  `PlanFilter(Long fieldId, String operator, List<String> values)` et
  `PlanSort(Long fieldId, String direction)` ; méthode
  `plan(String catalogJson, String message, List<String> previousErrors)` — consommés par Task 4.

- [ ] **Step 1 : écrire le test qui échoue**

```java
package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.bot.config.BotAiProperties;
import RHIS.com.RHIS.bot.dto.BotReportPlan;
import RHIS.com.RHIS.bot.exception.BotLlmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotReportPlannerTest {

    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec callSpec;

    private BotReportPlanner planner;
    private BotAiProperties properties;

    @BeforeEach
    void setUp() {
        properties = new BotAiProperties();
        properties.setLlmTimeoutSeconds(1);
        planner = new BotReportPlanner(chatClientBuilder, properties);
    }

    private void stubChain() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }

    private BotReportPlan readyPlan() {
        return new BotReportPlan("READY", null, "Rapport", 1L, List.of(10L), List.of(), List.of());
    }

    @Test
    void returnsPlanFromModel() {
        stubChain();
        BotReportPlan plan = readyPlan();
        when(callSpec.entity(BotReportPlan.class)).thenReturn(plan);

        BotReportPlan result = planner.plan("[]", "rapport employés", null);

        assertSame(plan, result);
    }

    @Test
    void includesPreviousErrorsInUserText() {
        stubChain();
        when(callSpec.entity(BotReportPlan.class)).thenReturn(readyPlan());

        planner.plan("[]", "rapport", List.of("L'opérateur BETWEEN attend 2 valeur(s)."));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(captor.capture());
        assertTrue(captor.getValue().contains("L'opérateur BETWEEN attend 2 valeur(s)."));
    }

    @Test
    void wrapsModelFailureInBotLlmException() {
        stubChain();
        when(callSpec.entity(BotReportPlan.class)).thenThrow(new IllegalStateException("json"));

        assertThrows(BotLlmException.class, () -> planner.plan("[]", "x", null));
    }

    @Test
    void timesOutWhenModelIsTooSlow() {
        stubChain();
        when(callSpec.entity(BotReportPlan.class)).thenAnswer(invocation -> {
            Thread.sleep(3000);
            return readyPlan();
        });

        assertThrows(BotLlmException.class, () -> planner.plan("[]", "x", null));
    }
}
```

- [ ] **Step 2 : exécuter, attendre l'échec de compilation**

Depuis `RHIS` : `mvn '-Dtest=BotReportPlannerTest' test` — attendu : échec, les classes
`BotReportPlan`, `BotLlmException` et `BotReportPlanner` n'existent pas encore.

- [ ] **Step 3 : implémentation minimale**

`RHIS/src/main/java/RHIS/com/RHIS/bot/dto/BotReportPlan.java` :

```java
package RHIS.com.RHIS.bot.dto;

import java.util.List;

public record BotReportPlan(
        String status,
        String question,
        String summary,
        Long rootDatasetId,
        List<Long> selectedFieldIds,
        List<PlanFilter> filters,
        List<PlanSort> sorts
) {
    public record PlanFilter(Long fieldId, String operator, List<String> values) {
    }

    public record PlanSort(Long fieldId, String direction) {
    }

    public boolean isReady() {
        return "READY".equalsIgnoreCase(status);
    }

    public boolean needsClarification() {
        return "NEEDS_CLARIFICATION".equalsIgnoreCase(status);
    }
}
```

`RHIS/src/main/java/RHIS/com/RHIS/bot/exception/BotLlmException.java` :

```java
package RHIS.com.RHIS.bot.exception;

public class BotLlmException extends RuntimeException {
    public BotLlmException(String message) {
        super(message);
    }

    public BotLlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

`RHIS/src/main/java/RHIS/com/RHIS/bot/BotReportPlanner.java` :

```java
package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.bot.config.BotAiProperties;
import RHIS.com.RHIS.bot.dto.BotReportPlan;
import RHIS.com.RHIS.bot.exception.BotLlmException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class BotReportPlanner {

    static final String SYSTEM_PROMPT = """
            Tu es l'assistant RHIS. À partir du catalogue JSON des datasets et de la demande
            de l'utilisateur, produis uniquement un objet JSON conforme au schéma fourni.
            Règles :
            - n'utilise que les datasetId et fieldId présents dans le catalogue ;
            - "operator" appartient exactement à la liste "operators" du champ choisi ;
            - "values" contient des chaînes au format du type du champ (DATE = yyyy-MM-dd,
              DATE_TIME = yyyy-MM-dd'T'HH:mm:ss, INTEGER = entier, DECIMAL = nombre,
              BOOLEAN = true/false) ;
            - BETWEEN attend exactement 2 valeurs, les autres opérateurs exactement 1 ;
            - "selectedFieldIds" ne doit jamais être vide ;
            - "sorts" référence uniquement des champs de "selectedFieldIds",
              "direction" vaut ASC ou DESC ;
            - si la demande est ambiguë, incomplète ou impossible avec ce catalogue :
              {"status":"NEEDS_CLARIFICATION","question":"<une seule question courte>"} ;
            - sinon {"status":"READY","summary":"<une phrase>", ...la définition...}.
            """;

    private final ChatClient.Builder chatClientBuilder;
    private final BotAiProperties properties;

    public BotReportPlan plan(String catalogJson, String message, List<String> previousErrors) {
        String userText = buildUserText(catalogJson, message, previousErrors);
        try {
            return CompletableFuture.supplyAsync(() -> callModel(userText))
                    .get(properties.getLlmTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            throw new BotLlmException("Le modèle n'a pas répondu dans le délai imparti.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BotLlmException("L'appel au modèle a été interrompu.");
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof BotLlmException botLlmException) {
                throw botLlmException;
            }
            throw new BotLlmException("L'appel au modèle a échoué.", exception.getCause());
        }
    }

    private BotReportPlan callModel(String userText) {
        ChatClient chatClient = chatClientBuilder.build();
        BotReportPlan plan = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userText)
                .call()
                .entity(BotReportPlan.class);
        if (plan == null) {
            throw new BotLlmException("La réponse du modèle est illisible.");
        }
        return plan;
    }

    private String buildUserText(String catalogJson, String message, List<String> previousErrors) {
        StringBuilder text = new StringBuilder();
        text.append("Date du jour : ").append(LocalDate.now()).append('\n');
        text.append("Catalogue JSON :\n").append(catalogJson).append('\n');
        if (previousErrors != null && !previousErrors.isEmpty()) {
            text.append("La proposition précédente a été rejetée avec ces erreurs :\n");
            previousErrors.forEach(error -> text.append("- ").append(error).append('\n'));
            text.append("Corrige la proposition en respectant strictement les règles.\n");
        }
        text.append("Demande de l'utilisateur : ").append(message);
        return text.toString();
    }
}
```

- [ ] **Step 4 : exécuter le test**

Depuis `RHIS` : `mvn '-Dtest=BotReportPlannerTest' test` — attendu : 4/4 PASS.
Si la compilation échoue sur les types `ChatClient` (API Spring AI 2.0.x), ouvrir
l'interface `ChatClient` dans le jar `spring-ai-client-chat` et ajuster les noms
exactes des interfaces imbriquées — ne jamais deviner.

- [ ] **Step 5 : commit du milestone**

Depuis la racine du dépôt :

```powershell
git add RHIS/src/main/java/RHIS/com/RHIS/bot/ RHIS/src/test/java/RHIS/com/RHIS/bot/
git commit -m "feat(bot): planneur LLM structured output avec timeout et erreurs typees"
```

### Task 4 : DTOs + BotReportService (orchestration + auto-correction)

**Files:**
- Create: `RHIS/src/main/java/RHIS/com/RHIS/bot/controller/dto/BotReportRequest.java`
- Create: `RHIS/src/main/java/RHIS/com/RHIS/bot/controller/dto/BotReportResponse.java`
- Create: `RHIS/src/main/java/RHIS/com/RHIS/bot/exception/BotRequestException.java`
- Create: `RHIS/src/main/java/RHIS/com/RHIS/bot/BotReportService.java`
- Test: `RHIS/src/test/java/RHIS/com/RHIS/bot/BotReportServiceTest.java`

**Interfaces:**
- Consumes: `ReportCatalogProvider.buildCatalog()` (Task 2), `BotReportPlanner.plan(...)`
  + `BotReportPlan` (Task 3), `ReportDefinitionResolver.resolve(ReportPreviewRequest)`
  (existant), `ReportGenerationService.create(UserEntity, UUID, ReportPreviewRequest)`
  → `ReportGenerationResponse` (existant, accès `generationId()`), `ObjectMapper`,
  `BotAiProperties` (Task 1), `ReportExportFormat` (XLSX|PDF), `SortDirection` (ASC|DESC).
- Produces: `BotReportService.generate(UserEntity owner, UUID idempotencyKey,
  BotReportRequest request)` → `BotReportResponse` (statuts READY / NEEDS_CLARIFICATION /
  FAILED) — consommé par Task 5. `BotReportRequest(String message, ReportExportFormat format)`.

- [ ] **Step 1 : écrire le test qui échoue**

```java
package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.auth.user.UserEntity;
import RHIS.com.RHIS.bot.catalog.ReportCatalogProvider;
import RHIS.com.RHIS.bot.controller.dto.BotReportRequest;
import RHIS.com.RHIS.bot.controller.dto.BotReportResponse;
import RHIS.com.RHIS.bot.dto.BotReportPlan;
import RHIS.com.RHIS.bot.exception.BotRequestException;
import RHIS.com.RHIS.report.controller.dto.ReportGenerationResponse;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewRequest;
import RHIS.com.RHIS.report.exception.ReportValidationException;
import RHIS.com.RHIS.report.model.ReportGenerationStatus;
import RHIS.com.RHIS.report.service.ReportDefinitionResolver;
import RHIS.com.RHIS.report.service.ReportGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotReportServiceTest {

    @Mock private ReportCatalogProvider catalogProvider;
    @Mock private BotReportPlanner planner;
    @Mock private ReportDefinitionResolver definitionResolver;
    @Mock private ReportGenerationService generationService;
    @Mock private UserEntity owner;

    private BotReportService service;

    @BeforeEach
    void setUp() {
        service = new BotReportService(catalogProvider, planner, definitionResolver,
                generationService, new ObjectMapper(), new BotAiProperties());
    }

    private BotReportPlan readyPlan() {
        return new BotReportPlan("READY", null, "Rapport employés",
                1L, List.of(10L), List.of(), List.of());
    }

    private ReportGenerationResponse generationResponse() {
        return new ReportGenerationResponse(UUID.randomUUID(), ReportGenerationStatus.PENDING,
                null, 0, null, null, null, null, null, List.of());
    }

    @Test
    void createsGenerationWhenPlanIsValid() {
        when(catalogProvider.buildCatalog()).thenReturn(List.of());
        when(planner.plan(anyString(), eq("liste des employés"), isNull()))
                .thenReturn(readyPlan());
        ReportGenerationResponse generation = generationResponse();
        when(generationService.create(any(), any(), any())).thenReturn(generation);

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("liste des employés", null));

        assertEquals("READY", response.status());
        assertEquals(generation.generationId(), response.generationId());
        assertEquals("XLSX", response.format().name());
        assertEquals("Rapport employés", response.planSummary());
    }

    @Test
    void returnsClarificationWithoutResolving() {
        when(catalogProvider.buildCatalog()).thenReturn(List.of());
        when(planner.plan(anyString(), anyString(), isNull())).thenReturn(
                new BotReportPlan("NEEDS_CLARIFICATION", "Quel restaurant ?",
                        null, null, null, null, null));

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("un rapport", null));

        assertEquals("NEEDS_CLARIFICATION", response.status());
        assertEquals("Quel restaurant ?", response.question());
        verifyNoInteractions(definitionResolver, generationService);
    }
```

*(suite du même fichier `BotReportServiceTest` — coller les tests suivants avant
l'accolade finale de la classe)*

```java
    @Test
    void retriesOnceThenSucceedsAfterValidationError() {
        when(catalogProvider.buildCatalog()).thenReturn(List.of());
        when(planner.plan(anyString(), anyString(), any())).thenReturn(readyPlan());
        when(definitionResolver.resolve(any(ReportPreviewRequest.class)))
                .thenThrow(new ReportValidationException("L'opérateur BETWEEN attend 2 valeur(s)."))
                .thenReturn(null);
        when(generationService.create(any(), any(), any())).thenReturn(generationResponse());

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("rapport", null));

        assertEquals("READY", response.status());
        verify(planner, times(2)).plan(anyString(), anyString(), any());
        verify(definitionResolver, times(2)).resolve(any(ReportPreviewRequest.class));
        verify(generationService, times(1)).create(any(), any(), any());
    }

    @Test
    void returnsFailedAfterSecondValidationError() {
        when(catalogProvider.buildCatalog()).thenReturn(List.of());
        when(planner.plan(anyString(), anyString(), any())).thenReturn(readyPlan());
        when(definitionResolver.resolve(any(ReportPreviewRequest.class)))
                .thenThrow(new ReportValidationException("Opérateur inconnu : FOO."));

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("rapport", null));

        assertEquals("FAILED", response.status());
        assertEquals(List.of("Opérateur inconnu : FOO."), response.errors());
        verifyNoInteractions(generationService);
        verify(planner, times(2)).plan(anyString(), anyString(), any());
    }

    @Test
    void rejectsMessageOverLimitBeforeCallingModel() {
        BotReportRequest request = new BotReportRequest("x".repeat(2001), null);

        assertThrows(BotRequestException.class,
                () -> service.generate(owner, UUID.randomUUID(), request));

        verifyNoInteractions(planner, catalogProvider);
    }
}
```

- [ ] **Step 2 : exécuter, attendre l'échec de compilation**

Depuis `RHIS` : `mvn '-Dtest=BotReportServiceTest' test` — attendu : échec, les classes
`BotReportRequest`, `BotReportResponse`, `BotRequestException` et `BotReportService`
n'existent pas encore.

- [ ] **Step 3 : implémentation minimale — DTOs et exception requête**

`RHIS/src/main/java/RHIS/com/RHIS/bot/controller/dto/BotReportRequest.java` :

```java
package RHIS.com.RHIS.bot.controller.dto;

import RHIS.com.RHIS.report.model.ReportExportFormat;
import jakarta.validation.constraints.NotBlank;

public record BotReportRequest(
        @NotBlank String message,
        ReportExportFormat format
) {
}
```

`RHIS/src/main/java/RHIS/com/RHIS/bot/controller/dto/BotReportResponse.java` :

```java
package RHIS.com.RHIS.bot.controller.dto;

import RHIS.com.RHIS.report.model.ReportExportFormat;

import java.util.List;
import java.util.UUID;

public record BotReportResponse(
        String status,
        String question,
        UUID generationId,
        ReportExportFormat format,
        String planSummary,
        List<String> errors
) {
    public static BotReportResponse ready(UUID generationId, ReportExportFormat format,
            String planSummary) {
        return new BotReportResponse("READY", null, generationId, format, planSummary, List.of());
    }

    public static BotReportResponse clarification(String question) {
        return new BotReportResponse("NEEDS_CLARIFICATION", question, null, null, null, List.of());
    }

    public static BotReportResponse failed(List<String> errors) {
        return new BotReportResponse("FAILED", null, null, null, null, List.copyOf(errors));
    }
}
```

`RHIS/src/main/java/RHIS/com/RHIS/bot/exception/BotRequestException.java` :

```java
package RHIS.com.RHIS.bot.exception;

public class BotRequestException extends RuntimeException {
    public BotRequestException(String message) {
        super(message);
    }
}
```

`RHIS/src/main/java/RHIS/com/RHIS/bot/BotReportService.java` *(partie 1 — coller les deux
blocs successivement dans le même fichier)* :

```java
package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.auth.user.UserEntity;
import RHIS.com.RHIS.bot.catalog.ReportCatalogProvider;
import RHIS.com.RHIS.bot.config.BotAiProperties;
import RHIS.com.RHIS.bot.controller.dto.BotReportRequest;
import RHIS.com.RHIS.bot.controller.dto.BotReportResponse;
import RHIS.com.RHIS.bot.dto.BotReportPlan;
import RHIS.com.RHIS.bot.exception.BotLlmException;
import RHIS.com.RHIS.bot.exception.BotRequestException;
import RHIS.com.RHIS.dataset.model.FilterOperator;
import RHIS.com.RHIS.report.controller.dto.ReportFilterRequest;
import RHIS.com.RHIS.report.controller.dto.ReportGenerationResponse;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewRequest;
import RHIS.com.RHIS.report.controller.dto.ReportSortRequest;
import RHIS.com.RHIS.report.controller.dto.SortDirection;
import RHIS.com.RHIS.report.exception.ReportDefinitionUnavailableException;
import RHIS.com.RHIS.report.exception.ReportValidationException;
import RHIS.com.RHIS.report.model.ReportExportFormat;
import RHIS.com.RHIS.report.service.ReportDefinitionResolver;
import RHIS.com.RHIS.report.service.ReportGenerationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BotReportService {

    private final ReportCatalogProvider catalogProvider;
    private final BotReportPlanner planner;
    private final ReportDefinitionResolver definitionResolver;
    private final ReportGenerationService generationService;
    private final ObjectMapper objectMapper;
    private final BotAiProperties properties;

    public BotReportResponse generate(UserEntity owner, UUID idempotencyKey,
            BotReportRequest request) {
        validateMessage(request);
        String catalogJson = catalogJson();
        BotReportPlan plan = planner.plan(catalogJson, request.message(), null);
        if (plan.needsClarification()) {
            return BotReportResponse.clarification(plan.question());
        }
        try {
            return createGeneration(owner, idempotencyKey, request, plan);
        } catch (ReportValidationException | ReportDefinitionUnavailableException firstAttempt) {
            BotReportPlan corrected = planner.plan(catalogJson, request.message(),
                    errorsOf(firstAttempt));
            if (corrected.needsClarification()) {
                return BotReportResponse.clarification(corrected.question());
            }
            try {
                return createGeneration(owner, idempotencyKey, request, corrected);
            } catch (ReportValidationException | ReportDefinitionUnavailableException secondAttempt) {
                return BotReportResponse.failed(errorsOf(secondAttempt));
            }
        }
    }

    private BotReportResponse createGeneration(UserEntity owner, UUID idempotencyKey,
            BotReportRequest request, BotReportPlan plan) {
        ReportExportFormat format = resolvedFormat(request);
        ReportPreviewRequest preview = toPreviewRequest(plan);
        definitionResolver.resolve(preview);
        ReportGenerationResponse generation =
                generationService.create(owner, idempotencyKey, preview);
        return BotReportResponse.ready(generation.generationId(), format, plan.summary());
    }

    private ReportExportFormat resolvedFormat(BotReportRequest request) {
        return request.format() == null ? ReportExportFormat.XLSX : request.format();
    }

    private ReportPreviewRequest toPreviewRequest(BotReportPlan plan) {
        if (!plan.isReady() || plan.rootDatasetId() == null
                || plan.selectedFieldIds() == null || plan.selectedFieldIds().isEmpty()) {
            throw new ReportValidationException(
                    "Le modèle n'a pas produit de définition de rapport exploitable.");
        }
        return new ReportPreviewRequest(
                plan.rootDatasetId(),
                plan.selectedFieldIds(),
                toFilters(plan.filters()),
                toSorts(plan.sorts()));
    }
```

*(partie 2 du même fichier — à coller avant l'accolade finale)*

```java
    private List<ReportFilterRequest> toFilters(List<BotReportPlan.PlanFilter> planFilters) {
        if (planFilters == null) {
            return List.of();
        }
        return planFilters.stream()
                .filter(Objects::nonNull)
                .map(filter -> new ReportFilterRequest(
                        filter.fieldId(),
                        parseOperator(filter.operator()),
                        filter.values() == null ? List.of() : filter.values()))
                .toList();
    }

    private FilterOperator parseOperator(String operator) {
        try {
            return FilterOperator.valueOf(operator);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ReportValidationException("Opérateur de filtre inconnu : " + operator);
        }
    }

    private List<ReportSortRequest> toSorts(List<BotReportPlan.PlanSort> planSorts) {
        if (planSorts == null) {
            return List.of();
        }
        return planSorts.stream()
                .filter(Objects::nonNull)
                .map(sort -> new ReportSortRequest(sort.fieldId(), parseDirection(sort.direction())))
                .toList();
    }

    private SortDirection parseDirection(String direction) {
        try {
            return SortDirection.valueOf(direction);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ReportValidationException("Direction de tri inconnue : " + direction);
        }
    }

    private List<String> errorsOf(Exception exception) {
        if (exception instanceof ReportDefinitionUnavailableException unavailable) {
            return unavailable.getUnavailableElements().stream()
                    .map(element -> element.kind() + " " + element.displayName()
                            + " : " + element.reason())
                    .toList();
        }
        return List.of(exception.getMessage());
    }

    private void validateMessage(BotReportRequest request) {
        if (request.message().length() > properties.getMaxMessageLength()) {
            throw new BotRequestException("La phrase ne peut pas dépasser "
                    + properties.getMaxMessageLength() + " caractères.");
        }
    }

    private String catalogJson() {
        try {
            return objectMapper.writeValueAsString(catalogProvider.buildCatalog());
        } catch (JsonProcessingException exception) {
            throw new BotLlmException("Le catalogue ne peut pas être sérialisé pour le modèle.");
        }
    }
}
```

- [ ] **Step 4 : exécuter le test**

Depuis `RHIS` : `mvn '-Dtest=BotReportServiceTest' test` — attendu : 5/5 PASS.

- [ ] **Step 5 : commit du milestone**

Depuis la racine du dépôt :

```powershell
git add RHIS/src/main/java/RHIS/com/RHIS/bot/ RHIS/src/test/java/RHIS/com/RHIS/bot/
git commit -m "feat(bot): orchestration phrase vers generation avec auto-correction unique"
```

### Task 5 : endpoint `/api/v1/bot/reports` + règle de sécurité

**Files:**
- Create: `RHIS/src/main/java/RHIS/com/RHIS/bot/BotReportController.java`
- Create: `RHIS/src/main/java/RHIS/com/RHIS/bot/BotExceptionHandler.java`
- Modify: `RHIS/src/main/java/RHIS/com/RHIS/auth/SecurityConfig.java` (1 règle)
- Test: `RHIS/src/test/java/RHIS/com/RHIS/bot/BotReportControllerSecurityTest.java`

**Interfaces:**
- Consumes: `BotReportService.generate(UserEntity, UUID, BotReportRequest)` (Task 4),
  `UserPrincipal.getUser()` → `UserEntity` (existant), `BotReportResponse` statuts
  READY / NEEDS_CLARIFICATION / FAILED (Task 4).
- Produces: `POST /api/v1/bot/reports` — `202` + `Location: /api/v1/report-generations/{id}`
  si READY, `200` si NEEDS_CLARIFICATION, `422` si FAILED, `400` requête invalide,
  `401` anonyme ; `Idempotency-Key` header optionnel (UUID serveur si absent) ;
  `BotExceptionHandler` → ProblemDetail 400 (`BotRequestException`) / 502 (`BotLlmException`).

- [ ] **Step 1 : écrire le test qui échoue**

```java
package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.auth.CustomUserDetailService;
import RHIS.com.RHIS.auth.JwtCookieFilter;
import RHIS.com.RHIS.auth.JwtService;
import RHIS.com.RHIS.auth.SecurityConfig;
import RHIS.com.RHIS.auth.UserPrincipal;
import RHIS.com.RHIS.auth.user.UserEntity;
import RHIS.com.RHIS.bot.controller.dto.BotReportRequest;
import RHIS.com.RHIS.bot.controller.dto.BotReportResponse;
import RHIS.com.RHIS.report.model.ReportExportFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.principal;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BotReportController.class)
@Import({SecurityConfig.class, JwtCookieFilter.class, BotExceptionHandler.class})
class BotReportControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BotReportService botReportService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    @Test
    void rejectsUnauthenticatedBotRequest() throws Exception {
        mockMvc.perform(post("/api/v1/bot/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void returnsAcceptedResponseForAuthenticatedRequest() throws Exception {
        when(botReportService.generate(any(), any(), any()))
                .thenReturn(BotReportResponse.ready(UUID.randomUUID(),
                        ReportExportFormat.XLSX, "Rapport des employés"));

        mockMvc.perform(post("/api/v1/bot/reports")
                        .with(principal(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.format").value("XLSX"));
    }

    @Test
    @WithMockUser
    void returnsUnprocessableEntityWhenPlanFailsValidation() throws Exception {
        when(botReportService.generate(any(), any(), any()))
                .thenReturn(BotReportResponse.failed(List.of("Opérateur inconnu : FOO.")));

        mockMvc.perform(post("/api/v1/bot/reports")
                        .with(principal(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errors[0]").value("Opérateur inconnu : FOO."));
    }

    @Test
    @WithMockUser
    void returnsBadRequestForBlankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/bot/reports")
                        .with(principal(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    private UserPrincipal userPrincipal() {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getUser()).thenReturn(mock(UserEntity.class));
        return principal;
    }

    private String validRequest() {
        return """
                { "message": "Liste des employés en Excel" }
                """;
    }
}
```

- [ ] **Step 2 : exécuter, attendre l'échec de compilation**

Depuis `RHIS` : `mvn '-Dtest=BotReportControllerSecurityTest' test` — attendu : échec,
`BotReportController` et `BotExceptionHandler` n'existent pas encore.

- [ ] **Step 3 : implémentation minimale**

`RHIS/src/main/java/RHIS/com/RHIS/bot/BotReportController.java` :

```java
package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.auth.UserPrincipal;
import RHIS.com.RHIS.bot.controller.dto.BotReportRequest;
import RHIS.com.RHIS.bot.controller.dto.BotReportResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bot")
@RequiredArgsConstructor
public class BotReportController {

    private final BotReportService botReportService;

    @PostMapping("/reports")
    public ResponseEntity<BotReportResponse> createReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody BotReportRequest request
    ) {
        UUID key = idempotencyKey == null ? UUID.randomUUID() : idempotencyKey;
        BotReportResponse response = botReportService.generate(principal.getUser(), key, request);
        if ("READY".equals(response.status())) {
            return ResponseEntity.accepted()
                    .location(URI.create("/api/v1/report-generations/" + response.generationId()))
                    .body(response);
        }
        if ("FAILED".equals(response.status())) {
            return ResponseEntity.unprocessableEntity().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
```

`RHIS/src/main/java/RHIS/com/RHIS/bot/BotExceptionHandler.java` :

```java
package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.bot.exception.BotLlmException;
import RHIS.com.RHIS.bot.exception.BotRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BotExceptionHandler {

    @ExceptionHandler(BotRequestException.class)
    public ProblemDetail handleBotRequest(BotRequestException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Requête du bot invalide");
        return problem;
    }

    @ExceptionHandler(BotLlmException.class)
    public ProblemDetail handleBotLlm(BotLlmException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, exception.getMessage());
        problem.setTitle("Le modèle de langage est indisponible");
        return problem;
    }
}
```

Dans `RHIS/src/main/java/RHIS/com/RHIS/auth/SecurityConfig.java`, insérer dans
`authorizeHttpRequests`, juste après le bloc `/api/v1/admin/**` :

```java
                        .requestMatchers("/api/v1/bot/**")
                        .authenticated()
```

- [ ] **Step 4 : exécuter le test**

Depuis `RHIS` : `mvn '-Dtest=BotReportControllerSecurityTest' test` — attendu : 4/4 PASS
(401 anonyme, 202 authentifié, 422 FAILED, 400 message vide).

- [ ] **Step 5 : commit du milestone**

Depuis la racine du dépôt :

```powershell
git add RHIS/src/main/java/RHIS/com/RHIS/bot/ RHIS/src/main/java/RHIS/com/RHIS/auth/SecurityConfig.java RHIS/src/test/java/RHIS/com/RHIS/bot/
git commit -m "feat(bot): endpoint /api/v1/bot/reports protege avec reponses structurees"
```

### Task 6 : validation intégrée, E2E manuel et documentation

**Files:**
- Create: `RHIS/docs/flows/07-bot-natural-language-report.md`
- Create: `RHIS/docs/superpowers/progress/2026-08-29-rhis-bot-natural-language-report.md`

**Interfaces:**
- Consumes: toutes les Tasks 1-5 ; endpoints existants de génération/export ; docker-compose racine.

- [ ] **Step 1 : suite des tests bot**

Depuis `RHIS` :

```powershell
mvn '-Dtest=BotAiPropertiesTest,ReportCatalogProviderTest,BotReportPlannerTest,BotReportServiceTest,BotReportControllerSecurityTest' test
```

Attendu : 16/16 PASS (2+2+4+5+4). Tout échec ici se corrige avant de continuer.

- [ ] **Step 2 : suite backend complète et comparaison aux baselines**

Depuis `RHIS` : `mvn test`. Attendu : exactement les échecs préexistants documentés
(`ReportExportWriterTest` 1 failure + 3 errors ; `ReportPreviewPostgresIntegrationTest`
4 errors de contexte — Testcontainers/Docker requis, sinon signifier les skips).
**Tout nouvel échec = régression du bot** : corriger avant de committer.

- [ ] **Step 3 : E2E manuel (prérequis : Docker + MISTRAL_API_KEY réelle)**

```powershell
# à la racine du dépôt
docker compose up -d
# depuis RHIS
mvn spring-boot:run
```

Puis, depuis un second terminal (vérifier au préalable les champs exacts de
`RHIS/src/main/java/RHIS/com/RHIS/auth/auth/dto/LoginRequest.java` pour le body) :

```powershell
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"<utilisateur existant>","password":"<mot de passe>"}'

curl -b cookies.txt -X POST http://localhost:8080/api/v1/bot/reports -H "Content-Type: application/json" -d "{\"message\":\"Liste des employés avec leur date d'embauche, triée par nom, en Excel\"}"
# attendu : 202 {"status":"READY","generationId":"...","format":"XLSX","planSummary":"..."}

curl -b cookies.txt http://localhost:8080/api/v1/report-generations/<generationId>
# attendu : status PENDING/RUNNING puis READY (polling)

curl -b cookies.txt -X POST http://localhost:8080/api/v1/report-generations/<generationId>/exports -H "Content-Type: application/json" -d '{"format":"XLSX"}'

curl -b cookies.txt -o rapport.xlsx http://localhost:8080/api/v1/report-exports/<exportId>/file
# attendu : fichier XLSX ouvert correctement
```

Cas à essayer aussi : phrase ambiguë (`"un rapport"` → 200 NEEDS_CLARIFICATION) ;
sans cookie → 401.

- [ ] **Step 4 : écrire `RHIS/docs/flows/07-bot-natural-language-report.md`**

Suivre la structure des flows existants (01-06) : Déclencheurs utilisateur, Chaîne
complète (du POST bot au téléchargement), Participants du flow, Cas d'erreur,
Diagramme de séquence mermaid, En langage métier, Points importants, Points
potentiellement confus. Y documenter : contenu exact envoyé au LLM (catalogue + phrase
+ date), max 2 appels, mapping des statuts HTTP, règle `authenticated()` ajoutée.

- [ ] **Step 5 : écrire la progression Ponytail**

`RHIS/docs/superpowers/progress/2026-08-29-rhis-bot-natural-language-report.md` :
État et décisions (HEAD, commit par milestone), Validation (tableau avant/après avec
les résultats réels des Steps 1-3), Changements livrés (liste exacte des fichiers),
Limites et anomalies hors périmètre, Prochaine action (phase 2 UI chat).

- [ ] **Step 6 : contrôle des empreintes préexistantes**

Ré-exécuter le script de fingerprints de Task 0 vers
`RHIS/target/ponytail-rhis-bot/after-dirty-files.json` et comparer à
`baseline-dirty-files.json`. Attendu : empreintes identiques pour chaque fichier
préexistant (les nouveaux fichiers du bot sont hors comparaison).

- [ ] **Step 7 : commit final de documentation**

Depuis la racine du dépôt :

```powershell
git diff --check
git add RHIS/docs/flows/07-bot-natural-language-report.md RHIS/docs/superpowers/progress/2026-08-29-rhis-bot-natural-language-report.md RHIS/docs/superpowers/plans/2026-08-29-rhis-bot-natural-language-report.md
git commit -m "docs(bot): flux 07 et progression ponytail rhis_bot"
```

## Rollback

- Chaque milestone = 1 commit → `git revert <sha>` du milestone fautif, ou retour au
  commit précédent ; ne jamais toucher aux fichiers préexistants hors du module `bot`.
- La fonctionnalité est purement additive : supprimer
  `RHIS/src/main/java/RHIS/com/RHIS/bot/**` (+ son test), les blocs `spring.ai`/`rhis.bot`
  du yaml, la règle `/api/v1/bot/**` du `SecurityConfig` et les 2 dépendances pom suffit
  à revenir à l'état antérieur.
- Aucun changement de schéma PostgreSQL → aucune migration à annuler.

## Completion Criteria

- `POST /api/v1/bot/reports` authentifié retourne `202` + `generationId` exploitable par
  les endpoints existants jusqu'au téléchargement (prouvé par l'E2E manuel).
- Phrase ambiguë → `200` `NEEDS_CLARIFICATION` sans génération (test + E2E).
- Auto-correction limitée à 1 passe (max 2 appels LLM), prouvée par `BotReportServiceTest`.
- Anonyme → `401` ; échec final → `422` ; message trop long/vide → `400`.
- Empreintes du travail préexistant identiques ; aucun fichier hors périmètre modifié.
- `mvn test` : aucun nouvel échec par rapport aux baselines documentées.
- Docs flows 07 + progression Ponytail à jour ; commits par milestone sur `rhis_bot`,
  aucun push ni merge.














