package RHIS.com.RHIS.bot.catalog;

import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.model.DataSetFieldType;
import RHIS.com.RHIS.dataset.repository.DataSetFieldRepository;
import RHIS.com.RHIS.dataset.repository.DataSetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportCatalogProviderTest {

    @Mock
    private DataSetRepository dataSetRepository;
    @Mock
    private DataSetFieldRepository dataSetFieldRepository;
    @InjectMocks
    private ReportCatalogProvider provider;

    @Test
    void buildsCatalogFromActiveMainDatasetsAndVisibleFields() {
        DataSetEntity dataset = new DataSetEntity("Employés", "employees");
        dataset.setId(1L);
        when(dataSetRepository.findByActiveTrueAndDisplayMainTrue()).thenReturn(List.of(dataset));
        DataSetField field = new DataSetField("Date d'embauche", "hire_date", 0, dataset);
        field.setId(10L);
        field.setDataType(DataSetFieldType.DATE);
        when(dataSetFieldRepository.findVisibleFieldsByDatasetId(1L)).thenReturn(List.of(field));

        List<CatalogDataset> catalog = provider.buildCatalog();

        assertEquals(1, catalog.size());
        assertEquals(1L, catalog.get(0).datasetId());
        assertEquals("Employés", catalog.get(0).displayName());
        CatalogField catalogField = catalog.get(0).fields().get(0);
        assertEquals(10L, catalogField.fieldId());
        assertEquals("DATE", catalogField.type());
        assertTrue(catalogField.operators().contains("BETWEEN"));
    }

    @Test
    void excludesUnsupportedFieldTypes() {
        DataSetEntity dataset = new DataSetEntity("Employés", "employees");
        dataset.setId(1L);
        when(dataSetRepository.findByActiveTrueAndDisplayMainTrue()).thenReturn(List.of(dataset));
        DataSetField unsupported = new DataSetField("Colonne binaire", "payload", 0, dataset);
        unsupported.setDataType(DataSetFieldType.UNSUPPORTED);
        when(dataSetFieldRepository.findVisibleFieldsByDatasetId(1L))
                .thenReturn(List.of(unsupported));

        List<CatalogDataset> catalog = provider.buildCatalog();

        assertTrue(catalog.get(0).fields().isEmpty());
    }
}
