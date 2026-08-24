package RHIS.com.RHIS.report.controller.dto;

import RHIS.com.RHIS.report.model.ReportExportFormat;
import RHIS.com.RHIS.report.model.ReportExportStatus;
import RHIS.com.RHIS.report.exception.UnavailableReportElement;

import java.util.List;
import java.util.UUID;

public record ReportExportResponse(
        UUID exportId,
        UUID generationId,
        ReportExportFormat format,
        ReportExportStatus status,
        int progress,
        String errorCode,
        List<UnavailableReportElement> unavailableElements
) {
}
