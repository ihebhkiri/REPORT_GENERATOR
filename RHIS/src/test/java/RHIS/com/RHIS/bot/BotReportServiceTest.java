package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.auth.user.UserEntity;
import RHIS.com.RHIS.bot.catalog.ReportCatalogProvider;
import RHIS.com.RHIS.bot.catalog.CatalogDataset;
import RHIS.com.RHIS.bot.catalog.CatalogField;
import RHIS.com.RHIS.bot.catalog.CatalogRelation;
import RHIS.com.RHIS.bot.catalog.ReportCatalog;
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
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
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
                1L, List.of(), List.of(10L), List.of(), List.of(), List.of(), List.of());
    }

    private ReportCatalog catalog() {
        CatalogField name = new CatalogField(10L, "Nom", "TEXT", List.of("EQUALS"), "", List.of());
        return new ReportCatalog(
                List.of(new CatalogDataset(1L, "Employés", List.of(name), "", List.of())),
                List.of(),
                List.of());
    }

    private ReportCatalog relatedCatalog(List<CatalogRelation> relations) {
        CatalogField name = new CatalogField(10L, "Nom", "TEXT", List.of("EQUALS"), "", List.of());
        CatalogField salary = new CatalogField(20L, "Salaire", "DECIMAL", List.of("EQUALS"), "", List.of());
        return new ReportCatalog(
                List.of(new CatalogDataset(1L, "Employés", List.of(name), "", List.of())),
                List.of(new CatalogDataset(2L, "Contrats", List.of(salary), "", List.of())),
                relations);
    }

    private BotReportPlan relatedPlan(Long relatedId, Long fieldId) {
        return new BotReportPlan("READY", null, "Employés et contrats", 1L,
                List.of(relatedId), List.of(10L, fieldId), List.of(), List.of(), List.of(), List.of());
    }

    private BotReportPlan clarificationPlan(String question) {
        return new BotReportPlan("NEEDS_CLARIFICATION", question, null, null,
                null, null, null, null, null, List.of());
    }

    private ReportGenerationResponse generationResponse() {
        return new ReportGenerationResponse(UUID.randomUUID(), ReportGenerationStatus.PENDING,
                null, 0, null, null, null, null, null, List.of());
    }

    @Test
    void createsGenerationWhenPlanIsValid() {
        when(catalogProvider.buildCatalog()).thenReturn(catalog());
        when(planner.plan(anyString(), any(BotReportRequest.class), isNull()))
                .thenReturn(readyPlan());
        ReportGenerationResponse generation = generationResponse();
        when(generationService.create(any(), any(), any())).thenReturn(generation);

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("liste des employés", null, null, null));

        assertEquals("READY", response.status());
        assertEquals(generation.generationId(), response.generationId());
        assertEquals("XLSX", response.format().name());
        assertEquals("Rapport employés", response.planSummary());
    }

    @Test
    void returnsClarificationWithoutResolving() {
        when(catalogProvider.buildCatalog()).thenReturn(catalog());
        when(planner.plan(anyString(), any(BotReportRequest.class), isNull())).thenReturn(
                new BotReportPlan("NEEDS_CLARIFICATION", "Quel restaurant ?",
                        null, null, null, null, null, null, null, List.of()));

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("un rapport", null, null, null));

        assertEquals("NEEDS_CLARIFICATION", response.status());
        assertEquals("Quel restaurant ?", response.question());
        verifyNoInteractions(definitionResolver, generationService);
    }
    @Test
    void retriesOnceThenSucceedsAfterValidationError() {
        when(catalogProvider.buildCatalog()).thenReturn(catalog());
        when(planner.plan(anyString(), any(BotReportRequest.class), any())).thenReturn(readyPlan());
        when(definitionResolver.resolve(any(ReportPreviewRequest.class)))
                .thenThrow(new ReportValidationException("L'opérateur BETWEEN attend 2 valeur(s)."))
                .thenReturn(null);
        when(generationService.create(any(), any(), any())).thenReturn(generationResponse());

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("rapport", null, null, null));

        assertEquals("READY", response.status());
        verify(planner, times(2)).plan(anyString(), any(BotReportRequest.class), any());
        verify(definitionResolver, times(2)).resolve(any(ReportPreviewRequest.class));
        verify(generationService, times(1)).create(any(), any(), any());
    }

    @Test
    void returnsFailedAfterSecondValidationError() {
        when(catalogProvider.buildCatalog()).thenReturn(catalog());
        when(planner.plan(anyString(), any(BotReportRequest.class), any())).thenReturn(readyPlan());
        when(definitionResolver.resolve(any(ReportPreviewRequest.class)))
                .thenThrow(new ReportValidationException("Opérateur inconnu : FOO."));

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("rapport", null, null, null));

        assertEquals("FAILED", response.status());
        assertEquals(List.of("Je n’ai pas pu créer ce rapport. Reformulez votre demande avec les informations souhaitées."),
                response.errors());
        verifyNoInteractions(generationService);
        verify(planner, times(2)).plan(anyString(), any(BotReportRequest.class), any());
    }

    @Test
    void rejectsMessageOverLimitBeforeCallingModel() {
        BotReportRequest request = new BotReportRequest("x".repeat(2001), null, null, null);

        assertThrows(BotRequestException.class,
                () -> service.generate(owner, UUID.randomUUID(), request));

        verifyNoInteractions(planner, catalogProvider);
    }

    @Test
    void createsGenerationForConnectedRelatedDataset() {
        ReportCatalog catalog = relatedCatalog(List.of(new CatalogRelation(2L, 1L)));
        when(catalogProvider.buildCatalog()).thenReturn(catalog);
        when(planner.plan(anyString(), any(BotReportRequest.class), isNull())).thenReturn(relatedPlan(2L, 20L));
        when(generationService.create(any(), any(), any())).thenReturn(generationResponse());

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("employés et salaires des contrats", null, null, null));

        assertEquals("READY", response.status());
        verify(definitionResolver).resolve(any(ReportPreviewRequest.class));
    }

    @Test
    void rejectsReadyPlanWhenRelatedDatasetIsNotConnected() {
        when(catalogProvider.buildCatalog()).thenReturn(relatedCatalog(List.of()));
        BotReportPlan plan = relatedPlan(2L, 20L);
        when(planner.plan(anyString(), any(BotReportRequest.class), any())).thenReturn(plan);

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("employés et contrats", null, null, null));

        assertEquals("FAILED", response.status());
        verifyNoInteractions(definitionResolver, generationService);
    }

    @Test
    void rejectsReadyPlanWithUnexposedRelatedDataset() {
        when(catalogProvider.buildCatalog()).thenReturn(catalog());
        BotReportPlan plan = relatedPlan(99L, 20L);
        when(planner.plan(anyString(), any(BotReportRequest.class), any())).thenReturn(plan);

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("employés et dataset inventé", null, null, null));

        assertEquals("FAILED", response.status());
        verifyNoInteractions(definitionResolver, generationService);
    }

    @Test
    void rejectsReadyPlanWithInvisibleOrInventedField() {
        when(catalogProvider.buildCatalog()).thenReturn(
                relatedCatalog(List.of(new CatalogRelation(1L, 2L))));
        BotReportPlan plan = relatedPlan(2L, 999L);
        when(planner.plan(anyString(), any(BotReportRequest.class), any())).thenReturn(plan);

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("employés et champ masqué", null, null, null));

        assertEquals("FAILED", response.status());
        verifyNoInteractions(definitionResolver, generationService);
    }

    @Test
    void acceptsStructuredClarificationThatProducesReadyPlan() {
        when(catalogProvider.buildCatalog()).thenReturn(catalog());
        when(planner.plan(anyString(), any(BotReportRequest.class), isNull()))
                .thenReturn(readyPlan());
        when(generationService.create(any(), any(), any())).thenReturn(generationResponse());

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("Liste des employés", null,
                        "Actifs uniquement ou tous ?", "Tous"));

        assertEquals("READY", response.status());
    }

    @Test
    void allowsADifferentClarificationQuestion() {
        when(catalogProvider.buildCatalog()).thenReturn(catalog());
        when(planner.plan(anyString(), any(BotReportRequest.class), isNull()))
                .thenReturn(clarificationPlan("Quel restaurant souhaitez-vous consulter ?"));

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("Liste des employés", null,
                        "Actifs uniquement ou tous ?", "Tous"));

        assertEquals("NEEDS_CLARIFICATION", response.status());
        assertEquals("Quel restaurant souhaitez-vous consulter ?", response.question());
    }

    @Test
    void failsWhenModelRepeatsAnsweredQuestionTwice() {
        String question = "Contrats actifs ou tous les contrats ?";
        when(catalogProvider.buildCatalog()).thenReturn(catalog());
        when(planner.plan(anyString(), any(BotReportRequest.class), isNull()))
                .thenReturn(clarificationPlan("  CONTRATS   ACTIFS OU TOUS LES CONTRATS ? "));
        when(planner.plan(anyString(), any(BotReportRequest.class), anyList()))
                .thenReturn(clarificationPlan(question));

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("Liste des contrats", null, question, "Tous les contrats"));

        assertEquals("FAILED", response.status());
        assertEquals(List.of("Je n’ai pas pu créer ce rapport. Reformulez votre demande avec les informations souhaitées."),
                response.errors());
        verify(planner, times(2)).plan(anyString(), any(BotReportRequest.class), any());
        verifyNoInteractions(definitionResolver, generationService);
    }

    @Test
    void rejectsIncompleteClarificationPairBeforeCallingModel() {
        BotReportRequest request = new BotReportRequest(
                "Liste des contrats", null, "Actifs ou tous ?", null);

        assertThrows(BotRequestException.class,
                () -> service.generate(owner, UUID.randomUUID(), request));

        verifyNoInteractions(planner, catalogProvider);
    }

    @Test
    void replacesTechnicalSummaryWithBusinessMessage() {
        BotReportPlan technicalPlan = new BotReportPlan("READY", null,
                "Rapport construit depuis le dataset Employés", 1L, List.of(),
                List.of(10L), List.of(), List.of(), List.of(), List.of());
        when(catalogProvider.buildCatalog()).thenReturn(catalog());
        when(planner.plan(anyString(), any(BotReportRequest.class), isNull()))
                .thenReturn(technicalPlan);
        when(generationService.create(any(), any(), any())).thenReturn(generationResponse());

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("Liste des employés", null, null, null));

        assertEquals("Votre rapport est prêt.", response.planSummary());
    }

    @Test
    void neverReturnsTechnicalClarification() {
        when(catalogProvider.buildCatalog()).thenReturn(catalog());
        when(planner.plan(anyString(), any(BotReportRequest.class), any()))
                .thenReturn(clarificationPlan("Quelles tables souhaitez-vous utiliser ?"));

        BotReportResponse response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("Un rapport", null, null, null));

        assertEquals("FAILED", response.status());
        assertEquals(List.of("Je n’ai pas pu créer ce rapport. Reformulez votre demande avec les informations souhaitées."),
                response.errors());
        verify(planner, times(2)).plan(anyString(), any(BotReportRequest.class), any());
    }

    @Test
    void expandsAllAuthorizedFieldsInCatalogOrderWithoutAddingFilterOnlyFields() {
        var name = new CatalogField(10L, "Nom", "TEXT", List.of("EQUALS"), "", List.of());
        var firstName = new CatalogField(11L, "Prénom", "TEXT", List.of("EQUALS"), "", List.of());
        var salary = new CatalogField(20L, "Salaire", "DECIMAL", List.of("GREATER_THAN"), "", List.of());
        when(catalogProvider.buildCatalog()).thenReturn(new ReportCatalog(
                List.of(new CatalogDataset(1L, "Employés", List.of(firstName, name), "", List.of())),
                List.of(new CatalogDataset(2L, "Contrats", List.of(salary), "", List.of())),
                List.of(new CatalogRelation(1L, 2L))));
        var plan = new BotReportPlan("READY", null, "Liste des employés", 1L, List.of(2L), List.of(),
                List.of(new BotReportPlan.PlanFilter(20L, "GREATER_THAN", List.of("2000"))),
                List.of(new BotReportPlan.PlanSort(10L, "ASC")), List.of(), List.of(1L));
        when(planner.plan(anyString(), any(), any())).thenReturn(plan);
        when(generationService.create(any(), any(), any())).thenReturn(generationResponse());
        var response = service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("Liste des employés avec un salaire supérieur à 2000, triée par nom", null, null, null));
        assertEquals("READY", response.status());
        var preview = ArgumentCaptor.forClass(ReportPreviewRequest.class);
        verify(definitionResolver).resolve(preview.capture());
        assertEquals(List.of(11L, 10L), preview.getValue().selectedFieldIds());
        assertEquals(20L, preview.getValue().filters().get(0).fieldId());
        assertEquals(10L, preview.getValue().sorts().get(0).fieldId());
        verify(generationService).create(any(), any(), eq(preview.getValue()));
    }

    @Test
    void keepsAnExplicitSelectionInTheRequestedOrder() {
        var root = catalog().rootDatasets().get(0);
        var firstName = new CatalogField(11L, "Prénom", "TEXT", List.of("EQUALS"), "", List.of());
        when(catalogProvider.buildCatalog()).thenReturn(new ReportCatalog(
                List.of(new CatalogDataset(1L, root.displayName(), List.of(root.fields().get(0), firstName), "", List.of())),
                List.of(), List.of()));
        var plan = new BotReportPlan("READY", null, "Prénom et nom", 1L, List.of(),
                List.of(11L, 10L), List.of(), List.of(), List.of(), List.of());
        when(planner.plan(anyString(), any(), any())).thenReturn(plan);
        when(generationService.create(any(), any(), any())).thenReturn(generationResponse());
        service.generate(owner, UUID.randomUUID(), new BotReportRequest("Prénom puis nom des employés", null, null, null));
        var preview = ArgumentCaptor.forClass(ReportPreviewRequest.class);
        verify(definitionResolver).resolve(preview.capture());
        assertEquals(List.of(11L, 10L), preview.getValue().selectedFieldIds());
    }

    @Test
    void refusesEmptySelectionsAndInvalidFullSelectionTargets() {
        when(catalogProvider.buildCatalog()).thenReturn(catalog());
        for (List<Long> allIds : List.of(List.<Long>of(), List.of(99L), List.of(1L, 1L))) {
            var plan = new BotReportPlan("READY", null, "Rapport", 1L, List.of(), List.of(),
                    List.of(), List.of(), List.of(), allIds);
            when(planner.plan(anyString(), any(), any())).thenReturn(plan);
            assertEquals("FAILED", service.generate(owner, UUID.randomUUID(),
                    new BotReportRequest("Liste des employés", null, null, null)).status());
        }
        verifyNoInteractions(definitionResolver, generationService);
    }

    @Test
    void fullSelectionDoesNotBypassValidationOfInventedFieldsOrCurrentPermissions() {
        when(catalogProvider.buildCatalog()).thenReturn(catalog());
        var invalid = new BotReportPlan("READY", null, "Rapport", 1L, List.of(), List.of(999L),
                List.of(), List.of(), List.of(), List.of(1L));
        when(planner.plan(anyString(), any(), any())).thenReturn(invalid);
        assertEquals("FAILED", service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("Liste des employés", null, null, null)).status());
        verifyNoInteractions(definitionResolver, generationService);

        var all = new BotReportPlan("READY", null, "Rapport", 1L, List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(1L));
        when(planner.plan(anyString(), any(), any())).thenReturn(all);
        when(definitionResolver.resolve(any())).thenThrow(new ReportValidationException("Champ révoqué"));
        assertEquals("FAILED", service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("Liste des employés", null, null, null)).status());
        verifyNoInteractions(generationService);
    }

    @Test
    void correctsGenericFieldQuestionsIntoACompleteList() {
        when(catalogProvider.buildCatalog()).thenReturn(catalog());
        when(planner.plan(anyString(), any(), isNull())).thenReturn(clarificationPlan("Quels champs souhaitez-vous afficher ?"));
        when(planner.plan(anyString(), any(), anyList())).thenReturn(new BotReportPlan(
                "READY", null, "Liste des employés", 1L, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(1L)));
        when(generationService.create(any(), any(), any())).thenReturn(generationResponse());
        assertEquals("READY", service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("Je veux la liste des employés", null, null, null)).status());
        verify(planner, times(2)).plan(anyString(), any(), any());
    }

    @Test
    void rejectsJoinAndInternalIdQuestionsButAllowsBusinessIdentityQuestions() {
        when(catalogProvider.buildCatalog()).thenReturn(catalog());
        for (String question : List.of("Quel join utiliser ?", "Quel ID choisir ?", "Quel dataset_id ?")) {
            when(planner.plan(anyString(), any(), any())).thenReturn(clarificationPlan(question));
            assertEquals("FAILED", service.generate(owner, UUID.randomUUID(),
                    new BotReportRequest("Un rapport", null, null, null)).status());
        }
        when(planner.plan(anyString(), any(), any())).thenReturn(clarificationPlan("L’identité du salarié ou du responsable ?"));
        assertEquals("NEEDS_CLARIFICATION", service.generate(owner, UUID.randomUUID(),
                new BotReportRequest("Un rapport", null, null, null)).status());
        verifyNoInteractions(generationService);
    }

}
