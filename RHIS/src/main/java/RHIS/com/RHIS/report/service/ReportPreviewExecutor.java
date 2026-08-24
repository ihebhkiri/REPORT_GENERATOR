package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.report.controller.dto.ReportPreviewColumnResponse;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewResponse;
import RHIS.com.RHIS.report.exception.ReportExecutionException;
import RHIS.com.RHIS.report.exception.ReportQueryTimeoutException;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Exécute les requêtes préparées d’aperçu via JDBC, applique leur délai maximal et transforme les
 * résultats typés selon les contraintes de pagination de l’aperçu.
 */
@Component
@RequiredArgsConstructor
class ReportPreviewExecutor {

    private static final int PREVIEW_LIMIT = 6;
    private static final int QUERY_TIMEOUT_SECONDS = 5;

    private final JdbcTemplate jdbcTemplate;



    /**
     * Expose au maximum six lignes et utilise uniquement la septième pour calculer {@code hasMore}.
     * Les timeouts et autres erreurs JDBC sont traduits en exceptions métier sans exposer le SQL.
     */
    ReportPreviewResponse execute(PreparedReportQuery query) {
        try {
            List<Map<String, Object>> fetchedRows = jdbcTemplate.query(
                    connection -> createPreparedStatement(connection.prepareStatement(query.sql()), query),
                    (resultSet, rowNumber) -> mapRow(resultSet, query.columns())
            );

            boolean hasMore = fetchedRows.size() > PREVIEW_LIMIT;
            List<Map<String, Object>> rows = hasMore
                    ? List.copyOf(fetchedRows.subList(0, PREVIEW_LIMIT))
                    : List.copyOf(fetchedRows);

            return new ReportPreviewResponse(
                    query.columns(),
                    rows,
                    hasMore,
                    rows.size()
            );
        } catch (QueryTimeoutException exception) {
            throw new ReportQueryTimeoutException("La prévisualisation a dépassé cinq secondes.", exception);
        } catch (DataAccessException exception) {
            throw new ReportExecutionException("La prévisualisation n'a pas pu être exécutée.", exception);
        }
    }

    /**
     * Applique le timeout de cinq secondes avant de lier les paramètres selon l’ordre des marqueurs
     * enregistré par le constructeur SQL.
     */
    private PreparedStatement createPreparedStatement(
            PreparedStatement statement,
            PreparedReportQuery query
    ) throws SQLException {
        statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
        for (int index = 0; index < query.parameters().size(); index++) {
            statement.setObject(index + 1, query.parameters().get(index));
        }
        return statement;
    }


    /**
     * Utilise une {@link LinkedHashMap} pour conserver dans chaque ligne l’ordre des colonnes défini
     * par la sélection du rapport.
     */
    private Map<String, Object> mapRow(
            ResultSet resultSet,
            List<ReportPreviewColumnResponse> columns
    ) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (ReportPreviewColumnResponse column : columns) {
            row.put(column.key(), readValue(resultSet, column));
        }
        return row;
    }

    /**
     * Utilise des lectures JDBC typées pour les colonnes temporelles et UUID afin que la réponse
     * contienne des types Java du domaine plutôt que des représentations propres au pilote.
     */
    private Object readValue(ResultSet resultSet, ReportPreviewColumnResponse column) throws SQLException {
        return switch (column.dataType()) {
            case DATE -> resultSet.getObject(column.key(), LocalDate.class);
            case TIME -> resultSet.getObject(column.key(), LocalTime.class);
            case DATE_TIME -> resultSet.getObject(column.key(), LocalDateTime.class);
            case OFFSET_DATE_TIME -> resultSet.getObject(column.key(), OffsetDateTime.class);
            case UUID -> resultSet.getObject(column.key(), UUID.class);
            case TEXT, INTEGER, DECIMAL, BOOLEAN -> resultSet.getObject(column.key());
            case UNSUPPORTED -> throw new SQLException("Unsupported result type");
        };
    }
}
