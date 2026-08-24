package RHIS.com.RHIS.report.controller;

import RHIS.com.RHIS.auth.UserPrincipal;
import RHIS.com.RHIS.report.controller.dto.ReportExportResponse;
import RHIS.com.RHIS.report.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/report-exports")
@RequiredArgsConstructor
public class ReportExportController {

    private final ReportExportService exportService;

    @GetMapping("/{exportId}")
    public ReportExportResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID exportId
    ) {
        return exportService.get(principal.getUser().getId(), exportId);
    }

    @GetMapping("/{exportId}/file")
    public ResponseEntity<InputStreamResource> download(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID exportId
    ) {
        ReportExportService.DownloadPayload payload = exportService.openDownload(
                principal.getUser().getId(),
                exportId
        );
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(payload.fileName())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(payload.input()));
    }
}
