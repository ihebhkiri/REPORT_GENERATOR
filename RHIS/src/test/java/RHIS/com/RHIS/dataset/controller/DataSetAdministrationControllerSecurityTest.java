package RHIS.com.RHIS.dataset.controller;

import RHIS.com.RHIS.auth.CustomUserDetailService;
import RHIS.com.RHIS.auth.JwtCookieFilter;
import RHIS.com.RHIS.auth.JwtService;
import RHIS.com.RHIS.auth.SecurityConfig;
import RHIS.com.RHIS.dataset.controller.dto.DataSetExposureConfigurationResponse;
import RHIS.com.RHIS.dataset.service.DataSetAdministrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataSetAdministrationController.class)
@Import({SecurityConfig.class, JwtCookieFilter.class, DataSetAdministrationExceptionHandler.class})
class DataSetAdministrationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSetAdministrationService administrationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    @Test
    void rejectsUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dataset-exposure"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAuthenticatedNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dataset-exposure")
                        .with(user("reader").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsConfigurationToAdmin() throws Exception {
        when(administrationService.getConfiguration()).thenReturn(
                new DataSetExposureConfigurationResponse(List.of(
                        new DataSetExposureConfigurationResponse.DataSetExposure(
                                1L,
                                "Employés",
                                true,
                                true,
                                false,
                                1,
                                List.of(new DataSetExposureConfigurationResponse.FieldExposure(
                                        11L,
                                        "Nom",
                                        true,
                                        true
                                , "", ""))
                        , "", "")
                ))
        );

        mockMvc.perform(get("/api/v1/admin/dataset-exposure")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasets[0].displayName").value("Employés"))
                .andExpect(jsonPath("$.datasets[0].fields[0].displayName").value("Nom"))
                .andExpect(jsonPath("$.datasets[0].sourceName").doesNotExist());
    }

    @Test
    void rejectsAnInvalidAdminPayloadWithoutCallingTheService() throws Exception {
        mockMvc.perform(put("/api/v1/admin/dataset-exposure")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "datasets": [{
                                    "id": 1,
                                    "displayMain": null,
                                    "displayRelated": false,
                                    "fields": []
                                  }]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Configuration des datasets invalide"));
    }
}
