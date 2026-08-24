package RHIS.com.RHIS.report.entity;

import RHIS.com.RHIS.auth.user.UserEntity;
import RHIS.com.RHIS.report.model.ReportGenerationPhase;
import RHIS.com.RHIS.report.model.ReportGenerationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "report_generation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_report_generation_owner_idempotency",
                columnNames = {"owner_user_id", "idempotency_key"}
        ),
        indexes = {
                @Index(name = "idx_report_generation_owner", columnList = "owner_user_id"),
                @Index(name = "idx_report_generation_status", columnList = "status"),
                @Index(name = "idx_report_generation_expires", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ReportGenerationEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserEntity owner;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "definition_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String definitionJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReportGenerationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private ReportGenerationPhase phase;

    @Column(nullable = false)
    private int progress;

    @Column(name = "processed_row_count")
    private Long processedRowCount;

    @Column(name = "total_row_count")
    private Long totalRowCount;

    @Column(name = "snapshot_location")
    private String snapshotLocation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "heartbeat_at")
    private Instant heartbeatAt;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportExportEntity> exports = new ArrayList<>();
}
