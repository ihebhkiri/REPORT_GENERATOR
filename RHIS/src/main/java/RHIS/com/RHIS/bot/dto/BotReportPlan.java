package RHIS.com.RHIS.bot.dto;

import java.util.List;

/**
 * Proposition structurée produite par le LLM : soit un rapport prêt (READY),
 * soit une demande de clarification (NEEDS_CLARIFICATION).
 */
public record BotReportPlan(
        String status,
        String question,
        String summary,
        Long rootDatasetId,
        List<Long> selectedFieldIds,
        List<PlanFilter> filters,
        List<PlanSort> sorts
) {
    public record PlanFilter(Long fieldId, String operator, List<String> values) {
    }

    public record PlanSort(Long fieldId, String direction) {
    }

    public boolean isReady() {
        return "READY".equalsIgnoreCase(status);
    }

    public boolean needsClarification() {
        return "NEEDS_CLARIFICATION".equalsIgnoreCase(status);
    }
}
