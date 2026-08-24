package RHIS.com.RHIS.dataset.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataSetFieldTypeTest {

    @Test
    void mapsSupportedPostgreSqlTypes() {
        assertThat(DataSetFieldType.fromPostgreSql("character varying", "varchar"))
                .isEqualTo(DataSetFieldType.TEXT);
        assertThat(DataSetFieldType.fromPostgreSql("bigint", "int8"))
                .isEqualTo(DataSetFieldType.INTEGER);
        assertThat(DataSetFieldType.fromPostgreSql("double precision", "float8"))
                .isEqualTo(DataSetFieldType.DECIMAL);
        assertThat(DataSetFieldType.fromPostgreSql("timestamp with time zone", "timestamptz"))
                .isEqualTo(DataSetFieldType.OFFSET_DATE_TIME);
        assertThat(DataSetFieldType.fromPostgreSql("USER-DEFINED", "uuid"))
                .isEqualTo(DataSetFieldType.UUID);
    }

    @Test
    void exposesOnlyOperatorsSupportedByTheType() {
        assertThat(DataSetFieldType.TEXT.supportedOperators())
                .contains(FilterOperator.CONTAINS)
                .doesNotContain(FilterOperator.BETWEEN);
        assertThat(DataSetFieldType.DATE.supportedOperators())
                .contains(FilterOperator.BETWEEN, FilterOperator.GREATER_THAN);
        assertThat(DataSetFieldType.BOOLEAN.supportedOperators())
                .containsExactly(
                        FilterOperator.EQUALS
                );
        assertThat(DataSetFieldType.UNSUPPORTED.supportedOperators()).isEmpty();
    }
}
