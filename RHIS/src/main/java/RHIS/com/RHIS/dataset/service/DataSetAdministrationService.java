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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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
        List<Long> requestedDataSetIds = request.datasets().stream()
                .map(UpdateDataSetExposureRequest.DataSetUpdate::id)
                .toList();
        rejectDuplicates(
                requestedDataSetIds,
                "Une table ne peut être modifiée qu'une fois."
        );

        Map<Long, DataSetEntity> dataSetsById = dataSetRepository.findAllById(requestedDataSetIds).stream()
                .collect(Collectors.toMap(DataSetEntity::getId, Function.identity()));
        if (dataSetsById.size() != requestedDataSetIds.size()) {
            throw new DataSetConfigurationException("Au moins une table demandée est inconnue.");
        }

        List<Long> requestedFieldIds = request.datasets().stream()
                .flatMap(dataSet -> dataSet.fields().stream())
                .map(UpdateDataSetExposureRequest.FieldUpdate::id)
                .toList();
        rejectDuplicates(
                requestedFieldIds,
                "Un champ ne peut être modifié qu'une fois."
        );
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
            validateMetadata(dataSetUpdate.description(), dataSetUpdate.aliases());
            if (!dataSet.isActive()) {
                throw new DataSetConfigurationException(
                        "La table " + dataSet.getDisplayName() + " est inactive et ne peut pas être modifiée."
                );
            }
            for (UpdateDataSetExposureRequest.FieldUpdate fieldUpdate : dataSetUpdate.fields()) {
                DataSetField field = fieldsById.get(fieldUpdate.id());
                validateMetadata(fieldUpdate.description(), fieldUpdate.aliases());
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
            if (dataSetUpdate.description() != null) {
                dataSet.setDescription(dataSetUpdate.description().strip());
            }
            if (dataSetUpdate.aliases() != null) {
                dataSet.setAliases(normalizeAliases(dataSetUpdate.aliases()));
            }
            for (UpdateDataSetExposureRequest.FieldUpdate fieldUpdate : dataSetUpdate.fields()) {
                DataSetField field = fieldsById.get(fieldUpdate.id());
                field.setVisible(fieldUpdate.visible());
                if (fieldUpdate.description() != null) {
                    field.setDescription(fieldUpdate.description().strip());
                }
                if (fieldUpdate.aliases() != null) {
                    field.setAliases(normalizeAliases(fieldUpdate.aliases()));
                }
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
                        field.isVisible(),
                        field.getDescription(),
                        field.getAliases()
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
                fields,
                dataSet.getDescription(),
                dataSet.getAliases()
        );
    }

    private void validateMetadata(String description, String aliases) {
        if (description != null && description.length() > 1000
                || aliases != null && aliases.length() > 2000) {
            throw new DataSetConfigurationException("Description : 1000 caractères maximum ; alias : 2000 caractères maximum.");
        }
        if (aliases != null) {
            List<String> values = normalizeAliases(aliases).lines().toList();
            if (values.size() > 20 || values.stream().anyMatch(value -> value.length() > 100)) {
                throw new DataSetConfigurationException("Utilisez au maximum 20 alias de 100 caractères chacun, un par ligne.");
            }
        }
    }

    private String normalizeAliases(String aliases) {
        Map<String, String> unique = new LinkedHashMap<>();
        aliases.lines().map(String::strip).filter(value -> !value.isEmpty())
                .forEach(value -> unique.putIfAbsent(value.toLowerCase(Locale.ROOT), value));
        return String.join("\n", unique.values());
    }

    private <T> void rejectDuplicates(Collection<T> values, String message) {
        if (new HashSet<>(values).size() != values.size()) {
            throw new DataSetConfigurationException(message);
        }
    }
}
