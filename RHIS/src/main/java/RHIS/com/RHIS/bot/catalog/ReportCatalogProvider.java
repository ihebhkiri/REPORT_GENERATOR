package RHIS.com.RHIS.bot.catalog;

import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.repository.DataSetFieldRepository;
import RHIS.com.RHIS.dataset.repository.DataSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Fournit le catalogue compact (datasets actifs principaux + champs visibles supportés)
 * sérialisé ensuite en JSON pour guider le LLM.
 */
@Component
@RequiredArgsConstructor
public class ReportCatalogProvider {

    private final DataSetRepository dataSetRepository;
    private final DataSetFieldRepository dataSetFieldRepository;

    @Transactional(readOnly = true)
    public ReportCatalog buildCatalog() {
        List<CatalogDataset> roots = catalogDatasets(
                dataSetRepository.findByActiveTrueAndDisplayMainTrue());
        List<CatalogDataset> related = catalogDatasets(
                dataSetRepository.findByActiveTrueAndDisplayRelatedTrue());
        LinkedHashSet<CatalogRelation> relations = new LinkedHashSet<>();
        dataSetRepository.findVisibleTableRelations().forEach(relation -> relations.add(
                new CatalogRelation(relation.getSourceDatasetId(), relation.getTargetDatasetId())));
        return new ReportCatalog(roots, related, List.copyOf(relations));
    }

    private List<CatalogDataset> catalogDatasets(List<DataSetEntity> datasets) {
        return datasets.stream()
                .map(dataset -> new CatalogDataset(dataset.getId(), dataset.getDisplayName(),
                        visibleFields(dataset.getId()), dataset.getDescription(), aliases(dataset.getAliases())))
                .toList();
    }

    private List<CatalogField> visibleFields(Long datasetId) {
        return dataSetFieldRepository.findVisibleFieldsByDatasetId(datasetId).stream()
                .filter(field -> field.getDataType().isSupported())
                .map(this::toCatalogField)
                .toList();
    }

    private CatalogField toCatalogField(DataSetField field) {
        return new CatalogField(
                field.getId(),
                field.getDisplayName(),
                field.getDataType().name(),
                field.getDataType().supportedOperators().stream().map(Enum::name).toList(),
                field.getDescription(), aliases(field.getAliases()));
    }

    private List<String> aliases(String value) {
        return value == null ? List.of() : value.lines().map(String::strip)
                .filter(alias -> !alias.isEmpty()).toList();
    }
}
