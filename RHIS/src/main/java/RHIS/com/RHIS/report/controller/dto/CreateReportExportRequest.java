package RHIS.com.RHIS.report.controller.dto;

import RHIS.com.RHIS.report.model.ReportExportFormat;
import jakarta.validation.constraints.NotNull;

public record CreateReportExportRequest(@NotNull ReportExportFormat format) {
}
