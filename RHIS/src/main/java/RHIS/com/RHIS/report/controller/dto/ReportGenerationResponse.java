package RHIS.com.RHIS.report.controller.dto;

import RHIS.com.RHIS.report.model.ReportGenerationPhase;
import RHIS.com.RHIS.report.model.ReportGenerationStatus;
import RHIS.com.RHIS.report.exception.UnavailableReportElement;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportGenerationResponse(
        UUID generationId,
        ReportGenerationStatus status,
        ReportGenerationPhase phase,
        int progress,
        Long processedRowCount,
        Long totalRowCount,
        Instant createdAt,
        Instant expiresAt,
        String errorCode,
        List<UnavailableReportElement> unavailableElements
) {
}
