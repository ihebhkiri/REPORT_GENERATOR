package RHIS.com.RHIS.report.repository;

import RHIS.com.RHIS.report.entity.ReportGenerationEntity;
import RHIS.com.RHIS.report.model.ReportGenerationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportGenerationRepository extends JpaRepository<ReportGenerationEntity, UUID> {
    Optional<ReportGenerationEntity> findByOwner_IdAndIdempotencyKey(Long ownerId, UUID idempotencyKey);

    Optional<ReportGenerationEntity> findByIdAndOwner_Id(UUID id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT generation FROM ReportGenerationEntity generation 
                         WHERE generation.id = :id AND generation.owner.id = :ownerId 
            """)
    Optional<ReportGenerationEntity> findByIdAndOwnerIdForUpdate(UUID id, Long ownerId);

    long countByOwner_IdAndStatusIn(Long ownerId, Collection<ReportGenerationStatus> statuses);

    List<ReportGenerationEntity> findByExpiresAtBeforeAndStatusNot(Instant now, ReportGenerationStatus status);

    List<ReportGenerationEntity> findByStatusAndHeartbeatAtBefore(
            ReportGenerationStatus status,
            Instant heartbeatBefore
    );

    List<ReportGenerationEntity> findByStatusAndExpiresAtBefore(
            ReportGenerationStatus status,
            Instant expiresBefore
    );
}
