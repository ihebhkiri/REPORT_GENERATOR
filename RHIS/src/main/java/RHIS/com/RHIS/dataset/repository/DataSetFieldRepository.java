package RHIS.com.RHIS.dataset.repository;

import RHIS.com.RHIS.dataset.entity.DataSetField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DataSetFieldRepository extends JpaRepository<DataSetField, Long> {

    List<DataSetField> findByDataset_IdAndActiveTrueOrderByPositionAsc(Long datasetId);

    @Query("""
            SELECT field
            FROM DataSetField field
            WHERE field.dataset.id = :datasetId
              AND field.active = true
              AND field.visible = true
              AND field.dataset.active = true
            ORDER BY field.position
            """)
    List<DataSetField> findVisibleFieldsByDatasetId(@Param("datasetId") Long datasetId);

    List<DataSetField> findByDataset_IdOrderByPositionAsc(Long datasetId);

    @EntityGraph(attributePaths = "dataset")
    List<DataSetField> findByIdIn(Collection<Long> ids);
}
