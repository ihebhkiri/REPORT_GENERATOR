package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.auth.UserPrincipal;
import RHIS.com.RHIS.bot.controller.dto.BotReportRequest;
import RHIS.com.RHIS.bot.controller.dto.BotReportResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bot")
@RequiredArgsConstructor
public class BotReportController {

    private final BotReportService botReportService;

    @PostMapping("/reports")
    public ResponseEntity<BotReportResponse> createReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody BotReportRequest request
    ) {
        UUID key = idempotencyKey == null ? UUID.randomUUID() : idempotencyKey;
        BotReportResponse response = botReportService.generate(principal.getUser(), key, request);
        if ("READY".equals(response.status())) {
            return ResponseEntity.accepted()
                    .location(URI.create("/api/v1/report-generations/" + response.generationId()))
                    .body(response);
        }
        if ("FAILED".equals(response.status())) {
            return ResponseEntity.unprocessableEntity().body(response);
        }
        return ResponseEntity.ok(response);
    }
}