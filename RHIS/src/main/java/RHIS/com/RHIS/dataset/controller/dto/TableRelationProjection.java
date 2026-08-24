package RHIS.com.RHIS.dataset.controller.dto;

public interface TableRelationProjection {
    String getConstraintName();

    Integer getPosition();

    Long getSourceDatasetId();

    String getSourceTable();

    String getSourceDisplayName();

    String getSourceColumn();

    Long getTargetDatasetId();

    String getTargetTable();

    String getTargetDisplayName();

    String getTargetColumn();
}
