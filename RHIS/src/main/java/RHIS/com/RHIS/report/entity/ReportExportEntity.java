package RHIS.com.RHIS.report.entity;

import RHIS.com.RHIS.report.model.ReportExportFormat;
import RHIS.com.RHIS.report.model.ReportExportStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "report_export",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_report_export_generation_format",
                columnNames = {"generation_id", "format"}
        ),
        indexes = @Index(name = "idx_report_export_status", columnList = "status")
)
@Getter
@Setter
@NoArgsConstructor
public class ReportExportEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generation_id", nullable = false)
    private ReportGenerationEntity generation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private ReportExportFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReportExportStatus status;

    @Column(nullable = false)
    private int progress;

    @Column(name = "file_location")
    private String fileLocation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "heartbeat_at")
    private Instant heartbeatAt;

    @Column(name = "error_code", length = 64)
    private String errorCode;
}
