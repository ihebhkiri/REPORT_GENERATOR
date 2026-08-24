package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.report.config.ReportJobProperties;
import RHIS.com.RHIS.report.entity.ReportGenerationEntity;
import RHIS.com.RHIS.report.model.ReportExportStatus;
import RHIS.com.RHIS.report.model.ReportGenerationStatus;
import RHIS.com.RHIS.report.repository.ReportExportRepository;
import RHIS.com.RHIS.report.repository.ReportGenerationRepository;
import RHIS.com.RHIS.report.storage.ReportArtifactStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ReportJobCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportJobCleanupService.class);
    private final ReportGenerationRepository generationRepository;
    private final ReportExportRepository exportRepository;
    private final ReportArtifactStorage storage;
    private final ReportJobDispatcher dispatcher;
    private final ReportJobProperties properties;
    private final Clock clock;

    public ReportJobCleanupService(
            ReportGenerationRepository generationRepository,
            ReportExportRepository exportRepository,
            ReportArtifactStorage storage,
            ReportJobDispatcher dispatcher,
            ReportJobProperties properties,
            Clock clock
    ) {
        this.generationRepository = generationRepository;
        this.exportRepository = exportRepository;
        this.storage = storage;
        this.dispatcher = dispatcher;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${rhis.report.jobs.cleanup-interval:PT1M}")
    @Transactional
    public void cleanup() {
        Instant now = clock.instant();
        failStaleWorkers(now.minus(properties.getHeartbeatTimeout()));
        expireArtifacts(now);
        purgeMetadata(now.minus(properties.getMetadataRetention()));
    }

    private void failStaleWorkers(Instant heartbeatBefore) {
        generationRepository.findByStatusAndHeartbeatAtBefore(
                ReportGenerationStatus.RUNNING,
                heartbeatBefore
        ).forEach(generation -> {
            dispatcher.cancelGeneration(generation.getId());
            generation.setStatus(ReportGenerationStatus.FAILED);
            generation.setErrorCode("WORKER_HEARTBEAT_TIMEOUT");
        });
        exportRepository.findByStatusAndHeartbeatAtBefore(
                ReportExportStatus.RUNNING,
                heartbeatBefore
        ).forEach(export -> {
            dispatcher.cancelExport(export.getId());
            export.setStatus(ReportExportStatus.FAILED);
            export.setErrorCode("WORKER_HEARTBEAT_TIMEOUT");
            export.setFileLocation(null);
        });
    }

    private void expireArtifacts(Instant now) {
        generationRepository.findByExpiresAtBeforeAndStatusNot(now, ReportGenerationStatus.EXPIRED)
                .stream()
                .filter(generation -> generation.getStatus() != ReportGenerationStatus.RUNNING)
                .forEach(generation -> expire(generation, now));
    }

    private void expire(ReportGenerationEntity generation, Instant now) {
        dispatcher.cancelGeneration(generation.getId());
        generation.getExports().forEach(export -> dispatcher.cancelExport(export.getId()));
        try {
            storage.deleteGeneration(generation.getId());
        } catch (Exception exception) {
            LOGGER.warn("Unable to remove expired report artifacts: generationId={}", generation.getId(), exception);
            return;
        }
        generation.getExports().clear();
        generation.setDefinitionJson(null);
        generation.setSnapshotLocation(null);
        generation.setStatus(ReportGenerationStatus.EXPIRED);
        generation.setPhase(null);
        generation.setHeartbeatAt(null);
        generation.setExpiresAt(now);
    }

    private void purgeMetadata(Instant expiresBefore) {
        generationRepository.deleteAll(
                generationRepository.findByStatusAndExpiresAtBefore(
                        ReportGenerationStatus.EXPIRED,
                        expiresBefore
                )
        );
    }
}
