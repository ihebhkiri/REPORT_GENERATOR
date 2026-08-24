package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.model.FilterOperator;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewColumnResponse;
import RHIS.com.RHIS.report.controller.dto.SortDirection;

import java.util.List;

/**
 * Définition de rapport issue du catalogue et produite après validation des champs, filtres, tris et
 * jointures. Les identifiants de champs fournis par le client sont résolus et les valeurs textuelles
 * des filtres sont déjà converties.
 */
record ResolvedReportDefinition(
        DataSetEntity rootDataset,
        List<DataSetField> selectedFields,
        List<ResolvedFilter> filters,
        List<ResolvedSort> sorts,
        List<ResolvedJoin> joins,
        List<DataSetField> rootPrimaryKeyFields
) {
}

/** Filtre dont les valeurs ont été converties vers le type Java déclaré par les métadonnées. */
record ResolvedFilter(
        DataSetField field,
        FilterOperator operator,
        List<Object> values
) {
}

/** Tri dont le champ client a été résolu vers les métadonnées accessibles du catalogue. */
record ResolvedSort(
        DataSetField field,
        SortDirection direction
) {
}

/** Jointure directe validée vers un dataset cible, avec ses colonnes dans l’ordre du catalogue. */
record ResolvedJoin(
        DataSetEntity targetDataset,
        List<ResolvedJoinColumn> columns
) {
}

/** Paire de colonnes source et cible constituant une position de la clé étrangère résolue. */
record ResolvedJoinColumn(
        String sourceColumn,
        String targetColumn
) {
}

/**
 * Transmission immuable entre la construction SQL et l’exécution JDBC. L’ordre des paramètres
 * correspond aux marqueurs SQL et celui des colonnes aux alias produits par la clause
 * {@code SELECT}.
 */
record PreparedReportQuery(
        String sql,
        List<Object> parameters,
        List<ReportPreviewColumnResponse> columns
) {
}

/** Requête de comptage paramétrée issue de la même définition logique que la lecture complète. */
record PreparedCountQuery(
        String sql,
        List<Object> parameters
) {
}
