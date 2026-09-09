package RHIS.com.RHIS.dataset.service;

import RHIS.com.RHIS.dataset.controller.dto.DataSetExposureConfigurationResponse;
import RHIS.com.RHIS.dataset.controller.dto.UpdateDataSetExposureRequest;
import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.exception.DataSetConfigurationException;
import RHIS.com.RHIS.dataset.repository.DataSetFieldRepository;
import RHIS.com.RHIS.dataset.repository.DataSetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSetAdministrationServiceTest {

    @Mock
    private DataSetRepository dataSetRepository;

    @Mock
    private DataSetFieldRepository dataSetFieldRepository;

    private DataSetAdministrationService service;

    @BeforeEach
    void setUp() {
        service = new DataSetAdministrationService(dataSetRepository, dataSetFieldRepository);
    }

    @Test
    void updatesTheCompleteValidBatchAndReturnsDisplayNamesOnly() {
        DataSetEntity dataSet = dataSet(1L, "Employés", "rhis_employee");
        DataSetField field = field(11L, "Nom", "employee_name", dataSet);
        dataSet.getDataSetFieldSet().add(field);

        when(dataSetRepository.findAllById(any())).thenReturn(List.of(dataSet));
        when(dataSetFieldRepository.findAllById(any())).thenReturn(List.of(field));
        when(dataSetRepository.findAllByOrderByDisplayNameAsc()).thenReturn(List.of(dataSet));

        DataSetExposureConfigurationResponse response = service.updateConfiguration(
                new UpdateDataSetExposureRequest(List.of(
                        new UpdateDataSetExposureRequest.DataSetUpdate(
                                1L,
                                false,
                                true,
                                List.of(new UpdateDataSetExposureRequest.FieldUpdate(11L, false, null, null))
                        , null, null)
                ))
        );

        assertThat(dataSet.isDisplayMain()).isFalse();
        assertThat(dataSet.isDisplayRelated()).isTrue();
        assertThat(field.isVisible()).isFalse();
        assertThat(response.datasets()).singleElement().satisfies(saved -> {
            assertThat(saved.displayName()).isEqualTo("Employés");
            assertThat(saved.visibleFieldCount()).isZero();
            assertThat(saved.fields()).singleElement()
                    .extracting(DataSetExposureConfigurationResponse.FieldExposure::displayName)
                    .isEqualTo("Nom");
        });
        verify(dataSetFieldRepository).flush();
        verify(dataSetRepository).flush();
    }

    @Test
    void rejectsAFieldAssignedToTheWrongDatasetBeforeAnyMutation() {
        DataSetEntity requestedDataSet = dataSet(1L, "Employés", "rhis_employee");
        DataSetEntity actualDataSet = dataSet(2L, "Contrats", "rhis_contract");
        DataSetField field = field(11L, "Type", "contract_type", actualDataSet);

        when(dataSetRepository.findAllById(any())).thenReturn(List.of(requestedDataSet));
        when(dataSetFieldRepository.findAllById(any())).thenReturn(List.of(field));

        UpdateDataSetExposureRequest request = new UpdateDataSetExposureRequest(List.of(
                new UpdateDataSetExposureRequest.DataSetUpdate(
                        1L,
                        false,
                        false,
                        List.of(new UpdateDataSetExposureRequest.FieldUpdate(11L, false, null, null))
                , null, null)
        ));

        assertThatThrownBy(() -> service.updateConfiguration(request))
                .isInstanceOf(DataSetConfigurationException.class)
                .hasMessageContaining("n'appartient pas");
        assertThat(requestedDataSet.isDisplayMain()).isTrue();
        assertThat(field.isVisible()).isTrue();
        verify(dataSetFieldRepository, never()).flush();
        verify(dataSetRepository, never()).flush();
    }

    @Test
    void rejectsDuplicateDatasetUpdates() {
        UpdateDataSetExposureRequest.DataSetUpdate update =
                new UpdateDataSetExposureRequest.DataSetUpdate(1L, true, false, List.of(), null, null);

        assertThatThrownBy(() -> service.updateConfiguration(
                new UpdateDataSetExposureRequest(List.of(update, update))
        ))
                .isInstanceOf(DataSetConfigurationException.class)
                .hasMessageContaining("table ne peut être modifiée qu'une fois");

        verify(dataSetRepository, never()).findAllById(any());
    }

    @Test
    void rejectsDuplicateFieldsAcrossDatasetsBeforeLoadingFieldsOrMutating() {
        DataSetEntity employees = dataSet(1L, "Employés", "rhis_employee");
        DataSetEntity contracts = dataSet(2L, "Contrats", "rhis_contract");
        when(dataSetRepository.findAllById(any())).thenReturn(List.of(employees, contracts));
        var repeatedField = new UpdateDataSetExposureRequest.FieldUpdate(11L, false, null, null);
        var request = new UpdateDataSetExposureRequest(List.of(
                new UpdateDataSetExposureRequest.DataSetUpdate(1L, false, false, List.of(repeatedField), null, null),
                new UpdateDataSetExposureRequest.DataSetUpdate(2L, false, false, List.of(repeatedField), null, null)
        ));

        assertThatThrownBy(() -> service.updateConfiguration(request))
                .isInstanceOf(DataSetConfigurationException.class)
                .hasMessage("Un champ ne peut être modifié qu'une fois.");
        assertThat(employees.isDisplayMain()).isTrue();
        assertThat(contracts.isDisplayMain()).isTrue();
        verify(dataSetFieldRepository, never()).findAllById(any());
    }

    @Test
    void rejectsUnknownDatasetBeforeLoadingFieldsOrMutatingKnownDataset() {
        DataSetEntity employees = dataSet(1L, "Employés", "rhis_employee");
        when(dataSetRepository.findAllById(any())).thenReturn(List.of(employees));
        var request = new UpdateDataSetExposureRequest(List.of(
                new UpdateDataSetExposureRequest.DataSetUpdate(1L, false, false, List.of(), null, null),
                new UpdateDataSetExposureRequest.DataSetUpdate(2L, false, false, List.of(), null, null)
        ));

        assertThatThrownBy(() -> service.updateConfiguration(request))
                .isInstanceOf(DataSetConfigurationException.class)
                .hasMessage("Au moins une table demandée est inconnue.");
        assertThat(employees.isDisplayMain()).isTrue();
        verify(dataSetFieldRepository, never()).findAllById(any());
    }

    @Test
    void rejectsUnknownFieldBeforeMutatingDatasetOrKnownField() {
        DataSetEntity employees = dataSet(1L, "Employés", "rhis_employee");
        DataSetField name = field(11L, "Nom", "employee_name", employees);
        when(dataSetRepository.findAllById(any())).thenReturn(List.of(employees));
        when(dataSetFieldRepository.findAllById(any())).thenReturn(List.of(name));
        var request = new UpdateDataSetExposureRequest(List.of(
                new UpdateDataSetExposureRequest.DataSetUpdate(1L, false, false, List.of(
                        new UpdateDataSetExposureRequest.FieldUpdate(11L, false, null, null),
                        new UpdateDataSetExposureRequest.FieldUpdate(12L, false, null, null)
                ), null, null)
        ));

        assertThatThrownBy(() -> service.updateConfiguration(request))
                .isInstanceOf(DataSetConfigurationException.class)
                .hasMessage("Au moins un champ demandé est inconnu.");
        assertThat(employees.isDisplayMain()).isTrue();
        assertThat(name.isVisible()).isTrue();
        verify(dataSetFieldRepository, never()).flush();
        verify(dataSetRepository, never()).flush();
    }

    @Test
    void savesNormalizesAndClearsBusinessMetadataWithoutErasingOmittedValues() {
        DataSetEntity employees = dataSet(1L, "Employés", "rhis_employee");
        DataSetField name = field(11L, "Nom", "name", employees);
        employees.getDataSetFieldSet().add(name);
        when(dataSetRepository.findAllById(any())).thenReturn(List.of(employees));
        when(dataSetFieldRepository.findAllById(any())).thenReturn(List.of(name));
        when(dataSetRepository.findAllByOrderByDisplayNameAsc()).thenReturn(List.of(employees));
        var response = service.updateConfiguration(new UpdateDataSetExposureRequest(List.of(
                new UpdateDataSetExposureRequest.DataSetUpdate(1L, true, false,
                        List.of(new UpdateDataSetExposureRequest.FieldUpdate(11L, true,
                                " Nom de famille ", " Patronyme\npatronyme\nNom familial ")),
                        " Personnel du restaurant ", " Salariés\r\nPersonnel\nSALARIÉS\n "))));
        assertThat(response.datasets().get(0).description()).isEqualTo("Personnel du restaurant");
        assertThat(response.datasets().get(0).aliases()).isEqualTo("Salariés\nPersonnel");
        assertThat(response.datasets().get(0).fields().get(0).aliases()).isEqualTo("Patronyme\nNom familial");
        service.updateConfiguration(new UpdateDataSetExposureRequest(List.of(
                new UpdateDataSetExposureRequest.DataSetUpdate(1L, true, false,
                        List.of(new UpdateDataSetExposureRequest.FieldUpdate(11L, true, "", "")), null, null))));
        assertThat(employees.getDescription()).isEqualTo("Personnel du restaurant");
        assertThat(employees.getAliases()).isEqualTo("Salariés\nPersonnel");
        assertThat(name.getDescription()).isEmpty();
        assertThat(name.getAliases()).isEmpty();
    }

    @Test
    void rejectsInvalidMetadataBeforeMutatingTheBatch() {
        DataSetEntity employees = dataSet(1L, "Employés", "rhis_employee");
        when(dataSetRepository.findAllById(any())).thenReturn(List.of(employees));
        for (String aliases : List.of("x".repeat(101), java.util.stream.IntStream.range(0, 21)
                .mapToObj(i -> "Alias " + i).collect(java.util.stream.Collectors.joining("\n")))) {
            var request = new UpdateDataSetExposureRequest(List.of(
                    new UpdateDataSetExposureRequest.DataSetUpdate(1L, false, false, List.of(), "Texte", aliases)));
            assertThatThrownBy(() -> service.updateConfiguration(request))
                    .isInstanceOf(DataSetConfigurationException.class);
            assertThat(employees.isDisplayMain()).isTrue();
            assertThat(employees.getDescription()).isEmpty();
        }
    }

    private DataSetEntity dataSet(Long id, String displayName, String sourceName) {
        DataSetEntity dataSet = new DataSetEntity(displayName, sourceName);
        dataSet.setId(id);
        dataSet.setActive(true);
        return dataSet;
    }

    private DataSetField field(Long id, String displayName, String sourceName, DataSetEntity dataSet) {
        DataSetField field = new DataSetField(displayName, sourceName, 1, dataSet);
        field.setId(id);
        return field;
    }
}
