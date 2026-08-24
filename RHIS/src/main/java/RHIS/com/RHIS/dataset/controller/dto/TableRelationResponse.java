package RHIS.com.RHIS.dataset.controller.dto;

public record TableRelationResponse(
        Long sourceDatasetId,
        String sourceTable,
        String sourceDisplayName,
        String sourceColumn,
        Long targetDatasetId,
        String targetTable,
        String targetDisplayName,
        String targetColumn
) {
}
