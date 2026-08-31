package RHIS.com.RHIS.bot.controller.dto;

import RHIS.com.RHIS.report.model.ReportExportFormat;

import java.util.List;
import java.util.UUID;

public record BotReportResponse(
        String status,
        String question,
        UUID generationId,
        ReportExportFormat format,
        String planSummary,
        List<String> errors
) {
    public static BotReportResponse ready(UUID generationId, ReportExportFormat format,
            String planSummary) {
        return new BotReportResponse("READY", null, generationId, format, planSummary, List.of());
    }

    public static BotReportResponse clarification(String question) {
        return new BotReportResponse("NEEDS_CLARIFICATION", question, null, null, null, List.of());
    }

    public static BotReportResponse failed(List<String> errors) {
        return new BotReportResponse("FAILED", null, null, null, null, List.copyOf(errors));
    }
}
