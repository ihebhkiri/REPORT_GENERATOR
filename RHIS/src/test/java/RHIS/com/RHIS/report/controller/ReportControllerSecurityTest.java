package RHIS.com.RHIS.report.controller;

import RHIS.com.RHIS.auth.CustomUserDetailService;
import RHIS.com.RHIS.auth.JwtCookieFilter;
import RHIS.com.RHIS.auth.JwtService;
import RHIS.com.RHIS.auth.SecurityConfig;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewResponse;
import RHIS.com.RHIS.report.exception.ReportResourceNotFoundException;
import RHIS.com.RHIS.report.exception.ReportQueryTimeoutException;
import RHIS.com.RHIS.report.service.ReportExportService;
import RHIS.com.RHIS.report.service.ReportGenerationService;
import RHIS.com.RHIS.report.service.ReportPreviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        ReportController.class,
        ReportGenerationController.class,
        ReportExportController.class
})
@Import({SecurityConfig.class, JwtCookieFilter.class, ReportExceptionHandler.class})
class ReportControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportPreviewService reportPreviewService;

    @MockitoBean
    private ReportGenerationService reportGenerationService;

    @MockitoBean
    private ReportExportService reportExportService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    @Test
    void rejectsUnauthenticatedPreview() throws Exception {
        mockMvc.perform(post("/api/v1/reports/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnauthenticatedGenerationPolling() throws Exception {
        mockMvc.perform(get("/api/v1/report-generations/66a6b933-3bd8-4d9b-99ae-4eac2d95f73a"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnauthenticatedExportPolling() throws Exception {
        mockMvc.perform(get("/api/v1/report-exports/8574b46f-a936-46e7-92a9-cc49ec41b652"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsAuthenticatedPreview() throws Exception {
        when(reportPreviewService.preview(any())).thenReturn(
                new ReportPreviewResponse(List.of(), List.of(), false, 0)
        );

        mockMvc.perform(post("/api/v1/reports/preview")
                        .with(user("preview-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedRowCount").value(0))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void returnsProblemDetailForInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/reports/preview")
                        .with(user("preview-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requête de preview invalide"));
    }

    @Test
    void returnsNotFoundProblemDetailForInaccessibleDataset() throws Exception {
        when(reportPreviewService.preview(any()))
                .thenThrow(new ReportResourceNotFoundException("Dataset inaccessible."));

        mockMvc.perform(post("/api/v1/reports/preview")
                        .with(user("preview-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ressource de rapport introuvable"))
                .andExpect(jsonPath("$.detail").value("Dataset inaccessible."));
    }

    @Test
    void returnsSanitizedGatewayTimeoutProblemDetail() throws Exception {
        when(reportPreviewService.preview(any()))
                .thenThrow(new ReportQueryTimeoutException(
                        "La prévisualisation a dépassé cinq secondes.",
                        new RuntimeException("SELECT secret FROM payroll")
                ));

        mockMvc.perform(post("/api/v1/reports/preview")
                        .with(user("preview-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.detail").value("La prévisualisation a dépassé cinq secondes."))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("payroll")
                )));
    }

    private String validRequest() {
        return """
                {
                  "rootDatasetId": 1,
                  "selectedFieldIds": [10],
                  "filters": [],
                  "sorts": []
                }
                """;
    }
}
