package RHIS.com.RHIS.bot.controller.dto;

import RHIS.com.RHIS.report.model.ReportExportFormat;
import jakarta.validation.constraints.NotBlank;

public record BotReportRequest(
        @NotBlank String message,
        ReportExportFormat format,
        String clarificationQuestion,
        String clarificationAnswer
) {
}
