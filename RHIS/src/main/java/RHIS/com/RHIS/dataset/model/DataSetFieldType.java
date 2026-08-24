package RHIS.com.RHIS.dataset.model;

import java.util.List;
import java.util.Locale;

import static RHIS.com.RHIS.dataset.model.FilterOperator.*;

public enum DataSetFieldType {
    TEXT,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    DATE,
    TIME,
    DATE_TIME,
    OFFSET_DATE_TIME,
    UUID,
    UNSUPPORTED;

    private static final List<FilterOperator> COMMON_OPERATORS = List.of(
            EQUALS
    );

    private static final List<FilterOperator> ORDERED_OPERATORS = List.of(
            EQUALS,
            GREATER_THAN,
            GREATER_THAN_OR_EQUAL,
            LESS_THAN,
            LESS_THAN_OR_EQUAL,
            BETWEEN
    );

    public static DataSetFieldType fromPostgreSql(String dataType, String udtName) {
        if (dataType == null) {
            return UNSUPPORTED;
        }

        String normalizedDataType = dataType.toLowerCase(Locale.ROOT);
        String normalizedUdtName = udtName == null ? "" : udtName.toLowerCase(Locale.ROOT);

        if ("uuid".equals(normalizedDataType) || "uuid".equals(normalizedUdtName)) {
            return UUID;
        }

        return switch (normalizedDataType) {
            case "character varying", "character", "text" -> TEXT;
            case "smallint", "integer", "bigint" -> INTEGER;
            case "numeric", "decimal", "real", "double precision" -> DECIMAL;
            case "boolean" -> BOOLEAN;
            case "date" -> DATE;
            case "time without time zone" -> TIME;
            case "timestamp without time zone" -> DATE_TIME;
            case "timestamp with time zone" -> OFFSET_DATE_TIME;
            default -> UNSUPPORTED;
        };
    }

    public boolean isSupported() {
        return this != UNSUPPORTED;
    }

    public List<FilterOperator> supportedOperators() {
        return switch (this) {
            case TEXT -> List.of(EQUALS, CONTAINS);
            case INTEGER, DECIMAL, DATE, TIME, DATE_TIME, OFFSET_DATE_TIME -> ORDERED_OPERATORS;
            case BOOLEAN, UUID -> COMMON_OPERATORS;
            case UNSUPPORTED -> List.of();
        };
    }
}
