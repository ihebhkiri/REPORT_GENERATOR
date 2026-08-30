package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.auth.user.UserEntity;
import RHIS.com.RHIS.bot.catalog.ReportCatalogProvider;
import RHIS.com.RHIS.bot.config.BotAiProperties;
import RHIS.com.RHIS.bot.controller.dto.BotReportRequest;
import RHIS.com.RHIS.bot.controller.dto.BotReportResponse;
import RHIS.com.RHIS.bot.dto.BotReportPlan;
import RHIS.com.RHIS.bot.exception.BotRequestException;
import RHIS.com.RHIS.report.controller.dto.ReportGenerationResponse;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewRequest;
import RHIS.com.RHIS.report.exception.ReportValidationException;
import RHIS.com.RHIS.report.model.ReportGenerationStatus;
import RHIS.com.RHIS.report.service.ReportDefinitionResolver;
import RHIS.com.RHIS.report.service.ReportGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotReportServiceTest {

    @Mock private ReportCatalogProvider catalogProvider;
    @Mock private BotReportPlanner planner;
    @Mock private ReportDefinitionResolver definitionResolver;
    @Mock private ReportGenerationService generationService;
    @Mock private UserEntity owner;

    private BotReportService service;

    @BeforeEach
    void setUp() {
        service = new BotReportService(catalogProvider, planner, definitionResolver,
                generationService, new ObjectMapper(), new BotAiProperties());
    }

    private BotReportPlan readyPlan() {
        return new BotReportPlan("READY", null, "Rapport employés",
                1L, List.of(10L), List.of(), List.of());
    }

    private ReportGenerationResponse generationResponse() {
        return new ReportGenerationResponse(UUID.randomUUID(), ReportGenerationStatus.PENDING,
                null, 0, null, null, null, null, null, List.of());
    }

    @Test
    void createsGenerationWhenPlanIsValid() {
        when(catalogProvider.buildCatalog()).thenReturn(List.of());
        when(planner.plan(anyString(), eq("liste des employés"), isNull()))
                .thenReturn(readyPlan());
        ReportGenerationResponse generation = generationResponse();
        when(generationService.create(any(), any(), any())).thenReturn(generation);

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("liste des employés", null));

        assertEquals("READY", response.status());
        assertEquals(generation.generationId(), response.generationId());
        assertEquals("XLSX", response.format().name());
        assertEquals("Rapport employés", response.planSummary());
    }

    @Test
    void returnsClarificationWithoutResolving() {
        when(catalogProvider.buildCatalog()).thenReturn(List.of());
        when(planner.plan(anyString(), anyString(), isNull())).thenReturn(
                new BotReportPlan("NEEDS_CLARIFICATION", "Quel restaurant ?",
                        null, null, null, null, null));

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("un rapport", null));

        assertEquals("NEEDS_CLARIFICATION", response.status());
        assertEquals("Quel restaurant ?", response.question());
        verifyNoInteractions(definitionResolver, generationService);
    }
    @Test
    void retriesOnceThenSucceedsAfterValidationError() {
        when(catalogProvider.buildCatalog()).thenReturn(List.of());
        when(planner.plan(anyString(), anyString(), any())).thenReturn(readyPlan());
        when(definitionResolver.resolve(any(ReportPreviewRequest.class)))
                .thenThrow(new ReportValidationException("L'opérateur BETWEEN attend 2 valeur(s)."))
                .thenReturn(null);
        when(generationService.create(any(), any(), any())).thenReturn(generationResponse());

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("rapport", null));

        assertEquals("READY", response.status());
        verify(planner, times(2)).plan(anyString(), anyString(), any());
        verify(definitionResolver, times(2)).resolve(any(ReportPreviewRequest.class));
        verify(generationService, times(1)).create(any(), any(), any());
    }

    @Test
    void returnsFailedAfterSecondValidationError() {
        when(catalogProvider.buildCatalog()).thenReturn(List.of());
        when(planner.plan(anyString(), anyString(), any())).thenReturn(readyPlan());
        when(definitionResolver.resolve(any(ReportPreviewRequest.class)))
                .thenThrow(new ReportValidationException("Opérateur inconnu : FOO."));

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("rapport", null));

        assertEquals("FAILED", response.status());
        assertEquals(List.of("Opérateur inconnu : FOO."), response.errors());
        verifyNoInteractions(generationService);
        verify(planner, times(2)).plan(anyString(), anyString(), any());
    }

    @Test
    void rejectsMessageOverLimitBeforeCallingModel() {
        BotReportRequest request = new BotReportRequest("x".repeat(2001), null);

        assertThrows(BotRequestException.class,
                () -> service.generate(owner, UUID.randomUUID(), request));

        verifyNoInteractions(planner, catalogProvider);
    }

}
