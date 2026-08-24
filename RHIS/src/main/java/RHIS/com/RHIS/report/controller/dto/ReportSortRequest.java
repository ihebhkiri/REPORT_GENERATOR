package RHIS.com.RHIS.report.controller.dto;

import jakarta.validation.constraints.NotNull;

public record ReportSortRequest(
        @NotNull Long fieldId,
        @NotNull SortDirection direction
) {
}
