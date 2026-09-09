package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.auth.CustomUserDetailService;
import RHIS.com.RHIS.auth.JwtCookieFilter;
import RHIS.com.RHIS.auth.JwtService;
import RHIS.com.RHIS.auth.SecurityConfig;
import RHIS.com.RHIS.auth.UserPrincipal;
import RHIS.com.RHIS.auth.user.UserEntity;
import RHIS.com.RHIS.bot.controller.BotReportController;
import RHIS.com.RHIS.bot.controller.dto.BotReportResponse;
import RHIS.com.RHIS.bot.exception.BotLlmException;
import RHIS.com.RHIS.report.model.ReportExportFormat;
import RHIS.com.RHIS.report.exception.ReportCapacityException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BotReportController.class)
@Import({SecurityConfig.class, JwtCookieFilter.class, BotExceptionHandler.class})
class BotReportControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BotReportService botReportService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    @Test
    void rejectsUnauthenticatedBotRequest() throws Exception {
        mockMvc.perform(post("/api/v1/bot/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsAcceptedResponseForAuthenticatedRequest() throws Exception {
        when(botReportService.generate(any(), any(), any()))
                .thenReturn(BotReportResponse.ready(UUID.randomUUID(),
                        ReportExportFormat.XLSX, "Rapport des employés"));

        mockMvc.perform(post("/api/v1/bot/reports")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.format").value("XLSX"));
    }

    @Test
    void returnsUnprocessableEntityWhenPlanFailsValidation() throws Exception {
        when(botReportService.generate(any(), any(), any()))
                .thenReturn(BotReportResponse.failed(List.of("Opérateur inconnu : FOO.")));

        mockMvc.perform(post("/api/v1/bot/reports")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errors[0]").value("Opérateur inconnu : FOO."));
    }

    @Test
    void returnsBadRequestForBlankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/bot/reports")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hidesProviderFailureDetails() throws Exception {
        when(botReportService.generate(any(), any(), any()))
                .thenThrow(new BotLlmException("Xkiro HTTP 429"));

        mockMvc.perform(post("/api/v1/bot/reports")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Assistant indisponible"))
                .andExpect(jsonPath("$.detail").value(
                        "L’assistant est temporairement indisponible. Réessayez."));
    }

    @Test
    void returnsTooManyRequestsWhenGenerationLimitIsReached() throws Exception {
        when(botReportService.generate(any(), any(), any()))
                .thenThrow(new ReportCapacityException("Le nombre maximal de générations actives est atteint."));

        mockMvc.perform(post("/api/v1/bot/reports")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(
                        "Vous avez déjà plusieurs rapports en cours. Attendez qu’un rapport se termine, puis réessayez."));
    }

    private UsernamePasswordAuthenticationToken principalAuthentication() {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getUser()).thenReturn(mock(UserEntity.class));
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private String validRequest() {
        return """
                { "message": "Liste des employés en Excel" }
                """;
    }
}
