package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.report.config.ReportJobProperties;
import RHIS.com.RHIS.report.entity.ReportExportEntity;
import RHIS.com.RHIS.report.model.ReportExportStatus;
import RHIS.com.RHIS.report.repository.ReportExportRepository;
import RHIS.com.RHIS.report.repository.ReportGenerationRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportJobStateServiceTest {

    @Test
    void keepsExportProgressMonotonicWhenWriterCallbacksDecreaseOrExceedTheMaximum() {
        UUID exportId = UUID.randomUUID();
        ReportExportEntity export = new ReportExportEntity();
        export.setStatus(ReportExportStatus.RUNNING);
        export.setProgress(1);
        ReportExportRepository exportRepository = mock(ReportExportRepository.class);
        when(exportRepository.findById(exportId)).thenReturn(Optional.of(export));
        Instant now = Instant.parse("2026-08-12T12:00:00Z");
        ReportJobStateService stateService = new ReportJobStateService(
                mock(ReportGenerationRepository.class),
                exportRepository,
                new ReportJobProperties(),
                Clock.fixed(now, ZoneOffset.UTC)
        );

        stateService.recordExportProgress(exportId, 70);
        stateService.recordExportProgress(exportId, 20);
        stateService.recordExportProgress(exportId, 120);

        assertThat(export.getProgress()).isEqualTo(99);
        assertThat(export.getHeartbeatAt()).isEqualTo(now);
    }
}
