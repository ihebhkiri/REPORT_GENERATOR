package RHIS.com.RHIS.report;

import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.model.FilterOperator;
import RHIS.com.RHIS.dataset.repository.DataSetFieldRepository;
import RHIS.com.RHIS.dataset.repository.DataSetRepository;
import RHIS.com.RHIS.report.controller.dto.ReportFilterRequest;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewRequest;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewResponse;
import RHIS.com.RHIS.report.controller.dto.ReportSortRequest;
import RHIS.com.RHIS.report.controller.dto.SortDirection;
import RHIS.com.RHIS.report.service.ReportPreviewService;
import RHIS.com.RHIS.report.service.ReportGenerationService;
import RHIS.com.RHIS.report.service.ReportExportService;
import RHIS.com.RHIS.report.repository.ReportGenerationRepository;
import RHIS.com.RHIS.report.repository.ReportExportRepository;
import RHIS.com.RHIS.report.model.ReportGenerationStatus;
import RHIS.com.RHIS.report.model.ReportExportStatus;
import RHIS.com.RHIS.report.model.ReportExportFormat;
import RHIS.com.RHIS.report.exception.ReportResourceNotFoundException;
import RHIS.com.RHIS.auth.user.UserEntity;
import RHIS.com.RHIS.auth.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReportPreviewPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    private DataSetRepository dataSetRepository;

    @Autowired
    private DataSetFieldRepository dataSetFieldRepository;

    @Autowired
    private ReportPreviewService reportPreviewService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReportGenerationService generationService;

    @Autowired
    private ReportExportService exportService;

    @Autowired
    private ReportGenerationRepository generationRepository;

    @Autowired
    private ReportExportRepository exportRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void previewsDirectOutgoingJoinWithTypedFilterAndSort() {
        DataSetEntity shift = dataSetRepository.findBySourceName("rhis_shift").orElseThrow();
        DataSetEntity employee = dataSetRepository.findBySourceName("rhis_employee").orElseThrow();
        DataSetField date = field(shift, "date_journee");
        DataSetField employeeName = field(employee, "nom");
        DataSetField managerFlag = field(shift, "from_planning_manager");

        ReportPreviewResponse response = reportPreviewService.preview(new ReportPreviewRequest(
                shift.getId(),
                List.of(date.getId(), employeeName.getId()),
                List.of(new ReportFilterRequest(
                        managerFlag.getId(),
                        FilterOperator.EQUALS,
                        List.of("true")
                )),
                List.of(new ReportSortRequest(date.getId(), SortDirection.ASC))
        ));

        assertThat(response.columns()).hasSize(2);
        assertThat(response.rows()).isNotEmpty().hasSizeLessThanOrEqualTo(50);
        assertThat(response.rows())
                .allSatisfy(row -> assertThat(row)
                        .containsKeys("field_" + date.getId(), "field_" + employeeName.getId()));
    }

    @Test
    void treatsSqlInjectionPayloadAsFilterData() {
        DataSetEntity employee = dataSetRepository.findBySourceName("rhis_employee").orElseThrow();
        DataSetField name = field(employee, "nom");

        ReportPreviewResponse response = reportPreviewService.preview(new ReportPreviewRequest(
                employee.getId(),
                List.of(name.getId()),
                List.of(new ReportFilterRequest(
                        name.getId(),
                        FilterOperator.CONTAINS,
                        List.of("' OR 1=1 --")
                )),
                List.of()
        ));

        assertThat(response.rows()).isEmpty();
    }

    @Test
    void limitsPreviewToSixRowsAndReportsMoreData() {
        jdbcTemplate.update(
                """
                        INSERT INTO rhis_restaurant (
                            uuid, libelle, matricule, adresse, code_pointeuse, periode_restaurant
                        ) VALUES (?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                "Restaurant RHIS 51",
                "RST-051",
                "51 rue du Test",
                "PNT-051",
                "HEBDOMADAIRE"
        );

        DataSetEntity restaurant = dataSetRepository.findBySourceName("rhis_restaurant").orElseThrow();
        DataSetField label = field(restaurant, "libelle");

        ReportPreviewResponse response = reportPreviewService.preview(new ReportPreviewRequest(
                restaurant.getId(),
                List.of(label.getId()),
                List.of(),
                List.of()
        ));

        assertThat(response.rows()).hasSize(6);
        assertThat(response.returnedRowCount()).isEqualTo(6);
        assertThat(response.hasMore()).isTrue();
    }

    @Test
    void generatesOneSnapshotAndBothOwnerScopedExports() throws Exception {
        DataSetEntity restaurant = dataSetRepository.findBySourceName("rhis_restaurant").orElseThrow();
        DataSetField label = field(restaurant, "libelle");
        UserEntity owner = user("report-owner@rhis.test");
        UserEntity other = user("other-owner@rhis.test");
        ReportPreviewRequest request = new ReportPreviewRequest(
                restaurant.getId(),
                List.of(label.getId()),
                List.of(),
                List.of(new ReportSortRequest(label.getId(), SortDirection.ASC))
        );
        UUID idempotencyKey = UUID.randomUUID();

        var created = generationService.create(owner, idempotencyKey, request);
        var replayed = generationService.create(owner, idempotencyKey, request);

        assertThat(replayed.generationId()).isEqualTo(created.generationId());
        var generation = awaitGeneration(created.generationId());
        assertThat(generation.getStatus()).isEqualTo(ReportGenerationStatus.READY);
        assertThat(generation.getTotalRowCount()).isGreaterThanOrEqualTo(50);
        assertThatThrownBy(() -> generationService.get(other.getId(), created.generationId()))
                .isInstanceOf(ReportResourceNotFoundException.class);
        assertThatThrownBy(() -> exportService.create(
                other.getId(),
                created.generationId(),
                ReportExportFormat.PDF
        )).isInstanceOf(ReportResourceNotFoundException.class);
        assertThatThrownBy(() -> generationService.delete(other.getId(), created.generationId()))
                .isInstanceOf(ReportResourceNotFoundException.class);

        var pdf = exportService.create(owner.getId(), created.generationId(), ReportExportFormat.PDF);
        var xlsx = exportService.create(owner.getId(), created.generationId(), ReportExportFormat.XLSX);
        awaitExport(pdf.exportId());
        awaitExport(xlsx.exportId());

        try (var pdfInput = exportService.openDownload(owner.getId(), pdf.exportId()).input();
             var xlsxInput = exportService.openDownload(owner.getId(), xlsx.exportId()).input()) {
            assertThat(pdfInput.readAllBytes()).isNotEmpty();
            assertThat(xlsxInput.readAllBytes()).isNotEmpty();
        }
        assertThatThrownBy(() -> exportService.get(other.getId(), pdf.exportId()))
                .isInstanceOf(ReportResourceNotFoundException.class);
        assertThatThrownBy(() -> exportService.openDownload(other.getId(), pdf.exportId()))
                .isInstanceOf(ReportResourceNotFoundException.class);

        generationService.delete(owner.getId(), created.generationId());
        assertThat(generationRepository.findById(created.generationId())).isEmpty();
    }

    private UserEntity user(String email) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword("test-password");
        return userRepository.saveAndFlush(user);
    }

    private RHIS.com.RHIS.report.entity.ReportGenerationEntity awaitGeneration(UUID id) throws Exception {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(20).toNanos();
        while (System.nanoTime() < deadline) {
            var generation = generationRepository.findById(id).orElseThrow();
            if (generation.getStatus() == ReportGenerationStatus.READY
                    || generation.getStatus() == ReportGenerationStatus.FAILED) {
                return generation;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Generation did not reach a terminal state");
    }

    private void awaitExport(UUID id) throws Exception {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(20).toNanos();
        while (System.nanoTime() < deadline) {
            var export = exportRepository.findById(id).orElseThrow();
            if (export.getStatus() == ReportExportStatus.READY) {
                return;
            }
            if (export.getStatus() == ReportExportStatus.FAILED) {
                throw new AssertionError("Export failed: " + export.getErrorCode());
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Export did not reach READY");
    }

    private DataSetField field(DataSetEntity dataSet, String sourceName) {
        return dataSetFieldRepository
                .findByDataset_IdAndActiveTrueOrderByPositionAsc(dataSet.getId())
                .stream()
                .filter(field -> sourceName.equals(field.getSourceName()))
                .findFirst()
                .orElseThrow();
    }
}
