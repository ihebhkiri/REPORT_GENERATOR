package RHIS.com.RHIS.dataset;

import RHIS.com.RHIS.dataset.bootstrap.DataSetInitializer;
import RHIS.com.RHIS.dataset.bootstrap.CatalogMetadataSeeder;
import RHIS.com.RHIS.dataset.controller.dto.UpdateDataSetExposureRequest;
import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.repository.DataSetRepository;
import RHIS.com.RHIS.dataset.repository.DataSetFieldRepository;
import RHIS.com.RHIS.dataset.service.DataSetAdministrationService;
import RHIS.com.RHIS.bot.catalog.ReportCatalogProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "rhis.report.jobs.max-pool-size=1")
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogMetadataPostgresIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    @Autowired private DataSetRepository dataSetRepository;
    @Autowired private DataSetFieldRepository dataSetFieldRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private CatalogMetadataSeeder metadataSeeder;

    @Autowired
    private DataSetAdministrationService administrationService;

    @Autowired
    private DataSetInitializer initializer;

    @Autowired
    private ReportCatalogProvider catalogProvider;

    @Test
    void preservesBusinessMetadataAfterSavingReloadingAndSynchronizing() {
        DataSetEntity employee = dataSetRepository.findBySourceName("rhis_employee").orElseThrow();
        DataSetField name = field(employee, "nom");
        try {
            administrationService.updateConfiguration(new UpdateDataSetExposureRequest(List.of(
                    new UpdateDataSetExposureRequest.DataSetUpdate(employee.getId(), true, employee.isDisplayRelated(),
                            List.of(new UpdateDataSetExposureRequest.FieldUpdate(name.getId(), true,
                                    "Nom de famille", "Patronyme\nNOM FAMILIAL")),
                            "Personnel du restaurant", "Salariés\nPersonnel\nsalariés"))));
            initializer.run(null);
            assertThat(dataSetRepository.findById(employee.getId()).orElseThrow().getAliases())
                    .isEqualTo("Salariés\nPersonnel");
            assertThat(dataSetFieldRepository.findById(name.getId()).orElseThrow().getDescription())
                    .isEqualTo("Nom de famille");
            var catalogEmployee = catalogProvider.buildCatalog().rootDatasets().stream()
                    .filter(dataset -> dataset.datasetId().equals(employee.getId())).findFirst().orElseThrow();
            assertThat(catalogEmployee.aliases()).containsExactly("Salariés", "Personnel");
            assertThat(catalogEmployee.fields().stream().filter(f -> f.fieldId().equals(name.getId()))
                    .findFirst().orElseThrow().aliases()).containsExactly("Patronyme", "NOM FAMILIAL");
            administrationService.updateConfiguration(new UpdateDataSetExposureRequest(List.of(
                    new UpdateDataSetExposureRequest.DataSetUpdate(employee.getId(), true, employee.isDisplayRelated(),
                            List.of(new UpdateDataSetExposureRequest.FieldUpdate(name.getId(), false, null, null)), null, null))));
            assertThat(catalogProvider.buildCatalog().rootDatasets().stream()
                    .flatMap(dataset -> dataset.fields().stream()).map(f -> f.fieldId()))
                    .doesNotContain(name.getId());
        } finally {
            administrationService.updateConfiguration(new UpdateDataSetExposureRequest(List.of(
                    new UpdateDataSetExposureRequest.DataSetUpdate(employee.getId(), employee.isDisplayMain(), employee.isDisplayRelated(),
                            List.of(new UpdateDataSetExposureRequest.FieldUpdate(name.getId(), name.isVisible(),
                                    name.getDescription(), name.getAliases())), employee.getDescription(), employee.getAliases()))));
        }
    }

    @Test
    void migratesExistingMetadataWithoutLosingDataAndCanRunTwice() throws Exception {
        jdbcTemplate.execute("CREATE SCHEMA catalog_metadata_migration");
        jdbcTemplate.execute("CREATE TABLE catalog_metadata_migration.datasets (id bigint PRIMARY KEY, display_name text)");
        jdbcTemplate.execute("CREATE TABLE catalog_metadata_migration.dataset_fields (id bigint PRIMARY KEY, display_name text)");
        jdbcTemplate.update("INSERT INTO catalog_metadata_migration.datasets VALUES (1, 'Employés')");
        String migration = java.nio.file.Files.readString(java.nio.file.Path.of(
                "docs/migrations/2026-09-08-catalogue-metier.sql")).replace("public.", "catalog_metadata_migration.");
        jdbcTemplate.execute(migration);
        jdbcTemplate.execute(migration);
        assertThat(jdbcTemplate.queryForMap("SELECT display_name, description, aliases FROM catalog_metadata_migration.datasets WHERE id = 1"))
                .containsEntry("display_name", "Employés").containsEntry("description", "").containsEntry("aliases", "");
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void seedsEveryTableAndFieldAtStartupAndPreservesCustomValuesOnRerun() {
        var datasets = dataSetRepository.findAll();
        assertThat(datasets).hasSize(7).allSatisfy(dataset -> {
            assertThat(dataset.getDescription()).isNotBlank().doesNotStartWith("Informations relatives à");
            assertThat(dataset.getAliases()).isNotBlank();
        });
        var fields = dataSetFieldRepository.findAll();
        assertThat(fields).isNotEmpty().allSatisfy(field -> {
            assertThat(field.getDescription()).isNotBlank().doesNotStartWith("Information «");
            assertThat(field.getAliases()).isNotBlank();
        });
        DataSetEntity employee = dataSetRepository.findBySourceName("rhis_employee").orElseThrow();
        DataSetField name = field(employee, "nom");
        jdbcTemplate.update("UPDATE public.datasets SET description = ?, aliases = ' ' WHERE id = ?",
                "Description personnalisée", employee.getId());
        jdbcTemplate.update("UPDATE public.dataset_fields SET description = NULL, aliases = ?, visible = false WHERE id = ?",
                "Nom personnalisé", name.getId());
        metadataSeeder.run(null);
        assertThat(jdbcTemplate.queryForMap("SELECT description, aliases FROM public.datasets WHERE id = ?", employee.getId()))
                .containsEntry("description", "Description personnalisée")
                .containsEntry("aliases", "Employés\nSalariés\nPersonnel\nÉquipe");
        assertThat(jdbcTemplate.queryForMap("SELECT description, aliases, visible FROM public.dataset_fields WHERE id = ?", name.getId()))
                .containsEntry("description", "Nom de famille du salarié.")
                .containsEntry("aliases", "Nom personnalisé").containsEntry("visible", false);
        var before = jdbcTemplate.queryForList("SELECT * FROM public.dataset_fields ORDER BY id");
        metadataSeeder.run(null);
        assertThat(jdbcTemplate.queryForList("SELECT * FROM public.dataset_fields ORDER BY id")).isEqualTo(before);
    }

    private DataSetField field(DataSetEntity dataset, String name) {
        return dataSetFieldRepository.findByDataset_IdOrderByPositionAsc(dataset.getId()).stream()
                .filter(field -> field.getSourceName().equals(name)).findFirst().orElseThrow();
    }
}
