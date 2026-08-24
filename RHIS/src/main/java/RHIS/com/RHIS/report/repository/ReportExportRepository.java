package RHIS.com.RHIS.report.repository;

import RHIS.com.RHIS.report.entity.ReportExportEntity;
import RHIS.com.RHIS.report.model.ReportExportFormat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.List;
import RHIS.com.RHIS.report.model.ReportExportStatus;

public interface ReportExportRepository extends JpaRepository<ReportExportEntity, UUID> {
    Optional<ReportExportEntity> findByIdAndGeneration_Owner_Id(UUID id, Long ownerId);

    Optional<ReportExportEntity> findByGeneration_IdAndFormat(UUID generationId, ReportExportFormat format);

    List<ReportExportEntity> findByStatusAndHeartbeatAtBefore(
            ReportExportStatus status,
            Instant heartbeatBefore
    );
}
