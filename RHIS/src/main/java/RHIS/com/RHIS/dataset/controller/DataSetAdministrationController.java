package RHIS.com.RHIS.dataset.controller;

import RHIS.com.RHIS.dataset.controller.dto.DataSetExposureConfigurationResponse;
import RHIS.com.RHIS.dataset.controller.dto.UpdateDataSetExposureRequest;
import RHIS.com.RHIS.dataset.service.DataSetAdministrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dataset-exposure")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DataSetAdministrationController {

    private final DataSetAdministrationService administrationService;

    @GetMapping
    public DataSetExposureConfigurationResponse getConfiguration() {
        return administrationService.getConfiguration();
    }

    @PutMapping
    public DataSetExposureConfigurationResponse updateConfiguration(
            @Valid @RequestBody UpdateDataSetExposureRequest request
    ) {
        return administrationService.updateConfiguration(request);
    }
}
