package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewColumnResponse;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Construit une requête d’aperçu PostgreSQL paramétrée et les métadonnées de résultat nécessaires à
 * son exécution JDBC à partir d’une définition validée issue du catalogue.
 */
@Component
class ReportSqlBuilder {

    private static final String PRODUCT_SCHEMA = "public";
    private static final int FETCH_LIMIT = 7;

    /**
     * Assemble dans un ordre déterministe la sélection, les jointures, les filtres, le tri et la
     * limite de sept lignes, puis aligne les paramètres et colonnes avec le SQL produit.
     */
    PreparedReportQuery buildPreview(ResolvedReportDefinition definition) {
        return buildDataQuery(definition, true);
    }

    PreparedReportQuery buildFull(ResolvedReportDefinition definition) {
        return buildDataQuery(definition, false);
    }

    PreparedCountQuery buildCount(ResolvedReportDefinition definition) {
        Map<Long, String> aliases = createAliases(definition);
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*)");

        appendFrom(sql, definition, aliases);
        appendFilters(sql, definition.filters(), aliases, parameters);

        return new PreparedCountQuery(sql.toString(), List.copyOf(parameters));
    }

    private PreparedReportQuery buildDataQuery(
            ResolvedReportDefinition definition,
            boolean limitedPreview
    ) {
        Map<Long, String> aliases = createAliases(definition);
        List<ReportPreviewColumnResponse> columns = createColumns(definition.selectedFields());
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder();

        appendSelect(sql, definition.selectedFields(), columns, aliases);
        appendFrom(sql, definition, aliases);
        appendFilters(sql, definition.filters(), aliases, parameters);
        appendOrderBy(sql, definition, aliases);
        if (limitedPreview) {
            sql.append(" LIMIT ").append(FETCH_LIMIT);
        }

        return new PreparedReportQuery(sql.toString(), List.copyOf(parameters), columns);
    }

    private void appendFrom(
            StringBuilder sql,
            ResolvedReportDefinition definition,
            Map<Long, String> aliases
    ) {
        sql.append(" FROM ")
                .append(qualifiedTable(definition.rootDataset().getSourceName()))
                .append(" t0");
        appendJoins(sql, definition, aliases);
    }

    /**
     * Réserve {@code t0} au dataset racine et attribue ensuite {@code t1}, {@code t2}, etc. aux
     * jointures dans l’ordre de la définition résolue.
     */
    private Map<Long, String> createAliases(ResolvedReportDefinition definition) {
        Map<Long, String> aliases = new HashMap<>();
        aliases.put(definition.rootDataset().getId(), "t0");
        for (int index = 0; index < definition.joins().size(); index++) {
            aliases.put(definition.joins().get(index).targetDataset().getId(), "t" + (index + 1));
        }
        return aliases;
    }

    /**
     * Crée les colonnes de réponse dans l’ordre de sélection ; leur clé devient également l’alias de
     * la colonne correspondante dans la clause {@code SELECT}.
     */
    private List<ReportPreviewColumnResponse> createColumns(List<DataSetField> selectedFields) {
        return selectedFields.stream()
                .map(field -> new ReportPreviewColumnResponse(
                        columnKey(field),
                        field.getId(),
                        field.getDisplayName(),
                        field.getDataType()
                ))
                .toList();
    }

    private void appendSelect(
            StringBuilder sql,
            List<DataSetField> selectedFields,
            List<ReportPreviewColumnResponse> columns,
            Map<Long, String> aliases
    ) {
        StringJoiner select = new StringJoiner(", ", "SELECT ", "");
        for (int index = 0; index < selectedFields.size(); index++) {
            DataSetField field = selectedFields.get(index);
            select.add(qualifiedColumn(field, aliases)
                    + " AS "
                    + quoteIdentifier(columns.get(index).key()));
        }
        sql.append(select);
    }

    /**
     * Ajoute un {@code LEFT JOIN} direct depuis la racine pour chaque cible et relie par {@code AND}
     * toutes les paires de colonnes d’une éventuelle clé étrangère composite.
     */
    private void appendJoins(
            StringBuilder sql,
            ResolvedReportDefinition definition,
            Map<Long, String> aliases
    ) {
        for (ResolvedJoin join : definition.joins()) {
            String targetAlias = aliases.get(join.targetDataset().getId());
            StringJoiner conditions = new StringJoiner(" AND ");
            for (ResolvedJoinColumn column : join.columns()) {
                conditions.add("t0." + quoteIdentifier(column.sourceColumn())
                        + " = " + targetAlias + "." + quoteIdentifier(column.targetColumn()));
            }

            sql.append(" LEFT JOIN ")
                    .append(qualifiedTable(join.targetDataset().getSourceName()))
                    .append(' ')
                    .append(targetAlias)
                    .append(" ON ")
                    .append(conditions);
        }
    }

    /**
     * Combine les filtres avec {@code AND} et alimente la liste de paramètres dans le même ordre que
     * les marqueurs ajoutés à la clause {@code WHERE}.
     */
    private void appendFilters(
            StringBuilder sql,
            List<ResolvedFilter> filters,
            Map<Long, String> aliases,
            List<Object> parameters
    ) {
        if (filters.isEmpty()) {
            return;
        }

        StringJoiner predicates = new StringJoiner(" AND ", " WHERE ", "");
        for (ResolvedFilter filter : filters) {
            String column = qualifiedColumn(filter.field(), aliases);
            predicates.add(toPredicate(column, filter, parameters));
        }
        sql.append(predicates);
    }

    private String toPredicate(
            String column,
            ResolvedFilter filter,
            List<Object> parameters
    ) {
        return switch (filter.operator()) {
            case EQUALS -> parameterized(column + " = ?", filter.values(), parameters);
            case CONTAINS -> {
                String value = (String) filter.values().get(0);
                parameters.add('%' + escapeLikeValue(value) + '%');
                yield column + " ILIKE ? ESCAPE '!'";
            }
            case GREATER_THAN -> parameterized(column + " > ?", filter.values(), parameters);
            case GREATER_THAN_OR_EQUAL -> parameterized(column + " >= ?", filter.values(), parameters);
            case LESS_THAN -> parameterized(column + " < ?", filter.values(), parameters);
            case LESS_THAN_OR_EQUAL -> parameterized(column + " <= ?", filter.values(), parameters);
            case BETWEEN -> {
                parameters.addAll(filter.values());
                yield column + " BETWEEN ? AND ?";
            }
        };
    }

    private String parameterized(String predicate, List<Object> values, List<Object> parameters) {
        parameters.add(values.get(0));
        return predicate;
    }

    /**
     * Applique l’ordre de tri demandé sans le modifier. Sans tri explicite, les champs de clé primaire
     * de la racine fournissent un ordre stable lorsqu’ils sont catalogués ; s’ils sont absents, aucune
     * clause {@code ORDER BY} n’est ajoutée.
     */
    private void appendOrderBy(
            StringBuilder sql,
            ResolvedReportDefinition definition,
            Map<Long, String> aliases
    ) {
        StringJoiner orderBy = new StringJoiner(", ");
        if (!definition.sorts().isEmpty()) {
            definition.sorts().forEach(sort -> orderBy.add(
                    qualifiedColumn(sort.field(), aliases) + " " + sort.direction().name()
            ));
        } else {
            definition.rootPrimaryKeyFields().forEach(field -> orderBy.add(
                    qualifiedColumn(field, aliases) + " ASC"
            ));
        }

        if (orderBy.length() > 0) {
            sql.append(" ORDER BY ").append(orderBy);
        }
    }

    private String qualifiedColumn(DataSetField field, Map<Long, String> aliases) {
        String alias = aliases.get(field.getDataset().getId());
        if (alias == null) {
            throw new IllegalStateException("No SQL alias for dataset " + field.getDataset().getId());
        }
        return alias + "." + quoteIdentifier(field.getSourceName());
    }

    private String qualifiedTable(String tableName) {
        return quoteIdentifier(PRODUCT_SCHEMA) + "." + quoteIdentifier(tableName);
    }

    private String quoteIdentifier(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    private String columnKey(DataSetField field) {
        return "field_" + field.getId();
    }

    /**
     * Échappe le caractère d’échappement {@code !} configuré avant les jokers PostgreSQL afin qu’une
     * valeur {@code CONTAINS} soit recherchée littéralement.
     */
    private String escapeLikeValue(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
