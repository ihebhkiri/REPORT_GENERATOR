package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.report.config.ReportJobProperties;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewColumnResponse;
import RHIS.com.RHIS.report.exception.ReportExecutionException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.time.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReportFullQueryExecutor {

    private final JdbcTemplate jdbcTemplate;
    private final ReportJobProperties properties;


    public long count(PreparedCountQuery query) {
        Long count = jdbcTemplate.query(
                connection -> prepare(connection, query.sql(), query.parameters(), false),
                resultSet -> resultSet.next() ? resultSet.getLong(1) : 0L
        );
        return count == null ? 0L : count;
    }

    public void stream(PreparedReportQuery query, RowConsumer consumer) {
        try {
            jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try (PreparedStatement statement = prepare(
                        connection,
                        query.sql(),
                        query.parameters(),
                        true
                ); ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        if (Thread.currentThread().isInterrupted()) {
                            throw new SQLException("Report generation cancelled");
                        }
                        try {
                            consumer.accept(mapRow(resultSet, query));
                        } catch (Exception exception) {
                            if (exception instanceof SQLException sqlException) {
                                throw sqlException;
                            }
                            throw new SQLException("Report row consumer failed", exception);
                        }
                    }
                    connection.rollback();
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
                return null;
            });
        } catch (Exception exception) {
            String message = Thread.currentThread().isInterrupted()
                    ? "La génération du rapport a été annulée."
                    : "La lecture complète du rapport a échoué.";
            throw new ReportExecutionException(message, exception);
        }
    }

    private PreparedStatement prepare(
            Connection connection,
            String sql,
            java.util.List<Object> parameters,
            boolean streaming
    ) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                sql,
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY
        );
        statement.setQueryTimeout(properties.getQueryTimeoutSeconds());
        if (streaming) {
            statement.setFetchSize(properties.getFetchSize());
        }
        for (int index = 0; index < parameters.size(); index++) {
            statement.setObject(index + 1, parameters.get(index));
        }
        return statement;
    }

    private Map<String, Object> mapRow(ResultSet resultSet, PreparedReportQuery query) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (ReportPreviewColumnResponse column : query.columns()) {
            row.put(column.key(), readValue(resultSet, column));
        }
        return row;
    }

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

    @FunctionalInterface
    public interface RowConsumer {
        void accept(Map<String, Object> row) throws Exception;
    }
}
