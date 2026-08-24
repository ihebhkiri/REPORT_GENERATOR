package RHIS.com.RHIS.report.config;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReportJobConfigurationTest {

    @Test
    void serializesTemporalValuesAsIsoStringsInNewSnapshots() throws Exception {
        var mapper = new ReportJobConfiguration().reportObjectMapper();

        String json = mapper.writeValueAsString(LocalDate.of(2022, 1, 3));

        assertThat(json).isEqualTo("\"2022-01-03\"");
    }
}
