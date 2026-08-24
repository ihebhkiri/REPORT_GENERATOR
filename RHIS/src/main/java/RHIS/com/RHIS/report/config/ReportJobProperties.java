package RHIS.com.RHIS.report.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties("rhis.report.jobs")
@Getter
@Setter
public class ReportJobProperties {
    private int corePoolSize ;
    private int maxPoolSize;
    private int queueCapacity;
    private int maxActivePerUser;
    private int fetchSize;
    private int queryTimeoutSeconds;
    private int progressBatchSize;
    private int xlsxRowWindow;
    private int jasperVirtualizerMaxPages;
    private Duration expiration = Duration.ofMinutes(30);
    private Duration heartbeatTimeout = Duration.ofMinutes(5);
    private Duration metadataRetention = Duration.ofHours(24);
    private Path storageRoot = Path.of(System.getProperty("java.io.tmpdir"), "rhis-report-artifacts");

}
