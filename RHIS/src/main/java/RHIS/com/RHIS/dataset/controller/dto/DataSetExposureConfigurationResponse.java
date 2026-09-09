package RHIS.com.RHIS.dataset.controller.dto;

import java.util.List;

public record DataSetExposureConfigurationResponse(
        List<DataSetExposure> datasets
) {
    public record DataSetExposure(
            Long id,
            String displayName,
            boolean active,
            boolean displayMain,
            boolean displayRelated,
            long visibleFieldCount,
            List<FieldExposure> fields,
            String description,
            String aliases
    ) {
    }

    public record FieldExposure(
            Long id,
            String displayName,
            boolean active,
            boolean visible,
            String description,
            String aliases
    ) {
    }
}
