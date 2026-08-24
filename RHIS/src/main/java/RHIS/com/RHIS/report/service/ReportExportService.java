package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.report.controller.dto.ReportExportResponse;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewRequest;
import RHIS.com.RHIS.report.entity.ReportExportEntity;
import RHIS.com.RHIS.report.entity.ReportGenerationEntity;
import RHIS.com.RHIS.report.exception.ReportConflictException;
import RHIS.com.RHIS.report.exception.ReportDefinitionUnavailableException;
import RHIS.com.RHIS.report.exception.ReportExecutionException;
import RHIS.com.RHIS.report.exception.ReportResourceNotFoundException;
import RHIS.com.RHIS.report.model.ReportExportFormat;
import RHIS.com.RHIS.report.model.ReportExportStatus;
import RHIS.com.RHIS.report.model.ReportGenerationStatus;
import RHIS.com.RHIS.report.exception.UnavailableReportElement;
import RHIS.com.RHIS.report.repository.ReportExportRepository;
import RHIS.com.RHIS.report.repository.ReportGenerationRepository;
import RHIS.com.RHIS.report.storage.ReportArtifactStorage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final ReportGenerationRepository generationRepository;
    private final ReportExportRepository exportRepository;
    private final ReportJobDispatcher dispatcher;
    private final Clock clock;
    private final ReportArtifactStorage storage;
    private final ObjectMapper objectMapper;
    private final ReportDefinitionResolver definitionResolver;

    @Transactional
    public ReportExportResponse create(Long ownerId, UUID generationId, ReportExportFormat format) {
        ReportGenerationEntity generation = generationRepository.findByIdAndOwnerIdForUpdate(generationId, ownerId)
                .orElseThrow(() -> new ReportResourceNotFoundException(
                        "La génération demandée est introuvable."
                ));
        if (generation.getStatus() != ReportGenerationStatus.READY) {
            throw new ReportConflictException("La génération doit être prête avant de créer un export.");
        }
        validateDefinition(generation.getDefinitionJson());

        ReportExportEntity export = exportRepository.findByGeneration_IdAndFormat(generationId, format)
                .map(existing -> prepareExisting(existing))
                .orElseGet(() -> newExport(generation, format));
        generation.setLastActivityAt(clock.instant());
        exportRepository.save(export);
        generationRepository.save(generation);

        if (export.getStatus() == ReportExportStatus.PENDING) {
            afterCommit(() -> dispatcher.dispatchExport(export.getId()));
        }
        return toResponse(export);
    }

    @Transactional(readOnly = true)
    public ReportExportResponse get(Long ownerId, UUID exportId) {
        return toResponse(findOwned(ownerId, exportId));
    }

    @Transactional(readOnly = true)
    public DownloadPayload openDownload(Long ownerId, UUID exportId) {
        ReportExportEntity export = findOwned(ownerId, exportId);
        if (export.getStatus() != ReportExportStatus.READY || export.getFileLocation() == null) {
            throw new ReportConflictException("Le fichier n'est pas encore prêt.");
        }
        validateDefinition(export.getGeneration().getDefinitionJson());
        try {
            InputStream input = storage.open(export.getFileLocation());
            String fileName = "rapport-" + export.getId() + '.' + export.getFormat().extension();
            return new DownloadPayload(input, export.getFormat().contentType(), fileName);
        } catch (IOException exception) {
            throw new ReportExecutionException("Le fichier exporté ne peut pas être ouvert.", exception);
        }
    }

    private ReportExportEntity prepareExisting(ReportExportEntity export) {
        if (export.getStatus() == ReportExportStatus.READY) {
            return export;
        }
        if (export.getStatus() == ReportExportStatus.PENDING
                || export.getStatus() == ReportExportStatus.RUNNING) {
            throw new ReportConflictException("Un export de ce format est déjà en cours.");
        }
        export.setStatus(ReportExportStatus.PENDING);
        export.setProgress(0);
        export.setFileLocation(null);
        export.setErrorCode(null);
        export.setHeartbeatAt(null);
        return export;
    }

    private ReportExportEntity newExport(ReportGenerationEntity generation, ReportExportFormat format) {
        ReportExportEntity export = new ReportExportEntity();
        export.setId(UUID.randomUUID());
        export.setGeneration(generation);
        export.setFormat(format);
        export.setStatus(ReportExportStatus.PENDING);
        export.setProgress(0);
        export.setCreatedAt(clock.instant());
        return export;
    }

    private ReportExportEntity findOwned(Long ownerId, UUID exportId) {
        return exportRepository.findByIdAndGeneration_Owner_Id(exportId, ownerId)
                .orElseThrow(() -> new ReportResourceNotFoundException(
                        "L'export demandé est introuvable."
                ));
    }

    private void validateDefinition(String definitionJson) {
        try {
            definitionResolver.resolve(objectMapper.readValue(definitionJson, ReportPreviewRequest.class));
        } catch (JsonProcessingException exception) {
            throw new ReportExecutionException("La définition enregistrée ne peut pas être relue.", exception);
        }
    }

    private ReportExportResponse toResponse(ReportExportEntity export) {
        return new ReportExportResponse(
                export.getId(),
                export.getGeneration().getId(),
                export.getFormat(),
                export.getStatus(),
                export.getProgress(),
                export.getErrorCode(),
                unavailableElements(export.getErrorCode(), export.getGeneration().getDefinitionJson())
        );
    }

    private List<UnavailableReportElement> unavailableElements(String errorCode, String definitionJson) {
        if (!"REPORT_DEFINITION_UNAVAILABLE".equals(errorCode) || definitionJson == null) {
            return List.of();
        }
        try {
            definitionResolver.resolve(objectMapper.readValue(definitionJson, ReportPreviewRequest.class));
        } catch (ReportDefinitionUnavailableException exception) {
            return exception.getUnavailableElements();
        } catch (JsonProcessingException ignored) {
            // The persisted error code remains the authoritative status when a legacy definition is unreadable.
        }
        return List.of();
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    public record DownloadPayload(InputStream input, String contentType, String fileName) {
    }
}
