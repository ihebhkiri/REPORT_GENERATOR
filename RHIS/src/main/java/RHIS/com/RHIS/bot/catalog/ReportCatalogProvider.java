package RHIS.com.RHIS.bot.catalog;

import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.repository.DataSetFieldRepository;
import RHIS.com.RHIS.dataset.repository.DataSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    public List<CatalogDataset> buildCatalog() {
        return dataSetRepository.findByActiveTrueAndDisplayMainTrue().stream()
                .map(dataset -> new CatalogDataset(
                        dataset.getId(),
                        dataset.getDisplayName(),
                        visibleFields(dataset.getId())))
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
                field.getDataType().supportedOperators().stream().map(Enum::name).toList());
    }
}
