package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.report.config.ReportJobProperties;
import RHIS.com.RHIS.report.entity.ReportGenerationEntity;
import RHIS.com.RHIS.report.model.ReportExportFormat;
import RHIS.com.RHIS.report.model.ReportExportStatus;
import RHIS.com.RHIS.report.model.ReportGenerationPhase;
import RHIS.com.RHIS.report.model.ReportGenerationStatus;
import RHIS.com.RHIS.report.repository.ReportExportRepository;
import RHIS.com.RHIS.report.repository.ReportGenerationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportJobStateService {

    private final ReportGenerationRepository generationRepository;
    private final ReportExportRepository exportRepository;
    private final ReportJobProperties properties;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<String> startGeneration(UUID id) {
        return generationRepository.findById(id)
                .filter(entity -> entity.getStatus() == ReportGenerationStatus.PENDING)
                .map(entity -> {
                    Instant now = clock.instant();
                    entity.setStatus(ReportGenerationStatus.RUNNING);
                    entity.setPhase(ReportGenerationPhase.VALIDATING);
                    entity.setHeartbeatAt(now);
                    entity.setProgress(Math.max(entity.getProgress(), 1));
                    return entity.getDefinitionJson();
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordGenerationProgress(
            UUID id,
            ReportGenerationPhase phase,
            int progress,
            long processedRows,
            Long totalRows
    ) {
        generationRepository.findById(id).ifPresent(entity -> {
            if (entity.getStatus() != ReportGenerationStatus.RUNNING) {
                return;
            }
            entity.setPhase(phase);
            entity.setProgress(Math.max(entity.getProgress(), Math.min(progress, 99)));
            entity.setProcessedRowCount(Math.max(
                    entity.getProcessedRowCount() == null ? 0L : entity.getProcessedRowCount(),
                    processedRows
            ));
            if (totalRows != null) {
                entity.setTotalRowCount(totalRows);
            }
            entity.setHeartbeatAt(clock.instant());
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeGeneration(UUID id, String location, long totalRows) {
        generationRepository.findById(id).ifPresent(entity -> {
            if (entity.getStatus() != ReportGenerationStatus.RUNNING) {
                return;
            }
            Instant now = clock.instant();
            entity.setSnapshotLocation(location);
            entity.setProcessedRowCount(totalRows);
            entity.setTotalRowCount(totalRows);
            entity.setProgress(100);
            entity.setPhase(ReportGenerationPhase.FINALIZING);
            entity.setStatus(ReportGenerationStatus.READY);
            entity.setHeartbeatAt(now);
            entity.setLastActivityAt(now);
            entity.setExpiresAt(now.plus(properties.getExpiration()));
            entity.setErrorCode(null);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failGeneration(UUID id, String errorCode) {
        generationRepository.findById(id).ifPresent(entity -> {
            if (entity.getStatus() == ReportGenerationStatus.EXPIRED) {
                return;
            }
            entity.setStatus(ReportGenerationStatus.FAILED);
            entity.setHeartbeatAt(clock.instant());
            entity.setErrorCode(errorCode);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ReportExportWork> startExport(UUID id) {
        return exportRepository.findById(id)
                .filter(entity -> entity.getStatus() == ReportExportStatus.PENDING)
                .filter(entity -> entity.getGeneration().getStatus() == ReportGenerationStatus.READY)
                .filter(entity -> entity.getGeneration().getSnapshotLocation() != null)
                .map(entity -> {
                    Instant now = clock.instant();
                    entity.setStatus(ReportExportStatus.RUNNING);
                    entity.setProgress(1);
                    entity.setHeartbeatAt(now);
                    return new ReportExportWork(
                            entity.getId(),
                            entity.getGeneration().getId(),
                            entity.getGeneration().getSnapshotLocation(),
                            entity.getGeneration().getDefinitionJson(),
                            entity.getFormat()
                    );
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordExportProgress(UUID id, int progress) {
        exportRepository.findById(id).ifPresent(entity -> {
            if (entity.getStatus() == ReportExportStatus.RUNNING) {
                entity.setProgress(Math.max(entity.getProgress(), Math.min(progress, 99)));
                entity.setHeartbeatAt(clock.instant());
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeExport(UUID id, String location) {
        exportRepository.findById(id).ifPresent(entity -> {
            if (entity.getStatus() != ReportExportStatus.RUNNING) {
                return;
            }
            Instant now = clock.instant();
            entity.setFileLocation(location);
            entity.setStatus(ReportExportStatus.READY);
            entity.setProgress(100);
            entity.setHeartbeatAt(now);
            entity.setErrorCode(null);
            ReportGenerationEntity generation = entity.getGeneration();
            generation.setLastActivityAt(now);
            generation.setExpiresAt(now.plus(properties.getExpiration()));
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failExport(UUID id, String errorCode) {
        exportRepository.findById(id).ifPresent(entity -> {
            entity.setStatus(ReportExportStatus.FAILED);
            entity.setHeartbeatAt(clock.instant());
            entity.setErrorCode(errorCode);
            entity.setFileLocation(null);
        });
    }

    public record ReportExportWork(
            UUID exportId,
            UUID generationId,
            String snapshotLocation,
            String definitionJson,
            ReportExportFormat format
    ) {
    }
}
