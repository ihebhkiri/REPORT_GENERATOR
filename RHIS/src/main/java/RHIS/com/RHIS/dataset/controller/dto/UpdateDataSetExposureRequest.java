package RHIS.com.RHIS.dataset.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateDataSetExposureRequest(
        @NotEmpty List<@Valid DataSetUpdate> datasets
) {
    public record DataSetUpdate(
            @NotNull Long id,
            @NotNull Boolean displayMain,
            @NotNull Boolean displayRelated,
            @NotNull List<@Valid FieldUpdate> fields
    ) {
    }

    public record FieldUpdate(
            @NotNull Long id,
            @NotNull Boolean visible
    ) {
    }
}
