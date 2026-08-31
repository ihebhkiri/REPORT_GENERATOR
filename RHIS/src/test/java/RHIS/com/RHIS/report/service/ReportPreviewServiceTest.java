package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.dataset.controller.dto.TableRelationProjection;
import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.model.DataSetFieldType;
import RHIS.com.RHIS.dataset.model.FilterOperator;
import RHIS.com.RHIS.dataset.repository.DataSetFieldRepository;
import RHIS.com.RHIS.dataset.repository.DataSetRepository;
import RHIS.com.RHIS.report.controller.dto.ReportFilterRequest;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewRequest;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewResponse;
import RHIS.com.RHIS.report.controller.dto.ReportSortRequest;
import RHIS.com.RHIS.report.controller.dto.SortDirection;
import RHIS.com.RHIS.report.exception.ReportValidationException;
import RHIS.com.RHIS.report.exception.ReportResourceNotFoundException;
import RHIS.com.RHIS.report.exception.ReportDefinitionUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportPreviewServiceTest {

    @Mock
    private DataSetRepository dataSetRepository;
    @Mock
    private DataSetFieldRepository dataSetFieldRepository;
    @Mock
    private ReportSqlBuilder reportSqlBuilder;
    @Mock
    private ReportPreviewExecutor reportPreviewExecutor;

    private ReportPreviewService service;

    @BeforeEach
    void setUp() {
        ReportDefinitionResolver definitionResolver = new ReportDefinitionResolver(
                dataSetRepository,
                dataSetFieldRepository
        );
        service = new ReportPreviewService(
                definitionResolver,
                reportSqlBuilder,
                reportPreviewExecutor
        );
    }

    @Test
    void rejectsUnavailableRootDatasetBeforeOtherValidation() {
        when(dataSetRepository.findById(1L)).thenReturn(Optional.empty());

        ReportPreviewRequest request = new ReportPreviewRequest(
                1L,
                List.of(11L, 11L),
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> service.preview(request))
                .isInstanceOf(ReportResourceNotFoundException.class)
                .hasMessageContaining("introuvable");
        verifyNoInteractions(dataSetFieldRepository, reportSqlBuilder, reportPreviewExecutor);
    }

    @Test
    void rejectsDuplicateSelectedFieldsBeforeLoadingFields() {
        DataSetEntity root = dataSet(1L, "Rhis Shift", "rhis_shift");
        when(dataSetRepository.findById(1L)).thenReturn(Optional.of(root));

        ReportPreviewRequest request = new ReportPreviewRequest(
                1L,
                List.of(11L, 11L),
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> service.preview(request))
                .isInstanceOf(ReportValidationException.class)
                .hasMessageContaining("sélectionnée qu'une fois");
        verifyNoInteractions(dataSetFieldRepository, reportSqlBuilder, reportPreviewExecutor);
    }

    @Test
    void rejectsDuplicateSortFields() {
        DataSetEntity root = dataSet(1L, "Rhis Shift", "rhis_shift");
        DataSetField selected = field(11L, "Date", "date_journee", DataSetFieldType.DATE, root);

        when(dataSetRepository.findById(1L)).thenReturn(Optional.of(root));
        when(dataSetFieldRepository.findByIdIn(any())).thenReturn(List.of(selected));

        ReportPreviewRequest request = new ReportPreviewRequest(
                1L,
                List.of(11L),
                List.of(),
                List.of(
                        new ReportSortRequest(11L, SortDirection.ASC),
                        new ReportSortRequest(11L, SortDirection.DESC)
                )
        );

        assertThatThrownBy(() -> service.preview(request))
                .isInstanceOf(ReportValidationException.class)
                .hasMessageContaining("une fois dans le tri");
        verifyNoInteractions(reportSqlBuilder, reportPreviewExecutor);
    }

    @Test
    void preservesDefinitionOrdering() {
        DataSetEntity root = dataSet(1L, "Rhis Shift", "rhis_shift");
        DataSetEntity lowerTarget = dataSet(2L, "Rhis Employee", "rhis_employee");
        DataSetEntity higherTarget = dataSet(3L, "Rhis Contract", "rhis_contract");
        DataSetField rootField = field(11L, "Date", "date_journee", DataSetFieldType.DATE, root);
        DataSetField lowerTargetField = field(21L, "Employee", "nom", DataSetFieldType.TEXT, lowerTarget);
        DataSetField higherTargetField = field(31L, "Contract", "type", DataSetFieldType.TEXT, higherTarget);
        DataSetField secondPrimaryKey = field(13L, "Second key", "pk_b", DataSetFieldType.INTEGER, root);
        DataSetField firstPrimaryKey = field(12L, "First key", "pk_a", DataSetFieldType.INTEGER, root);
        secondPrimaryKey.setPrimaryKey(true);
        firstPrimaryKey.setPrimaryKey(true);
        PreparedReportQuery preparedQuery = new PreparedReportQuery("SELECT 1", List.of(), List.of());

        when(dataSetRepository.findById(1L)).thenReturn(Optional.of(root));
        when(dataSetFieldRepository.findByIdIn(any()))
                .thenReturn(List.of(lowerTargetField, rootField, higherTargetField));
        when(dataSetRepository.findVisibleTableRelations()).thenReturn(List.of(
                relation("fk_lower", 2, 1L, 2L, "lower_fk_b", "lower_pk_b"),
                relation("fk_higher", 1, 1L, 3L, "higher_fk", "higher_pk"),
                relation("fk_lower", 1, 1L, 2L, "lower_fk_a", "lower_pk_a")
        ));
        when(dataSetFieldRepository.findByDataset_IdAndActiveTrueOrderByPositionAsc(1L))
                .thenReturn(List.of(secondPrimaryKey, firstPrimaryKey));
        when(reportSqlBuilder.buildPreview(any())).thenReturn(preparedQuery);
        when(reportPreviewExecutor.execute(preparedQuery))
                .thenReturn(new ReportPreviewResponse(List.of(), List.of(), false, 0));

        service.preview(new ReportPreviewRequest(
                1L,
                List.of(31L, 11L, 21L),
                List.of(
                        new ReportFilterRequest(31L, FilterOperator.EQUALS, List.of("CDD")),
                        new ReportFilterRequest(21L, FilterOperator.EQUALS, List.of("Durand"))
                ),
                List.of(
                        new ReportSortRequest(21L, SortDirection.DESC),
                        new ReportSortRequest(11L, SortDirection.ASC)
                )
        ));

        ArgumentCaptor<ResolvedReportDefinition> captor =
                ArgumentCaptor.forClass(ResolvedReportDefinition.class);
        verify(reportSqlBuilder).buildPreview(captor.capture());
        ResolvedReportDefinition definition = captor.getValue();

        assertThat(definition.selectedFields())
                .extracting(DataSetField::getId)
                .containsExactly(31L, 11L, 21L);
        assertThat(definition.filters())
                .extracting(filter -> filter.field().getId())
                .containsExactly(31L, 21L);
        assertThat(definition.sorts())
                .extracting(sort -> sort.field().getId())
                .containsExactly(21L, 11L);
        assertThat(definition.joins())
                .extracting(join -> join.targetDataset().getId())
                .containsExactly(2L, 3L);
        assertThat(definition.joins().get(0).columns())
                .extracting(ResolvedJoinColumn::sourceColumn)
                .containsExactly("lower_fk_a", "lower_fk_b");
        assertThat(definition.rootPrimaryKeyFields())
                .extracting(DataSetField::getId)
                .containsExactly(13L, 12L);
    }

    @Test
    void acceptsNonSelectedFilterOnDirectOutgoingDataset() {
        DataSetEntity root = dataSet(1L, "Rhis Shift", "rhis_shift");
        DataSetEntity employee = dataSet(2L, "Rhis Employee", "rhis_employee");
        employee.setDisplayMain(false);
        employee.setDisplayRelated(true);
        DataSetField date = field(11L, "Date", "date_journee", DataSetFieldType.DATE, root);
        DataSetField name = field(21L, "Nom", "nom", DataSetFieldType.TEXT, employee);
        TableRelationProjection relation = relation(1L, 2L);
        PreparedReportQuery preparedQuery = new PreparedReportQuery("SELECT 1", List.of(), List.of());
        ReportPreviewResponse expected = new ReportPreviewResponse(List.of(), List.of(), false, 0);

        when(dataSetRepository.findById(1L)).thenReturn(Optional.of(root));
        when(dataSetFieldRepository.findByIdIn(any())).thenReturn(List.of(date, name));
        when(dataSetFieldRepository.findByDataset_IdAndActiveTrueOrderByPositionAsc(1L))
                .thenReturn(List.of(date));
        when(dataSetRepository.findVisibleTableRelations()).thenReturn(List.of(relation));
        when(reportSqlBuilder.buildPreview(any())).thenReturn(preparedQuery);
        when(reportPreviewExecutor.execute(preparedQuery)).thenReturn(expected);

        ReportPreviewResponse actual = service.preview(new ReportPreviewRequest(
                1L,
                List.of(11L),
                List.of(new ReportFilterRequest(21L, FilterOperator.CONTAINS, List.of("Durand"))),
                List.of()
        ));

        assertThat(actual).isSameAs(expected);
        ArgumentCaptor<ResolvedReportDefinition> captor =
                ArgumentCaptor.forClass(ResolvedReportDefinition.class);
        verify(reportSqlBuilder).buildPreview(captor.capture());
        assertThat(captor.getValue().joins()).hasSize(1);
        assertThat(captor.getValue().filters()).hasSize(1);
    }

    @Test
    void rejectsSortOnNonSelectedField() {
        DataSetEntity root = dataSet(1L, "Rhis Shift", "rhis_shift");
        DataSetField selected = field(11L, "Date", "date_journee", DataSetFieldType.DATE, root);
        DataSetField hiddenSort = field(12L, "Heure", "heure_debut", DataSetFieldType.TIME, root);

        when(dataSetRepository.findById(1L)).thenReturn(Optional.of(root));
        when(dataSetFieldRepository.findByIdIn(any())).thenReturn(List.of(selected, hiddenSort));

        ReportPreviewRequest request = new ReportPreviewRequest(
                1L,
                List.of(11L),
                List.of(),
                List.of(new ReportSortRequest(12L, SortDirection.ASC))
        );

        assertThatThrownBy(() -> service.preview(request))
                .isInstanceOf(ReportValidationException.class)
                .hasMessageContaining("colonnes sélectionnées");
    }

    @Test
    void acceptsRelationInForeignKeyReverseDirection() {
        DataSetEntity root = dataSet(1L, "Rhis Employee", "rhis_employee");
        DataSetEntity shift = dataSet(2L, "Rhis Shift", "rhis_shift");
        DataSetField employeeName = field(11L, "Nom", "nom", DataSetFieldType.TEXT, root);
        DataSetField shiftDate = field(21L, "Date", "date_journee", DataSetFieldType.DATE, shift);
        TableRelationProjection incoming = relation(2L, 1L);
        PreparedReportQuery preparedQuery = new PreparedReportQuery("SELECT 1", List.of(), List.of());

        when(dataSetRepository.findById(1L)).thenReturn(Optional.of(root));
        when(dataSetFieldRepository.findByIdIn(any())).thenReturn(List.of(employeeName, shiftDate));
        when(dataSetRepository.findVisibleTableRelations()).thenReturn(List.of(incoming));
        when(dataSetFieldRepository.findByDataset_IdAndActiveTrueOrderByPositionAsc(1L))
                .thenReturn(List.of());
        when(reportSqlBuilder.buildPreview(any())).thenReturn(preparedQuery);
        when(reportPreviewExecutor.execute(preparedQuery))
                .thenReturn(new ReportPreviewResponse(List.of(), List.of(), false, 0));

        ReportPreviewRequest request = new ReportPreviewRequest(
                1L,
                List.of(11L, 21L),
                List.of(),
                List.of()
        );

        service.preview(request);

        ArgumentCaptor<ResolvedReportDefinition> captor =
                ArgumentCaptor.forClass(ResolvedReportDefinition.class);
        verify(reportSqlBuilder).buildPreview(captor.capture());
        ResolvedJoin join = captor.getValue().joins().get(0);
        assertThat(join.sourceDataset().getId()).isEqualTo(1L);
        assertThat(join.targetDataset().getId()).isEqualTo(2L);
        assertThat(join.columns().get(0).sourceColumn()).isEqualTo("emp_pk_id");
        assertThat(join.columns().get(0).targetColumn()).isEqualTo("employee_fk_id");
    }

    @Test
    void acceptsIndirectRelationThroughAuthorizedIntermediate() {
        DataSetEntity root = dataSet(1L, "Rhis Shift", "rhis_shift");
        DataSetEntity intermediate = dataSet(2L, "Rhis Employee", "rhis_employee");
        DataSetEntity target = dataSet(3L, "Rhis Contrat", "rhis_contrat");
        DataSetField rootDate = field(11L, "Date", "date_journee", DataSetFieldType.DATE, root);
        DataSetField contractType = field(31L, "Type", "type_contrat", DataSetFieldType.TEXT, target);

        when(dataSetRepository.findById(1L)).thenReturn(Optional.of(root));
        when(dataSetFieldRepository.findByIdIn(any())).thenReturn(List.of(rootDate, contractType));
        when(dataSetRepository.findVisibleTableRelations()).thenReturn(List.of(
                relation("fk_shift_employee", 1L, 2L),
                relation("fk_employee_contract", 2L, 3L)
        ));
        when(dataSetRepository.findAllById(any())).thenReturn(List.of(intermediate));
        when(dataSetFieldRepository.findByDataset_IdAndActiveTrueOrderByPositionAsc(1L))
                .thenReturn(List.of());
        PreparedReportQuery preparedQuery = new PreparedReportQuery("SELECT 1", List.of(), List.of());
        when(reportSqlBuilder.buildPreview(any())).thenReturn(preparedQuery);
        when(reportPreviewExecutor.execute(preparedQuery))
                .thenReturn(new ReportPreviewResponse(List.of(), List.of(), false, 0));

        ReportPreviewRequest request = new ReportPreviewRequest(
                1L,
                List.of(11L, 31L),
                List.of(),
                List.of()
        );

        service.preview(request);

        ArgumentCaptor<ResolvedReportDefinition> captor =
                ArgumentCaptor.forClass(ResolvedReportDefinition.class);
        verify(reportSqlBuilder).buildPreview(captor.capture());
        assertThat(captor.getValue().joins())
                .extracting(join -> join.targetDataset().getId())
                .containsExactly(2L, 3L);
    }

    @Test
    void rejectsAmbiguousDirectRelations() {
        DataSetEntity root = dataSet(1L, "Rhis Shift", "rhis_shift");
        DataSetEntity employee = dataSet(2L, "Rhis Employee", "rhis_employee");
        DataSetField shiftDate = field(11L, "Date", "date_journee", DataSetFieldType.DATE, root);
        DataSetField employeeName = field(21L, "Nom", "nom", DataSetFieldType.TEXT, employee);

        when(dataSetRepository.findById(1L)).thenReturn(Optional.of(root));
        when(dataSetFieldRepository.findByIdIn(any())).thenReturn(List.of(shiftDate, employeeName));
        when(dataSetRepository.findVisibleTableRelations()).thenReturn(List.of(
                relation("fk_employee_primary", 1L, 2L),
                relation("fk_employee_secondary", 1L, 2L)
        ));

        ReportPreviewRequest request = new ReportPreviewRequest(
                1L,
                List.of(11L, 21L),
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> service.preview(request))
                .isInstanceOf(ReportValidationException.class)
                .hasMessageContaining("Plusieurs chemins");
    }

    @Test
    void rejectsInvalidBooleanValue() {
        DataSetEntity root = dataSet(1L, "Rhis Shift", "rhis_shift");
        DataSetField flag = field(
                11L,
                "Planning manager",
                "from_planning_manager",
                DataSetFieldType.BOOLEAN,
                root
        );

        when(dataSetRepository.findById(1L)).thenReturn(Optional.of(root));
        when(dataSetFieldRepository.findByIdIn(any())).thenReturn(List.of(flag));

        ReportPreviewRequest request = new ReportPreviewRequest(
                1L,
                List.of(11L),
                List.of(new ReportFilterRequest(11L, FilterOperator.EQUALS, List.of("yes"))),
                List.of()
        );

        assertThatThrownBy(() -> service.preview(request))
                .isInstanceOf(ReportValidationException.class)
                .hasMessageContaining("true ou false");
    }

    @Test
    void rejectsOperatorIncompatibleWithFieldType() {
        DataSetEntity root = dataSet(1L, "Rhis Shift", "rhis_shift");
        DataSetField flag = field(11L, "Actif", "actif", DataSetFieldType.BOOLEAN, root);

        when(dataSetRepository.findById(1L)).thenReturn(Optional.of(root));
        when(dataSetFieldRepository.findByIdIn(any())).thenReturn(List.of(flag));

        ReportPreviewRequest request = new ReportPreviewRequest(
                1L,
                List.of(11L),
                List.of(new ReportFilterRequest(11L, FilterOperator.CONTAINS, List.of("true"))),
                List.of()
        );

        assertThatThrownBy(() -> service.preview(request))
                .isInstanceOf(ReportValidationException.class)
                .hasMessageContaining("n'est pas compatible");
    }

    @Test
    void rejectsInvalidOperatorArity() {
        DataSetEntity root = dataSet(1L, "Rhis Shift", "rhis_shift");
        DataSetField date = field(11L, "Date", "date_journee", DataSetFieldType.DATE, root);

        when(dataSetRepository.findById(1L)).thenReturn(Optional.of(root));
        when(dataSetFieldRepository.findByIdIn(any())).thenReturn(List.of(date));

        ReportPreviewRequest request = new ReportPreviewRequest(
                1L,
                List.of(11L),
                List.of(new ReportFilterRequest(11L, FilterOperator.BETWEEN, List.of("2026-08-09"))),
                List.of()
        );

        assertThatThrownBy(() -> service.preview(request))
                .isInstanceOf(ReportValidationException.class)
                .hasMessageContaining("attend 2 valeur");
    }

    @Test
    void rejectsInvisibleFieldAsNotFound() {
        DataSetEntity root = dataSet(1L, "Rhis Shift", "rhis_shift");
        DataSetField invisible = field(
                11L,
                "Champ masqué",
                "hidden_field",
                DataSetFieldType.TEXT,
                root
        );
        invisible.setVisible(false);

        when(dataSetRepository.findById(1L)).thenReturn(Optional.of(root));
        when(dataSetFieldRepository.findByIdIn(any())).thenReturn(List.of(invisible));

        ReportPreviewRequest request = new ReportPreviewRequest(
                1L,
                List.of(11L),
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> service.preview(request))
                .isInstanceOf(ReportDefinitionUnavailableException.class)
                .hasMessageContaining("indisponible");
    }

    @Test
    void parsesTypedFilterValuesBeforeBuildingSql() {
        DataSetEntity root = dataSet(1L, "Rhis Test", "rhis_test");
        DataSetField selected = field(10L, "Texte", "texte", DataSetFieldType.TEXT, root);
        List<DataSetField> typedFields = List.of(
                field(11L, "Entier", "entier", DataSetFieldType.INTEGER, root),
                field(12L, "Décimal", "decimal", DataSetFieldType.DECIMAL, root),
                field(13L, "Booléen", "booleen", DataSetFieldType.BOOLEAN, root),
                field(14L, "Date", "date", DataSetFieldType.DATE, root),
                field(15L, "Heure", "heure", DataSetFieldType.TIME, root),
                field(16L, "Date heure", "date_heure", DataSetFieldType.DATE_TIME, root),
                field(17L, "Date heure offset", "date_heure_offset", DataSetFieldType.OFFSET_DATE_TIME, root),
                field(18L, "UUID", "uuid", DataSetFieldType.UUID, root)
        );
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        List<ReportFilterRequest> filters = List.of(
                new ReportFilterRequest(11L, FilterOperator.EQUALS, List.of("42")),
                new ReportFilterRequest(12L, FilterOperator.EQUALS, List.of("12.50")),
                new ReportFilterRequest(13L, FilterOperator.EQUALS, List.of("true")),
                new ReportFilterRequest(14L, FilterOperator.EQUALS, List.of("2026-08-09")),
                new ReportFilterRequest(15L, FilterOperator.EQUALS, List.of("12:30:00")),
                new ReportFilterRequest(16L, FilterOperator.EQUALS, List.of("2026-08-09T12:30:00")),
                new ReportFilterRequest(17L, FilterOperator.EQUALS, List.of("2026-08-09T12:30:00+02:00")),
                new ReportFilterRequest(18L, FilterOperator.EQUALS, List.of(uuid))
        );
        PreparedReportQuery preparedQuery = new PreparedReportQuery("SELECT 1", List.of(), List.of());

        when(dataSetRepository.findById(1L)).thenReturn(Optional.of(root));
        when(dataSetFieldRepository.findByIdIn(any())).thenReturn(
                java.util.stream.Stream.concat(java.util.stream.Stream.of(selected), typedFields.stream()).toList()
        );
        when(dataSetFieldRepository.findByDataset_IdAndActiveTrueOrderByPositionAsc(1L))
                .thenReturn(List.of());
        when(reportSqlBuilder.buildPreview(any())).thenReturn(preparedQuery);
        when(reportPreviewExecutor.execute(preparedQuery))
                .thenReturn(new ReportPreviewResponse(List.of(), List.of(), false, 0));

        service.preview(new ReportPreviewRequest(1L, List.of(10L), filters, List.of()));

        ArgumentCaptor<ResolvedReportDefinition> captor =
                ArgumentCaptor.forClass(ResolvedReportDefinition.class);
        verify(reportSqlBuilder).buildPreview(captor.capture());
        List<Object> values = captor.getValue().filters().stream()
                .map(filter -> filter.values().get(0))
                .toList();

        assertThat(values).containsExactly(
                42L,
                new BigDecimal("12.50"),
                true,
                LocalDate.of(2026, 8, 9),
                LocalTime.of(12, 30),
                LocalDateTime.of(2026, 8, 9, 12, 30),
                OffsetDateTime.parse("2026-08-09T12:30:00+02:00"),
                UUID.fromString(uuid)
        );
    }

    private DataSetEntity dataSet(Long id, String displayName, String sourceName) {
        DataSetEntity dataSet = new DataSetEntity(displayName, sourceName);
        dataSet.setId(id);
        dataSet.setActive(true);
        dataSet.setDisplayMain(true);
        dataSet.setDisplayRelated(true);
        return dataSet;
    }

    private DataSetField field(
            Long id,
            String displayName,
            String sourceName,
            DataSetFieldType type,
            DataSetEntity dataSet
    ) {
        DataSetField field = new DataSetField();
        field.setId(id);
        field.setDisplayName(displayName);
        field.setSourceName(sourceName);
        field.setDataType(type);
        field.setActive(true);
        field.setVisible(true);
        field.setDataset(dataSet);
        return field;
    }

    private TableRelationProjection relation(Long sourceDataSetId, Long targetDataSetId) {
        return relation("fk_relation", sourceDataSetId, targetDataSetId);
    }

    private TableRelationProjection relation(
            String constraintName,
            Long sourceDataSetId,
            Long targetDataSetId
    ) {
        return relation(
                constraintName,
                1,
                sourceDataSetId,
                targetDataSetId,
                "employee_fk_id",
                "emp_pk_id"
        );
    }

    private TableRelationProjection relation(
            String constraintName,
            Integer position,
            Long sourceDataSetId,
            Long targetDataSetId,
            String sourceColumn,
            String targetColumn
    ) {
        return new TestTableRelation(
                constraintName,
                position,
                sourceDataSetId,
                targetDataSetId,
                sourceColumn,
                targetColumn
        );
    }

    private record TestTableRelation(
            String constraintName,
            Integer position,
            Long sourceDatasetId,
            Long targetDatasetId,
            String sourceColumn,
            String targetColumn
    ) implements TableRelationProjection {

        @Override
        public String getConstraintName() {
            return constraintName;
        }

        @Override
        public Integer getPosition() {
            return position;
        }

        @Override
        public Long getSourceDatasetId() {
            return sourceDatasetId;
        }

        @Override
        public String getSourceTable() {
            return "rhis_shift";
        }

        @Override
        public String getSourceDisplayName() {
            return "Rhis Shift";
        }

        @Override
        public String getSourceColumn() {
            return sourceColumn;
        }

        @Override
        public Long getTargetDatasetId() {
            return targetDatasetId;
        }

        @Override
        public String getTargetTable() {
            return "rhis_employee";
        }

        @Override
        public String getTargetDisplayName() {
            return "Rhis Employee";
        }

        @Override
        public String getTargetColumn() {
            return targetColumn;
        }
    }
}
