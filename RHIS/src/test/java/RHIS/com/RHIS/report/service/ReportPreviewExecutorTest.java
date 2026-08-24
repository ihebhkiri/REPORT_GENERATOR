package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.report.exception.ReportQueryTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportPreviewExecutorTest {

    @Test
    void translatesJdbcTimeoutWithoutExposingSql() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(
                any(PreparedStatementCreator.class),
                org.mockito.ArgumentMatchers.<RowMapper<Map<String, Object>>>any()
        )).thenThrow(new QueryTimeoutException("database timeout"));
        ReportPreviewExecutor executor = new ReportPreviewExecutor(jdbcTemplate);

        assertThatThrownBy(() -> executor.execute(
                new PreparedReportQuery("SELECT secret FROM payroll", List.of(), List.of())
        ))
                .isInstanceOf(ReportQueryTimeoutException.class)
                .hasMessage("La prévisualisation a dépassé cinq secondes.")
                .hasMessageNotContaining("payroll");
    }
}
