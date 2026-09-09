package RHIS.com.RHIS.bot.catalog;

import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.controller.dto.TableRelationProjection;
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
import static org.mockito.Mockito.mock;

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
        dataset.setDescription("Personnes employées par le restaurant.");
        dataset.setAliases("Salariés\nPersonnel");
        when(dataSetRepository.findByActiveTrueAndDisplayMainTrue()).thenReturn(List.of(dataset));
        DataSetField field = new DataSetField("Date d'embauche", "hire_date", 0, dataset);
        field.setId(10L);
        field.setDescription("Date de début d'emploi.");
        field.setAliases("Entrée\nDébut d'emploi");
        field.setDataType(DataSetFieldType.DATE);
        when(dataSetFieldRepository.findVisibleFieldsByDatasetId(1L)).thenReturn(List.of(field));

        when(dataSetRepository.findByActiveTrueAndDisplayRelatedTrue()).thenReturn(List.of());
        when(dataSetRepository.findVisibleTableRelations()).thenReturn(List.of());

        ReportCatalog catalog = provider.buildCatalog();

        assertEquals(1, catalog.rootDatasets().size());
        assertEquals(1L, catalog.rootDatasets().get(0).datasetId());
        assertEquals("Employés", catalog.rootDatasets().get(0).displayName());
        assertEquals(dataset.getDescription(), catalog.rootDatasets().get(0).description());
        assertEquals(List.of("Salariés", "Personnel"), catalog.rootDatasets().get(0).aliases());
        CatalogField catalogField = catalog.rootDatasets().get(0).fields().get(0);
        assertEquals(10L, catalogField.fieldId());
        assertEquals("DATE", catalogField.type());
        assertEquals(field.getDescription(), catalogField.description());
        assertEquals(List.of("Entrée", "Début d'emploi"), catalogField.aliases());
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

        when(dataSetRepository.findByActiveTrueAndDisplayRelatedTrue()).thenReturn(List.of());
        when(dataSetRepository.findVisibleTableRelations()).thenReturn(List.of());

        ReportCatalog catalog = provider.buildCatalog();

        assertTrue(catalog.rootDatasets().get(0).fields().isEmpty());
    }

    @Test
    void exposesRelatedDatasetsAndDeduplicatesCompositeRelations() {
        DataSetEntity root = new DataSetEntity("Employés", "employees");
        root.setId(1L);
        DataSetEntity related = new DataSetEntity("Contrats", "contracts");
        related.setId(2L);
        related.setDisplayRelated(true);
        when(dataSetRepository.findByActiveTrueAndDisplayMainTrue()).thenReturn(List.of(root));
        when(dataSetRepository.findByActiveTrueAndDisplayRelatedTrue()).thenReturn(List.of(related));
        when(dataSetFieldRepository.findVisibleFieldsByDatasetId(1L)).thenReturn(List.of());
        when(dataSetFieldRepository.findVisibleFieldsByDatasetId(2L)).thenReturn(List.of());
        TableRelationProjection first = relation(1L, 2L);
        TableRelationProjection second = relation(1L, 2L);
        when(dataSetRepository.findVisibleTableRelations()).thenReturn(List.of(first, second));

        ReportCatalog catalog = provider.buildCatalog();

        assertEquals(List.of(new CatalogRelation(1L, 2L)), catalog.relations());
        assertEquals(2L, catalog.relatedDatasets().get(0).datasetId());
    }

    private TableRelationProjection relation(Long sourceId, Long targetId) {
        TableRelationProjection relation = mock(TableRelationProjection.class);
        when(relation.getSourceDatasetId()).thenReturn(sourceId);
        when(relation.getTargetDatasetId()).thenReturn(targetId);
        return relation;
    }
}
