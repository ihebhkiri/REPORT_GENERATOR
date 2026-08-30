package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.auth.user.UserEntity;
import RHIS.com.RHIS.bot.catalog.ReportCatalogProvider;
import RHIS.com.RHIS.bot.config.BotAiProperties;
import RHIS.com.RHIS.bot.controller.dto.BotReportRequest;
import RHIS.com.RHIS.bot.controller.dto.BotReportResponse;
import RHIS.com.RHIS.bot.dto.BotReportPlan;
import RHIS.com.RHIS.bot.exception.BotLlmException;
import RHIS.com.RHIS.bot.exception.BotRequestException;
import RHIS.com.RHIS.dataset.model.FilterOperator;
import RHIS.com.RHIS.report.controller.dto.ReportFilterRequest;
import RHIS.com.RHIS.report.controller.dto.ReportGenerationResponse;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewRequest;
import RHIS.com.RHIS.report.controller.dto.ReportSortRequest;
import RHIS.com.RHIS.report.controller.dto.SortDirection;
import RHIS.com.RHIS.report.exception.ReportDefinitionUnavailableException;
import RHIS.com.RHIS.report.exception.ReportValidationException;
import RHIS.com.RHIS.report.model.ReportExportFormat;
import RHIS.com.RHIS.report.service.ReportDefinitionResolver;
import RHIS.com.RHIS.report.service.ReportGenerationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BotReportService {

    private final ReportCatalogProvider catalogProvider;
    private final BotReportPlanner planner;
    private final ReportDefinitionResolver definitionResolver;
    private final ReportGenerationService generationService;
    private final ObjectMapper objectMapper;
    private final BotAiProperties properties;

    public BotReportResponse generate(UserEntity owner, UUID idempotencyKey,
            BotReportRequest request) {
        validateMessage(request);
        String catalogJson = catalogJson();
        BotReportPlan plan = planner.plan(catalogJson, request.message(), null);
        if (plan.needsClarification()) {
            return BotReportResponse.clarification(plan.question());
        }
        try {
            return createGeneration(owner, idempotencyKey, request, plan);
        } catch (ReportValidationException | ReportDefinitionUnavailableException firstAttempt) {
            BotReportPlan corrected = planner.plan(catalogJson, request.message(),
                    errorsOf(firstAttempt));
            if (corrected.needsClarification()) {
                return BotReportResponse.clarification(corrected.question());
            }
            try {
                return createGeneration(owner, idempotencyKey, request, corrected);
            } catch (ReportValidationException | ReportDefinitionUnavailableException secondAttempt) {
                return BotReportResponse.failed(errorsOf(secondAttempt));
            }
        }
    }

    private BotReportResponse createGeneration(UserEntity owner, UUID idempotencyKey,
            BotReportRequest request, BotReportPlan plan) {
        ReportExportFormat format = resolvedFormat(request);
        ReportPreviewRequest preview = toPreviewRequest(plan);
        definitionResolver.resolve(preview);
        ReportGenerationResponse generation =
                generationService.create(owner, idempotencyKey, preview);
        return BotReportResponse.ready(generation.generationId(), format, plan.summary());
    }

    private ReportExportFormat resolvedFormat(BotReportRequest request) {
        return request.format() == null ? ReportExportFormat.XLSX : request.format();
    }

    private ReportPreviewRequest toPreviewRequest(BotReportPlan plan) {
        if (!plan.isReady() || plan.rootDatasetId() == null
                || plan.selectedFieldIds() == null || plan.selectedFieldIds().isEmpty()) {
            throw new ReportValidationException(
                    "Le modèle n'a pas produit de définition de rapport exploitable.");
        }
        return new ReportPreviewRequest(
                plan.rootDatasetId(),
                plan.selectedFieldIds(),
                toFilters(plan.filters()),
                toSorts(plan.sorts()));
    }
    private List<ReportFilterRequest> toFilters(List<BotReportPlan.PlanFilter> planFilters) {
        if (planFilters == null) {
            return List.of();
        }
        return planFilters.stream()
                .filter(Objects::nonNull)
                .map(filter -> new ReportFilterRequest(
                        filter.fieldId(),
                        parseOperator(filter.operator()),
                        filter.values() == null ? List.of() : filter.values()))
                .toList();
    }

    private FilterOperator parseOperator(String operator) {
        try {
            return FilterOperator.valueOf(operator);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ReportValidationException("Opérateur de filtre inconnu : " + operator);
        }
    }

    private List<ReportSortRequest> toSorts(List<BotReportPlan.PlanSort> planSorts) {
        if (planSorts == null) {
            return List.of();
        }
        return planSorts.stream()
                .filter(Objects::nonNull)
                .map(sort -> new ReportSortRequest(sort.fieldId(), parseDirection(sort.direction())))
                .toList();
    }

    private SortDirection parseDirection(String direction) {
        try {
            return SortDirection.valueOf(direction);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ReportValidationException("Direction de tri inconnue : " + direction);
        }
    }

    private List<String> errorsOf(Exception exception) {
        if (exception instanceof ReportDefinitionUnavailableException unavailable) {
            return unavailable.getUnavailableElements().stream()
                    .map(element -> element.kind() + " " + element.displayName()
                            + " : " + element.reason())
                    .toList();
        }
        return List.of(exception.getMessage());
    }

    private void validateMessage(BotReportRequest request) {
        if (request.message().length() > properties.getMaxMessageLength()) {
            throw new BotRequestException("La phrase ne peut pas dépasser "
                    + properties.getMaxMessageLength() + " caractères.");
        }
    }

    private String catalogJson() {
        try {
            return objectMapper.writeValueAsString(catalogProvider.buildCatalog());
        } catch (JsonProcessingException exception) {
            throw new BotLlmException("Le catalogue ne peut pas être sérialisé pour le modèle.");
        }
    }

}
