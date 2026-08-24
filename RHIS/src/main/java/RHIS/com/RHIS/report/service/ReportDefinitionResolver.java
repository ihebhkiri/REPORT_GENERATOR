package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.dataset.controller.dto.TableRelationProjection;
import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.model.DataSetFieldType;
import RHIS.com.RHIS.dataset.model.FilterOperator;
import RHIS.com.RHIS.dataset.repository.DataSetFieldRepository;
import RHIS.com.RHIS.dataset.repository.DataSetRepository;
import RHIS.com.RHIS.report.controller.dto.ReportFilterRequest;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewRequest;
import RHIS.com.RHIS.report.controller.dto.ReportSortRequest;

import RHIS.com.RHIS.report.exception.ReportDefinitionUnavailableException;
import RHIS.com.RHIS.report.exception.ReportResourceNotFoundException;
import RHIS.com.RHIS.report.exception.ReportValidationException;
import RHIS.com.RHIS.report.exception.UnavailableReportElement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Résout et valide une définition publique à partir des métadonnées accessibles du catalogue.
 */
@Service
@RequiredArgsConstructor
public class ReportDefinitionResolver {

    private final DataSetRepository dataSetRepository;
    private final DataSetFieldRepository dataSetFieldRepository;


    /**
     * Valide l’ensemble de la demande dans une transaction en lecture seule avant toute construction SQL.
     */
    @Transactional(readOnly = true)
    public ResolvedReportDefinition resolve(ReportPreviewRequest request) {
        DataSetEntity rootDataset = findVisibleDataSet(request.rootDatasetId());
        rejectDuplicates(request.selectedFieldIds(), "Une colonne ne peut être sélectionnée qu'une fois.");
        Set<Long> requestedFieldIds = collectRequestedFieldIds(request);
        Map<Long, DataSetField> fieldsById = loadAndValidateAccessibleFields(requestedFieldIds);
        List<DataSetField> selectedFields = resolveSelectedFields(request.selectedFieldIds(), fieldsById);
        List<ResolvedFilter> filters = resolveFilters(request.filters(), fieldsById);
        List<ResolvedSort> sorts = resolveSorts(request.sorts(), fieldsById, request.selectedFieldIds());
        List<DataSetField> referencedReportFields = referencedFields(selectedFields, filters);
        List<ResolvedJoin> joins = resolveJoins(rootDataset, referencedReportFields);
        validateReferencedDatasets(rootDataset, referencedReportFields, joins);
        List<DataSetField> primaryKeyFields = loadRootPrimaryKeyFields(rootDataset.getId());
        ResolvedReportDefinition definition = new ResolvedReportDefinition(
                rootDataset,
                selectedFields,
                filters,
                sorts,
                joins,
                primaryKeyFields
        );

        return definition;
    }

    // HELPERS
    private DataSetEntity findVisibleDataSet(Long dataSetId) {
        DataSetEntity dataSet = dataSetRepository.findById(dataSetId)
                .orElseThrow(() -> new ReportResourceNotFoundException(
                        "Le dataset demandé est introuvable."
                ));
        if (!dataSet.isActive() || !dataSet.isDisplayMain()) {
            throw unavailable("DATASET", dataSet, "MAIN_NOT_AVAILABLE");
        }
        return dataSet;
    }

    /**
     * Réunit les champs sélectionnés, filtrés et triés afin que chaque référence client soit chargée
     * et soumise aux mêmes contrôles d’accès avant la résolution du rapport.
     */
    private Set<Long> collectRequestedFieldIds(ReportPreviewRequest request) {
        Set<Long> requestedFieldIds = new LinkedHashSet<>(request.selectedFieldIds());
        request.filters().stream()
                .map(ReportFilterRequest::fieldId)
                .forEach(requestedFieldIds::add);
        request.sorts().stream()
                .map(ReportSortRequest::fieldId)
                .forEach(requestedFieldIds::add);
        return requestedFieldIds;
    }

    /**
     * Exige que tous les champs demandés existent, soient actifs et visibles avec leur dataset, et
     * utilisent un type pris en charge avant de les indexer par identifiant.
     */
    private Map<Long, DataSetField> loadAndValidateAccessibleFields(Set<Long> fieldIds) {
        Map<Long, DataSetField> fieldsById = dataSetFieldRepository.findByIdIn(fieldIds).stream()
                .collect(Collectors.toMap(DataSetField::getId, Function.identity()));

        List<UnavailableReportElement> unavailableElements = new ArrayList<>();
        fieldIds.stream()
                .filter(id -> !fieldsById.containsKey(id))
                .map(id -> new UnavailableReportElement("FIELD", id, null, "NOT_FOUND"))
                .forEach(unavailableElements::add);
        for (DataSetField field : fieldsById.values()) {
            if (!field.isActive() || !field.isVisible() || !field.getDataset().isActive()) {
                unavailableElements.add(new UnavailableReportElement(
                        "FIELD",
                        field.getId(),
                        field.getDisplayName(),
                        !field.getDataset().isActive() ? "DATASET_INACTIVE" : "FIELD_NOT_AVAILABLE"
                ));
            }
        }
        if (!unavailableElements.isEmpty()) {
            throw new ReportDefinitionUnavailableException(unavailableElements);
        }
        for (DataSetField field : fieldsById.values()) {
            if (!field.getDataType().isSupported()) {
                throw new ReportValidationException(
                        "Le champ " + field.getDisplayName() + " utilise un type non supporté."
                );
            }
        }
        return fieldsById;
    }

    /**
     * Charge les champs actifs de clé primaire dans l’ordre du catalogue pour fournir au constructeur
     * SQL un tri stable lorsque la demande ne contient aucun tri explicite.
     */
    private List<DataSetField> loadRootPrimaryKeyFields(Long rootDatasetId) {
        return dataSetFieldRepository
                .findByDataset_IdAndActiveTrueOrderByPositionAsc(rootDatasetId)
                .stream()
                .filter(DataSetField::isPrimaryKey)
                .toList();
    }

    private List<DataSetField> resolveSelectedFields(
            List<Long> selectedFieldIds,
            Map<Long, DataSetField> fieldsById
    ) {
        return selectedFieldIds.stream()
                .map(fieldsById::get)
                .toList();
    }

    private List<ResolvedFilter> resolveFilters(
            List<ReportFilterRequest> filterRequests,
            Map<Long, DataSetField> fieldsById
    ) {
        List<ResolvedFilter> filters = new ArrayList<>();
        for (ReportFilterRequest filterRequest : filterRequests) {
            filters.add(resolveFilter(filterRequest, fieldsById));
        }
        return List.copyOf(filters);
    }

    /**
     * Vérifie la compatibilité et l’arité de l’opérateur, puis convertit les valeurs textuelles dans
     * le type Java déclaré par les métadonnées du champ.
     */
    private ResolvedFilter resolveFilter(
            ReportFilterRequest filterRequest,
            Map<Long, DataSetField> fieldsById
    ) {
        DataSetField field = fieldsById.get(filterRequest.fieldId());
        if (!field.getDataType().supportedOperators().contains(filterRequest.operator())) {
            throw new ReportValidationException(
                    "L'opérateur " + filterRequest.operator()
                            + " n'est pas compatible avec " + field.getDisplayName() + "."
            );
        }

        validateValueCount(filterRequest.operator(), filterRequest.values());
        List<Object> parsedValues = filterRequest.values().stream()
                .map(value -> parseValue(field.getDataType(), value, field.getDisplayName()))
                .toList();
        return new ResolvedFilter(field, filterRequest.operator(), parsedValues);
    }

    /**
     * Préserve l’ordre des tris demandé tout en interdisant les doublons et les champs absents de la
     * sélection de sortie.
     */
    private List<ResolvedSort> resolveSorts(
            List<ReportSortRequest> sortRequests,
            Map<Long, DataSetField> fieldsById,
            List<Long> selectedFieldIds
    ) {
        List<Long> sortFieldIds = sortRequests.stream()
                .map(ReportSortRequest::fieldId)
                .toList();
        rejectDuplicates(sortFieldIds, "Un champ ne peut apparaître qu'une fois dans le tri.");

        Set<Long> selectedIds = Set.copyOf(selectedFieldIds);
        List<ResolvedSort> sorts = new ArrayList<>();
        for (ReportSortRequest sortRequest : sortRequests) {
            sorts.add(resolveSort(sortRequest, fieldsById, selectedIds));
        }
        return List.copyOf(sorts);
    }

    private ResolvedSort resolveSort(
            ReportSortRequest sortRequest,
            Map<Long, DataSetField> fieldsById,
            Set<Long> selectedFieldIds
    ) {
        if (!selectedFieldIds.contains(sortRequest.fieldId())) {
            throw new ReportValidationException(
                    "Le tri est limité aux colonnes sélectionnées."
            );
        }
        return new ResolvedSort(fieldsById.get(sortRequest.fieldId()), sortRequest.direction());
    }

    /**
     * Détermine les champs qui imposent une jointure à partir de la sélection et des filtres. Les tris
     * n’ajoutent aucune référence puisqu’ils sont déjà limités aux champs sélectionnés.
     */
    private List<DataSetField> referencedFields(
            List<DataSetField> selectedFields,
            List<ResolvedFilter> filters
    ) {
        Map<Long, DataSetField> referenced = new HashMap<>();
        selectedFields.forEach(field -> referenced.put(field.getId(), field));
        filters.forEach(filter -> referenced.put(filter.field().getId(), filter.field()));
        return List.copyOf(referenced.values());
    }

    /**
     * Résout chaque dataset référencé hors racine par une relation directe sortante et trie les cibles
     * par identifiant afin de stabiliser l’ordre des jointures et des alias SQL.
     */

    private List<ResolvedJoin> resolveJoins(
            DataSetEntity rootDataset,
            List<DataSetField> referencedFields
    ) {
        Map<Long, DataSetEntity> targetDataSets = referencedFields.stream()
                .map(DataSetField::getDataset)
                .filter(dataSet -> !dataSet.getId().equals(rootDataset.getId()))
                .collect(Collectors.toMap(
                        DataSetEntity::getId,
                        Function.identity(),
                        (left, right) -> left
                ));
        List<TableRelationProjection> relations = dataSetRepository.findVisibleTableRelations();
        return targetDataSets.values().stream()
                .sorted(Comparator.comparing(DataSetEntity::getId))
                .map(target -> resolveJoin(rootDataset, target, relations))
                .toList();
    }

    /**
     * Résout une contrainte de clé étrangère sortante vers le jeu de données cible. Les colonnes
     * d’une clé composite conservent l’ordre de position du catalogue ; l’absence de contrainte ou
     * plusieurs contraintes correspondantes sont rejetées comme ambiguës.
     */
    private ResolvedJoin resolveJoin(
            DataSetEntity rootDataset,
            DataSetEntity targetDataset,
            List<TableRelationProjection> relations
    ) {
        Map<String, List<TableRelationProjection>> candidates = relations.stream()
                .filter(relation -> rootDataset.getId().equals(relation.getSourceDatasetId()))
                .filter(relation -> targetDataset.getId().equals(relation.getTargetDatasetId()))
                .collect(Collectors.groupingBy(TableRelationProjection::getConstraintName));

        if (candidates.isEmpty()) {
            throw unavailable("DATASET", targetDataset, "RELATION_NOT_AVAILABLE");
        }
        if (candidates.size() > 1) {
            throw new ReportValidationException(
                    "Plusieurs relations directes existent vers "
                            + targetDataset.getDisplayName() + "."
            );
        }

        List<ResolvedJoinColumn> columns = candidates.values().iterator().next().stream()
                .sorted(Comparator.comparing(TableRelationProjection::getPosition))
                .map(relation -> new ResolvedJoinColumn(
                        relation.getSourceColumn(),
                        relation.getTargetColumn()
                ))
                .toList();
        return new ResolvedJoin(targetDataset, columns);
    }

    /**
     * Vérifie défensivement qu’après résolution chaque champ appartient à la racine ou à une cible
     * effectivement autorisée par une jointure résolue.
     */
    private void validateReferencedDatasets(
            DataSetEntity rootDataset,
            List<DataSetField> fields,
            List<ResolvedJoin> joins
    ) {
        Set<Long> allowedDataSetIds = new HashSet<>();
        allowedDataSetIds.add(rootDataset.getId());
        joins.forEach(join -> allowedDataSetIds.add(join.targetDataset().getId()));

        boolean hasUnexpectedDataSet = fields.stream()
                .map(field -> field.getDataset().getId())
                .anyMatch(dataSetId -> !allowedDataSetIds.contains(dataSetId));
        if (hasUnexpectedDataSet) {
            throw new ReportValidationException("La définition référence un dataset non autorisé.");
        }
    }

    private void validateValueCount(FilterOperator operator, List<String> values) {
        int expectedValueCount = switch (operator) {
            case BETWEEN -> 2;
            default -> 1;
        };
        if (values.size() != expectedValueCount) {
            throw new ReportValidationException(
                    "L'opérateur " + operator + " attend " + expectedValueCount + " valeur(s)."
            );
        }
    }

    /**
     * Convertit la représentation textuelle reçue en valeur Java correspondant au type catalogué du
     * champ afin que le constructeur SQL manipule des paramètres JDBC déjà typés.
     */
    private Object parseValue(DataSetFieldType type, String value, String fieldName) {
        try {
            return switch (type) {
                case TEXT -> value;
                case INTEGER -> Long.valueOf(value);
                case DECIMAL -> new BigDecimal(value);
                case BOOLEAN -> parseBoolean(value);
                case DATE -> LocalDate.parse(value);
                case TIME -> LocalTime.parse(value);
                case DATE_TIME -> LocalDateTime.parse(value);
                case OFFSET_DATE_TIME -> OffsetDateTime.parse(value);
                case UUID -> UUID.fromString(value);
                case UNSUPPORTED -> throw new ReportValidationException(
                        "Le champ " + fieldName + " utilise un type non supporté."
                );
            };
        } catch (NumberFormatException | DateTimeParseException exception) {
            throw new ReportValidationException(
                    "La valeur '" + value + "' n'est pas valide pour " + fieldName + "."
            );
        }
    }

    private Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        throw new ReportValidationException("Une valeur booléenne doit être true ou false.");
    }

    private <T> void rejectDuplicates(Collection<T> values, String message) {
        if (new HashSet<>(values).size() != values.size()) {
            throw new ReportValidationException(message);
        }
    }

    private ReportDefinitionUnavailableException unavailable(
            String kind,
            DataSetEntity dataSet,
            String reason
    ) {
        return new ReportDefinitionUnavailableException(List.of(
                new UnavailableReportElement(kind, dataSet.getId(), dataSet.getDisplayName(), reason)
        ));
    }
}
