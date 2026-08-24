package RHIS.com.RHIS.dataset.controller.dto;

import RHIS.com.RHIS.dataset.model.DataSetFieldType;
import RHIS.com.RHIS.dataset.model.FilterOperator;

import java.util.List;

public record DataSetFieldResponse(
        Long id,
        String displayName,
        String sourceName,
        DataSetFieldType dataType,
        boolean nullable,
        boolean supported,
        List<FilterOperator> supportedOperators
) {
}
