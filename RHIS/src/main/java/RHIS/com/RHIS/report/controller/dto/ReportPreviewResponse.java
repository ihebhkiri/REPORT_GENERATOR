package RHIS.com.RHIS.report.controller.dto;

import java.util.List;
import java.util.Map;

public record ReportPreviewResponse(
        List<ReportPreviewColumnResponse> columns,
        List<Map<String, Object>> rows,
        boolean hasMore,
        int returnedRowCount
) {
}
