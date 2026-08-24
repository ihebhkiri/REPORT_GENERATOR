package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.model.DataSetFieldType;
import RHIS.com.RHIS.dataset.model.FilterOperator;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewColumnResponse;
import RHIS.com.RHIS.report.controller.dto.SortDirection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportSqlBuilderTest {

    private final ReportSqlBuilder builder = new ReportSqlBuilder();

    @Test
    void buildsParameterizedQueryAndEscapesContainsWildcards() {
        DataSetEntity root = dataSet(1L, "rhis_shift");
        DataSetField name = field(10L, "commentaire", DataSetFieldType.TEXT, root);

        ResolvedReportDefinition definition = new ResolvedReportDefinition(
                root,
                List.of(name),
                List.of(new ResolvedFilter(name, FilterOperator.CONTAINS, List.of("50%_!"))),
                List.of(new ResolvedSort(name, SortDirection.DESC)),
                List.of(),
                List.of()
        );

        PreparedReportQuery query = builder.buildPreview(definition);

        assertThat(query.sql())
                .contains("FROM \"public\".\"rhis_shift\" t0")
                .contains("t0.\"commentaire\" ILIKE ? ESCAPE '!'")
                .contains("ORDER BY t0.\"commentaire\" DESC")
                .endsWith("LIMIT 7")
                .doesNotContain("50%_!");
        assertThat(query.parameters()).containsExactly("%50!%!_!!%");
    }

    @Test
    void buildsLeftJoinAndFallsBackToRootPrimaryKeyOrder() {
        DataSetEntity root = dataSet(1L, "rhis_shift");
        DataSetEntity employee = dataSet(2L, "rhis_employee");
        DataSetField rootId = field(10L, "shift_pk_id", DataSetFieldType.INTEGER, root);
        DataSetField employeeName = field(20L, "nom", DataSetFieldType.TEXT, employee);

        ResolvedReportDefinition definition = new ResolvedReportDefinition(
                root,
                List.of(rootId, employeeName),
                List.of(),
                List.of(),
                List.of(new ResolvedJoin(
                        employee,
                        List.of(new ResolvedJoinColumn("employee_fk_id", "emp_pk_id"))
                )),
                List.of(rootId)
        );

        PreparedReportQuery query = builder.buildPreview(definition);

        assertThat(query.sql())
                .contains("LEFT JOIN \"public\".\"rhis_employee\" t1")
                .contains("ON t0.\"employee_fk_id\" = t1.\"emp_pk_id\"")
                .contains("ORDER BY t0.\"shift_pk_id\" ASC");
        assertThat(query.columns())
                .extracting(ReportPreviewColumnResponse::fieldId)
                .containsExactly(10L, 20L);
    }

    @Test
    void buildsUnlimitedFullQueryWithTheSameSelectionFiltersAndOrder() {
        DataSetEntity root = dataSet(1L, "rhis_shift");
        DataSetField name = field(10L, "commentaire", DataSetFieldType.TEXT, root);
        ResolvedReportDefinition definition = new ResolvedReportDefinition(
                root,
                List.of(name),
                List.of(new ResolvedFilter(name, FilterOperator.CONTAINS, List.of("50%"))),
                List.of(new ResolvedSort(name, SortDirection.ASC)),
                List.of(),
                List.of()
        );

        PreparedReportQuery query = builder.buildFull(definition);

        assertThat(query.sql())
                .startsWith("SELECT t0.\"commentaire\" AS \"field_10\"")
                .contains("t0.\"commentaire\" ILIKE ? ESCAPE '!'")
                .endsWith("ORDER BY t0.\"commentaire\" ASC")
                .doesNotContain("LIMIT");
        assertThat(query.parameters()).containsExactly("%50!%%");
    }

    @Test
    void buildsCountQueryWithTheSameJoinsAndFiltersWithoutSelectionOrOrder() {
        DataSetEntity root = dataSet(1L, "rhis_shift");
        DataSetEntity employee = dataSet(2L, "rhis_employee");
        DataSetField rootId = field(10L, "shift_pk_id", DataSetFieldType.INTEGER, root);
        DataSetField employeeName = field(20L, "nom", DataSetFieldType.TEXT, employee);
        ResolvedReportDefinition definition = new ResolvedReportDefinition(
                root,
                List.of(rootId, employeeName),
                List.of(new ResolvedFilter(employeeName, FilterOperator.EQUALS, List.of("Durand"))),
                List.of(new ResolvedSort(rootId, SortDirection.DESC)),
                List.of(new ResolvedJoin(
                        employee,
                        List.of(new ResolvedJoinColumn("employee_fk_id", "emp_pk_id"))
                )),
                List.of(rootId)
        );

        PreparedCountQuery query = builder.buildCount(definition);

        assertThat(query.sql())
                .startsWith("SELECT COUNT(*) FROM \"public\".\"rhis_shift\" t0")
                .contains("LEFT JOIN \"public\".\"rhis_employee\" t1")
                .contains("t1.\"nom\" = ?")
                .doesNotContain("ORDER BY", "LIMIT", "field_10", "field_20");
        assertThat(query.parameters()).containsExactly("Durand");
    }

    private DataSetEntity dataSet(Long id, String sourceName) {
        DataSetEntity dataSet = new DataSetEntity(sourceName, sourceName);
        dataSet.setId(id);
        return dataSet;
    }

    private DataSetField field(
            Long id,
            String sourceName,
            DataSetFieldType type,
            DataSetEntity dataSet
    ) {
        DataSetField field = new DataSetField();
        field.setId(id);
        field.setDisplayName(sourceName);
        field.setSourceName(sourceName);
        field.setDataType(type);
        field.setActive(true);
        field.setVisible(true);
        field.setDataset(dataSet);
        return field;
    }
}
