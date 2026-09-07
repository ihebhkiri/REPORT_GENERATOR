package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.auth.user.UserEntity;
import RHIS.com.RHIS.bot.catalog.ReportCatalogProvider;
import RHIS.com.RHIS.bot.catalog.CatalogDataset;
import RHIS.com.RHIS.bot.catalog.CatalogRelation;
import RHIS.com.RHIS.bot.catalog.ReportCatalog;
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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BotReportService {

    private static final String GENERIC_READY_SUMMARY = "Votre rapport est prêt.";
    private static final String GENERIC_FAILURE =
            "Je n’ai pas pu créer ce rapport. Reformulez votre demande avec les informations souhaitées.";
    private static final Pattern TECHNICAL_LANGUAGE = Pattern.compile(
            "(?iu)\\b(datasets?|rootdataset|fieldid|datasetid|sql|json|apis?|llm|modèles?|"
                    + "opérateurs?|jointures?|tables?|colonnes?)\\b");

    private final ReportCatalogProvider catalogProvider;
    private final BotReportPlanner planner;
    private final ReportDefinitionResolver definitionResolver;
    private final ReportGenerationService generationService;
    private final ObjectMapper objectMapper;
    private final BotAiProperties properties;

    public BotReportResponse generate(UserEntity owner, UUID idempotencyKey,
            BotReportRequest request) {
        validateMessage(request);
        ReportCatalog catalog = catalogProvider.buildCatalog();
        String catalogJson = catalogJson(catalog);
        BotReportPlan plan = planner.plan(catalogJson, request, null);
        try {
            return handlePlan(owner, idempotencyKey, request, plan, catalog);
        } catch (ReportValidationException | ReportDefinitionUnavailableException firstAttempt) {
            BotReportPlan corrected = planner.plan(catalogJson, request,
                    errorsOf(firstAttempt));
            try {
                return handlePlan(owner, idempotencyKey, request, corrected, catalog);
            } catch (ReportValidationException | ReportDefinitionUnavailableException secondAttempt) {
                return businessFailure();
            }
        }
    }

    private BotReportResponse handlePlan(UserEntity owner, UUID idempotencyKey,
            BotReportRequest request, BotReportPlan plan, ReportCatalog catalog) {
        if (plan.needsClarification()) {
            validateBusinessText(plan.question());
            if (sameQuestion(plan.question(), request.clarificationQuestion())) {
                throw new ReportValidationException("La question a déjà reçu une réponse.");
            }
            return BotReportResponse.clarification(plan.question());
        }
        if (plan.isFailed()) {
            return businessFailure();
        }
        return createGeneration(owner, idempotencyKey, request, plan, catalog);
    }

    private BotReportResponse createGeneration(UserEntity owner, UUID idempotencyKey,
            BotReportRequest request, BotReportPlan plan, ReportCatalog catalog) {
        validatePlan(plan, catalog);
        ReportExportFormat format = resolvedFormat(request);
        ReportPreviewRequest preview = toPreviewRequest(plan);
        definitionResolver.resolve(preview);
        ReportGenerationResponse generation =
                generationService.create(owner, idempotencyKey, preview);
        return BotReportResponse.ready(generation.generationId(), format,
                businessSummary(plan.summary()));
    }

    private ReportExportFormat resolvedFormat(BotReportRequest request) {
        return request.format() == null ? ReportExportFormat.XLSX : request.format();
    }

    private ReportPreviewRequest toPreviewRequest(BotReportPlan plan) {
        return new ReportPreviewRequest(
                plan.rootDatasetId(),
                plan.selectedFieldIds(),
                toFilters(plan.filters()),
                toSorts(plan.sorts()));
    }

    private void validatePlan(BotReportPlan plan, ReportCatalog catalog) {
        if (!plan.isReady() || plan.rootDatasetId() == null
                || plan.selectedFieldIds() == null || plan.selectedFieldIds().isEmpty()) {
            throw new ReportValidationException(
                    "Le modèle n'a pas produit de définition de rapport exploitable.");
        }

        Map<Long, CatalogDataset> roots = byId(catalog.rootDatasets());
        Map<Long, CatalogDataset> related = byId(catalog.relatedDatasets());
        CatalogDataset root = roots.get(plan.rootDatasetId());
        if (root == null) {
            throw new ReportValidationException("Le dataset principal proposé n'est pas exposé.");
        }

        List<Long> relatedIds = plan.relatedDatasetIds() == null
                ? List.of() : plan.relatedDatasetIds();
        if (new HashSet<>(relatedIds).size() != relatedIds.size()
                || relatedIds.contains(plan.rootDatasetId())) {
            throw new ReportValidationException("La liste des datasets associés est invalide.");
        }
        for (Long relatedId : relatedIds) {
            CatalogDataset target = related.get(relatedId);
            if (target == null) {
                throw new ReportValidationException("Le dataset associé proposé n'est pas exposé.");
            }
            if (!isConnected(plan.rootDatasetId(), relatedId, catalog.relations())) {
                throw new ReportValidationException("Les datasets " + root.displayName() + " et "
                        + target.displayName()
                        + " ne sont pas reliés et ne peuvent pas être utilisés dans le même rapport.");
            }
        }

        Set<Long> declaredDatasetIds = new HashSet<>(relatedIds);
        declaredDatasetIds.add(plan.rootDatasetId());
        Map<Long, Long> fieldDatasets = new HashMap<>();
        catalog.rootDatasets().forEach(dataset -> dataset.fields().forEach(
                field -> fieldDatasets.putIfAbsent(field.fieldId(), dataset.datasetId())));
        catalog.relatedDatasets().forEach(dataset -> dataset.fields().forEach(
                field -> fieldDatasets.putIfAbsent(field.fieldId(), dataset.datasetId())));
        for (Long fieldId : referencedFieldIds(plan)) {
            Long datasetId = fieldDatasets.get(fieldId);
            if (datasetId == null || !declaredDatasetIds.contains(datasetId)) {
                throw new ReportValidationException(
                        "Le champ " + fieldId + " n'est pas visible dans les datasets déclarés.");
            }
        }
    }

    private Map<Long, CatalogDataset> byId(List<CatalogDataset> datasets) {
        Map<Long, CatalogDataset> result = new HashMap<>();
        datasets.forEach(dataset -> result.put(dataset.datasetId(), dataset));
        return result;
    }

    private Set<Long> referencedFieldIds(BotReportPlan plan) {
        Set<Long> ids = new LinkedHashSet<>(plan.selectedFieldIds());
        if (plan.filters() != null) {
            plan.filters().stream().filter(Objects::nonNull)
                    .map(BotReportPlan.PlanFilter::fieldId).forEach(ids::add);
        }
        if (plan.sorts() != null) {
            plan.sorts().stream().filter(Objects::nonNull)
                    .map(BotReportPlan.PlanSort::fieldId).forEach(ids::add);
        }
        return ids;
    }

    private boolean isConnected(Long rootId, Long targetId, List<CatalogRelation> relations) {
        Map<Long, Set<Long>> graph = new HashMap<>();
        relations.forEach(relation -> {
            graph.computeIfAbsent(relation.sourceDatasetId(), ignored -> new HashSet<>())
                    .add(relation.targetDatasetId());
            graph.computeIfAbsent(relation.targetDatasetId(), ignored -> new HashSet<>())
                    .add(relation.sourceDatasetId());
        });
        Set<Long> visited = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long current = queue.remove();
            if (!visited.add(current)) {
                continue;
            }
            if (current.equals(targetId)) {
                return true;
            }
            queue.addAll(graph.getOrDefault(current, Set.of()));
        }
        return false;
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

    private BotReportResponse businessFailure() {
        return BotReportResponse.failed(List.of(GENERIC_FAILURE));
    }

    private String businessSummary(String summary) {
        return summary == null || summary.isBlank() || containsTechnicalLanguage(summary)
                ? GENERIC_READY_SUMMARY : summary;
    }

    private void validateBusinessText(String text) {
        if (text == null || text.isBlank() || containsTechnicalLanguage(text)) {
            throw new ReportValidationException(
                    "La formulation destinée à l'utilisateur doit rester en langage métier.");
        }
    }

    private boolean containsTechnicalLanguage(String text) {
        return TECHNICAL_LANGUAGE.matcher(text).find();
    }

    private void validateMessage(BotReportRequest request) {
        boolean hasQuestion = request.clarificationQuestion() != null;
        boolean hasAnswer = request.clarificationAnswer() != null;
        if (hasQuestion != hasAnswer
                || hasQuestion && (request.clarificationQuestion().isBlank()
                        || request.clarificationAnswer().isBlank())) {
            throw new BotRequestException(
                    "La question et la réponse de clarification doivent être fournies ensemble.");
        }
        int length = request.message().length()
                + (hasQuestion ? request.clarificationQuestion().length()
                        + request.clarificationAnswer().length() : 0);
        if (length > properties.getMaxMessageLength()) {
            throw new BotRequestException("La phrase ne peut pas dépasser "
                    + properties.getMaxMessageLength() + " caractères.");
        }
    }

    private boolean sameQuestion(String left, String right) {
        return left != null && right != null && normalizeQuestion(left).equals(normalizeQuestion(right));
    }

    private String normalizeQuestion(String question) {
        return question.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String catalogJson(ReportCatalog catalog) {
        try {
            return objectMapper.writeValueAsString(catalog);
        } catch (JsonProcessingException exception) {
            throw new BotLlmException("Le catalogue ne peut pas être sérialisé pour le modèle.");
        }
    }

}
