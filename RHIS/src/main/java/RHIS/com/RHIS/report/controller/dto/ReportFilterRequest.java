package RHIS.com.RHIS.report.controller.dto;

import RHIS.com.RHIS.dataset.model.FilterOperator;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReportFilterRequest(
        @NotNull Long fieldId,
        @NotNull FilterOperator operator,
        List<@NotNull String> values
) {
    public ReportFilterRequest {
        values = values == null ? List.of() : values;
    }
}
