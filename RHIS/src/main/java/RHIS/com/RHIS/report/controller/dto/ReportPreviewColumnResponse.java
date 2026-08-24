package RHIS.com.RHIS.report.controller.dto;

import RHIS.com.RHIS.dataset.model.DataSetFieldType;

public record ReportPreviewColumnResponse(
        String key,
        Long fieldId,
        String displayName,
        DataSetFieldType dataType
) {
}
