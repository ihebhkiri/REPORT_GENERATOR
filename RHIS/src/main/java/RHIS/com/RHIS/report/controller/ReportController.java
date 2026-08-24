package RHIS.com.RHIS.report.controller;

import RHIS.com.RHIS.report.controller.dto.ReportPreviewRequest;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewResponse;
import RHIS.com.RHIS.report.service.ReportPreviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportPreviewService reportPreviewService;

    @PostMapping("/preview")
    public ResponseEntity<ReportPreviewResponse> preview(
            @Valid @RequestBody ReportPreviewRequest request
    ) {
        return ResponseEntity.ok(reportPreviewService.preview(request));
    }
}

