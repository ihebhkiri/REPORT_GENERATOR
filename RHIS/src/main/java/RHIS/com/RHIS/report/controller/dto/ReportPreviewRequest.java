package RHIS.com.RHIS.report.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReportPreviewRequest(
        @NotNull Long rootDatasetId,
        @NotEmpty List<@NotNull Long> selectedFieldIds,
        List<@Valid ReportFilterRequest> filters,
        List<@Valid ReportSortRequest> sorts
) {
    public ReportPreviewRequest {
        filters = filters == null ? List.of() : filters;
        sorts = sorts == null ? List.of() : sorts;
    }

}