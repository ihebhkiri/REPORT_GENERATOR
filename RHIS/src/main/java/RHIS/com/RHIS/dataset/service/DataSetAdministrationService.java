package RHIS.com.RHIS.dataset.service;

import RHIS.com.RHIS.dataset.controller.dto.DataSetExposureConfigurationResponse;
import RHIS.com.RHIS.dataset.controller.dto.UpdateDataSetExposureRequest;
import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.exception.DataSetConfigurationException;
import RHIS.com.RHIS.dataset.repository.DataSetFieldRepository;
import RHIS.com.RHIS.dataset.repository.DataSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataSetAdministrationService {

    private static final Comparator<DataSetField> FIELD_ORDER = Comparator
            .comparing(DataSetField::getDisplayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(DataSetField::getId);

    private final DataSetRepository dataSetRepository;
    private final DataSetFieldRepository dataSetFieldRepository;

    @Transactional(readOnly = true)
    public DataSetExposureConfigurationResponse getConfiguration() {
        return toResponse(dataSetRepository.findAllByOrderByDisplayNameAsc());
    }

    @Transactional
    public DataSetExposureConfigurationResponse updateConfiguration(UpdateDataSetExposureRequest request) {
        rejectDuplicates(
                request.datasets().stream().map(UpdateDataSetExposureRequest.DataSetUpdate::id).toList(),
                "Une table ne peut être modifiée qu'une fois."
        );

        Set<Long> requestedDataSetIds = request.datasets().stream()
                .map(UpdateDataSetExposureRequest.DataSetUpdate::id)
                .collect(Collectors.toSet());
        Map<Long, DataSetEntity> dataSetsById = dataSetRepository.findAllById(requestedDataSetIds).stream()
                .collect(Collectors.toMap(DataSetEntity::getId, Function.identity()));
        if (dataSetsById.size() != requestedDataSetIds.size()) {
            throw new DataSetConfigurationException("Au moins une table demandée est inconnue.");
        }

        List<UpdateDataSetExposureRequest.FieldUpdate> requestedFields = request.datasets().stream()
                .flatMap(dataSet -> dataSet.fields().stream())
                .toList();
        rejectDuplicates(
                requestedFields.stream().map(UpdateDataSetExposureRequest.FieldUpdate::id).toList(),
                "Un champ ne peut être modifié qu'une fois."
        );
        Set<Long> requestedFieldIds = requestedFields.stream()
                .map(UpdateDataSetExposureRequest.FieldUpdate::id)
                .collect(Collectors.toSet());
        Map<Long, DataSetField> fieldsById = dataSetFieldRepository.findAllById(requestedFieldIds).stream()
                .collect(Collectors.toMap(DataSetField::getId, Function.identity()));
        if (fieldsById.size() != requestedFieldIds.size()) {
            throw new DataSetConfigurationException("Au moins un champ demandé est inconnu.");
        }

        validateRequest(request, dataSetsById, fieldsById);
        applyRequest(request, dataSetsById, fieldsById);
        dataSetFieldRepository.flush();
        dataSetRepository.flush();
        return toResponse(dataSetRepository.findAllByOrderByDisplayNameAsc());
    }

    private void validateRequest(
            UpdateDataSetExposureRequest request,
            Map<Long, DataSetEntity> dataSetsById,
            Map<Long, DataSetField> fieldsById
    ) {
        for (UpdateDataSetExposureRequest.DataSetUpdate dataSetUpdate : request.datasets()) {
            DataSetEntity dataSet = dataSetsById.get(dataSetUpdate.id());
            if (!dataSet.isActive()) {
                throw new DataSetConfigurationException(
                        "La table " + dataSet.getDisplayName() + " est inactive et ne peut pas être modifiée."
                );
            }
            for (UpdateDataSetExposureRequest.FieldUpdate fieldUpdate : dataSetUpdate.fields()) {
                DataSetField field = fieldsById.get(fieldUpdate.id());
                if (!field.isActive()) {
                    throw new DataSetConfigurationException(
                            "Le champ " + field.getDisplayName() + " est inactif et ne peut pas être modifié."
                    );
                }
                if (!dataSet.getId().equals(field.getDataset().getId())) {
                    throw new DataSetConfigurationException(
                            "Le champ " + field.getDisplayName() + " n'appartient pas à la table indiquée."
                    );
                }
            }
        }
    }

    private void applyRequest(
            UpdateDataSetExposureRequest request,
            Map<Long, DataSetEntity> dataSetsById,
            Map<Long, DataSetField> fieldsById
    ) {
        for (UpdateDataSetExposureRequest.DataSetUpdate dataSetUpdate : request.datasets()) {
            DataSetEntity dataSet = dataSetsById.get(dataSetUpdate.id());
            dataSet.setDisplayMain(dataSetUpdate.displayMain());
            dataSet.setDisplayRelated(dataSetUpdate.displayRelated());
            for (UpdateDataSetExposureRequest.FieldUpdate fieldUpdate : dataSetUpdate.fields()) {
                fieldsById.get(fieldUpdate.id()).setVisible(fieldUpdate.visible());
            }
        }
    }

    private DataSetExposureConfigurationResponse toResponse(List<DataSetEntity> dataSets) {
        List<DataSetExposureConfigurationResponse.DataSetExposure> responseDataSets = dataSets.stream()
                .map(this::toDataSetResponse)
                .toList();
        return new DataSetExposureConfigurationResponse(responseDataSets);
    }

    private DataSetExposureConfigurationResponse.DataSetExposure toDataSetResponse(DataSetEntity dataSet) {
        List<DataSetField> orderedFields = dataSet.getDataSetFieldSet().stream()
                .sorted(FIELD_ORDER)
                .toList();
        List<DataSetExposureConfigurationResponse.FieldExposure> fields = orderedFields.stream()
                .map(field -> new DataSetExposureConfigurationResponse.FieldExposure(
                        field.getId(),
                        field.getDisplayName(),
                        field.isActive(),
                        field.isVisible()
                ))
                .toList();
        long visibleFieldCount = orderedFields.stream()
                .filter(DataSetField::isActive)
                .filter(DataSetField::isVisible)
                .count();
        return new DataSetExposureConfigurationResponse.DataSetExposure(
                dataSet.getId(),
                dataSet.getDisplayName(),
                dataSet.isActive(),
                dataSet.isDisplayMain(),
                dataSet.isDisplayRelated(),
                visibleFieldCount,
                fields
        );
    }

    private <T> void rejectDuplicates(Collection<T> values, String message) {
        if (new HashSet<>(values).size() != values.size()) {
            throw new DataSetConfigurationException(message);
        }
    }
}
