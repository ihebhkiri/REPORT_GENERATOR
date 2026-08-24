package RHIS.com.RHIS.report.controller;

import RHIS.com.RHIS.auth.UserPrincipal;
import RHIS.com.RHIS.report.controller.dto.CreateReportExportRequest;
import RHIS.com.RHIS.report.controller.dto.ReportExportResponse;
import RHIS.com.RHIS.report.controller.dto.ReportGenerationResponse;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewRequest;
import RHIS.com.RHIS.report.service.ReportExportService;
import RHIS.com.RHIS.report.service.ReportGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/report-generations")
@RequiredArgsConstructor
public class ReportGenerationController {

    private final ReportGenerationService generationService;
    private final ReportExportService exportService;

    @PostMapping
    public ResponseEntity<ReportGenerationResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody ReportPreviewRequest request
    ) {
        ReportGenerationResponse response = generationService.create(
                principal.getUser(),
                idempotencyKey,
                request
        );
        return ResponseEntity.accepted()
                .body(response);
    }

    @GetMapping("/{generationId}")
    public ReportGenerationResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID generationId
    ) {
        return generationService.get(principal.getUser().getId(), generationId);
    }

    @DeleteMapping("/{generationId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID generationId
    ) {
        generationService.delete(principal.getUser().getId(), generationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{generationId}/exports")
    public ResponseEntity<ReportExportResponse> createExport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID generationId,
            @Valid @RequestBody CreateReportExportRequest request
    ) {
        ReportExportResponse response = exportService.create(
                principal.getUser().getId(),
                generationId,
                request.format()
        );
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/report-exports/" + response.exportId()))
                .body(response);
    }
}
