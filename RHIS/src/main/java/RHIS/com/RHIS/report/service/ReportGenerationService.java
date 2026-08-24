package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.auth.user.UserEntity;
import RHIS.com.RHIS.auth.user.UserRepository;
import RHIS.com.RHIS.report.config.ReportJobProperties;
import RHIS.com.RHIS.report.controller.dto.ReportGenerationResponse;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewRequest;
import RHIS.com.RHIS.report.entity.ReportGenerationEntity;
import RHIS.com.RHIS.report.exception.ReportCapacityException;
import RHIS.com.RHIS.report.exception.ReportExecutionException;
import RHIS.com.RHIS.report.exception.ReportDefinitionUnavailableException;
import RHIS.com.RHIS.report.exception.ReportResourceNotFoundException;
import RHIS.com.RHIS.report.model.ReportGenerationStatus;
import RHIS.com.RHIS.report.exception.UnavailableReportElement;
import RHIS.com.RHIS.report.repository.ReportGenerationRepository;
import RHIS.com.RHIS.report.storage.ReportArtifactStorage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private static final List<ReportGenerationStatus> ACTIVE_STATUSES = List.of(
            ReportGenerationStatus.PENDING,
            ReportGenerationStatus.RUNNING
    );

    private final ReportGenerationRepository generationRepository;
    private final UserRepository userRepository;
    private final ReportDefinitionResolver definitionResolver;
    private final ReportJobDispatcher dispatcher;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final ReportJobProperties properties;
    private final ReportArtifactStorage storage;


    @Transactional
    public ReportGenerationResponse create(
            UserEntity owner,
            UUID idempotencyKey,
            ReportPreviewRequest request
    ) {

        ReportGenerationEntity existing = generationRepository
                .findByOwner_IdAndIdempotencyKey(owner.getId(), idempotencyKey)
                .orElse(null);

        if (existing != null) {
            return toResponse(existing);
        }

        UserEntity lockedOwner = userRepository.findByIdForUpdate(owner.getId())
                .orElseThrow(() -> new ReportResourceNotFoundException("L'utilisateur courant est introuvable."));
        return generationRepository.findByOwner_IdAndIdempotencyKey(owner.getId(), idempotencyKey)
                .map(this::toResponse)
                .orElseGet(() -> createNew(lockedOwner, idempotencyKey, request));

    }

    @Transactional(readOnly = true)
    public ReportGenerationResponse get(Long ownerId, UUID generationId) {
        return toResponse(findOwned(ownerId, generationId));
    }

    @Transactional
    public void delete(Long ownerId, UUID generationId) {
        ReportGenerationEntity generation = findOwned(ownerId, generationId);
        dispatcher.cancelGeneration(generationId);
        generation.getExports().forEach(export -> dispatcher.cancelExport(export.getId()));
        try {
            storage.deleteGeneration(generationId);
        } catch (IOException exception) {
            throw new ReportExecutionException("Les fichiers temporaires du rapport ne peuvent pas être supprimés.", exception);
        }
        generationRepository.delete(generation);
    }

    // HELPERS
    private ReportGenerationResponse createNew(
            UserEntity owner,
            UUID idempotencyKey,
            ReportPreviewRequest request
    ) {
        long activeCount = generationRepository.countByOwner_IdAndStatusIn(owner.getId(), ACTIVE_STATUSES);
        if (activeCount >= properties.getMaxActivePerUser()) {
            throw new ReportCapacityException("Le nombre maximal de générations actives est atteint.");
        }

        definitionResolver.resolve(request);
        Instant now = clock.instant();
        ReportGenerationEntity generation = new ReportGenerationEntity();
        generation.setId(UUID.randomUUID());
        generation.setOwner(owner);
        generation.setIdempotencyKey(idempotencyKey);
        generation.setDefinitionJson(serialize(request));
        generation.setStatus(ReportGenerationStatus.PENDING);
        generation.setProgress(0);
        generation.setCreatedAt(now);
        generation.setLastActivityAt(now);
        generation.setExpiresAt(now.plus(properties.getExpiration()));

        try {
            generationRepository.saveAndFlush(generation);
        } catch (DataIntegrityViolationException exception) {
            return generationRepository.findByOwner_IdAndIdempotencyKey(owner.getId(), idempotencyKey)
                    .map(this::toResponse)
                    .orElseThrow(() -> exception);
        }
        afterCommit(() -> dispatcher.dispatchGeneration(generation.getId()));
        return toResponse(generation);
    }

    private ReportGenerationEntity findOwned(Long ownerId, UUID generationId) {
        return generationRepository.findByIdAndOwner_Id(generationId, ownerId)
                .orElseThrow(() -> new ReportResourceNotFoundException(
                        "La génération demandée est introuvable."
                ));
    }

    private String serialize(ReportPreviewRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new ReportExecutionException("La définition du rapport ne peut pas être enregistrée.", exception);
        }
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

    // MAPPERS

    private ReportGenerationResponse toResponse(ReportGenerationEntity generation) {
        return new ReportGenerationResponse(
                generation.getId(),
                generation.getStatus(),
                generation.getPhase(),
                generation.getProgress(),
                generation.getProcessedRowCount(),
                generation.getTotalRowCount(),
                generation.getCreatedAt(),
                generation.getExpiresAt(),
                generation.getErrorCode(),
                unavailableElements(generation.getErrorCode(), generation.getDefinitionJson())
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
}
