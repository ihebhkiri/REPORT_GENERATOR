package RHIS.com.RHIS.dataset.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateDataSetExposureRequest(
        @NotEmpty List<@Valid DataSetUpdate> datasets
) {
    public record DataSetUpdate(
            @NotNull Long id,
            @NotNull Boolean displayMain,
            @NotNull Boolean displayRelated,
            @NotNull List<@Valid FieldUpdate> fields,
            @Size(max = 1000) String description,
            @Size(max = 2000) String aliases
    ) {
    }

    public record FieldUpdate(
            @NotNull Long id,
            @NotNull Boolean visible,
            @Size(max = 1000) String description,
            @Size(max = 2000) String aliases
    ) {
    }
}
