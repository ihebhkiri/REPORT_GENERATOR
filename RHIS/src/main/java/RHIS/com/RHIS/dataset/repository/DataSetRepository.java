package RHIS.com.RHIS.dataset.repository;

import RHIS.com.RHIS.dataset.controller.dto.TableRelationProjection;
import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DataSetRepository extends JpaRepository<DataSetEntity, Long> {

    Optional<DataSetEntity> findBySourceName(String sourceName);

    @EntityGraph(attributePaths = "dataSetFieldSet")
    List<DataSetEntity> findByActiveTrueAndDisplayMainTrue();

    @EntityGraph(attributePaths = "dataSetFieldSet")
    List<DataSetEntity> findAllByOrderByDisplayNameAsc();


    @Query(value = """
        SELECT DISTINCT
            tc.constraint_name AS "constraintName",
            source_kcu.ordinal_position AS "position",
            source_dataset.id AS "sourceDatasetId",
            source_kcu.table_name AS "sourceTable",
            source_dataset.display_name AS "sourceDisplayName",
            source_kcu.column_name AS "sourceColumn",
            target_dataset.id AS "targetDatasetId",
            target_kcu.table_name AS "targetTable",
            target_dataset.display_name AS "targetDisplayName",
            target_kcu.column_name AS "targetColumn"
        FROM information_schema.table_constraints tc
        JOIN information_schema.referential_constraints rc
            ON rc.constraint_catalog = tc.constraint_catalog
            AND rc.constraint_schema = tc.constraint_schema
            AND rc.constraint_name = tc.constraint_name
        JOIN information_schema.key_column_usage source_kcu
            ON source_kcu.constraint_catalog = tc.constraint_catalog
            AND source_kcu.constraint_schema = tc.constraint_schema
            AND source_kcu.constraint_name = tc.constraint_name
            AND source_kcu.table_schema = tc.table_schema
            AND source_kcu.table_name = tc.table_name
        JOIN information_schema.key_column_usage target_kcu
            ON target_kcu.constraint_catalog = rc.unique_constraint_catalog
            AND target_kcu.constraint_schema = rc.unique_constraint_schema
            AND target_kcu.constraint_name = rc.unique_constraint_name
            AND target_kcu.ordinal_position = source_kcu.position_in_unique_constraint
        JOIN datasets source_dataset
            ON source_dataset.source_name = source_kcu.table_name
            AND source_dataset.active = true
        JOIN datasets target_dataset
            ON target_dataset.source_name = target_kcu.table_name
            AND target_dataset.active = true
            AND target_dataset.display_related = true
        WHERE tc.constraint_type = 'FOREIGN KEY'
            AND tc.table_schema = 'public'
            AND source_kcu.table_schema = 'public'
            AND target_kcu.table_schema = 'public'
        ORDER BY "sourceTable", "constraintName", "position"
        """, nativeQuery = true)
    List<TableRelationProjection> findVisibleTableRelations();

}
