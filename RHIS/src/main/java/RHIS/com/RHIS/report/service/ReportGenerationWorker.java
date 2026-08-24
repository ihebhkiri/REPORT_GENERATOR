package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.report.config.ReportJobProperties;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewColumnResponse;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewRequest;
import RHIS.com.RHIS.report.exception.ReportDefinitionUnavailableException;
import RHIS.com.RHIS.report.model.ReportGenerationPhase;
import RHIS.com.RHIS.report.snapshot.ReportSnapshotMetadata;
import RHIS.com.RHIS.report.snapshot.ReportSnapshotWriter;
import RHIS.com.RHIS.report.storage.ReportArtifactStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class ReportGenerationWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportGenerationWorker.class);

    private final ReportJobStateService stateService;
    private final ReportDefinitionResolver definitionResolver;
    private final ReportSqlBuilder sqlBuilder;
    private final ReportFullQueryExecutor queryExecutor;
    private final ReportSnapshotWriter snapshotWriter;
    private final ReportArtifactStorage storage;
    private final ReportJobProperties properties;
    private final ObjectMapper objectMapper;


    public void run(UUID generationId) {
        try {
            String definitionJson = stateService.startGeneration(generationId).orElse(null);
            if (definitionJson == null) {
                return;
            }
            ReportPreviewRequest request = objectMapper.readValue(definitionJson, ReportPreviewRequest.class);
            ResolvedReportDefinition definition = definitionResolver.resolve(request);
            stateService.recordGenerationProgress(
                    generationId,
                    ReportGenerationPhase.COUNTING,
                    10,
                    0,
                    null
            );

            long totalRows = queryExecutor.count(sqlBuilder.buildCount(definition));
            stateService.recordGenerationProgress(
                    generationId,
                    ReportGenerationPhase.READING_ROWS,
                    20,
                    0,
                    totalRows
            );
            PreparedReportQuery fullQuery = sqlBuilder.buildFull(definition);
            ReportSnapshotMetadata metadata = new ReportSnapshotMetadata(
                    columns(fullQuery.columns()),
                    totalRows
            );
            AtomicLong processed = new AtomicLong();
            String location = storage.writeAtomically(generationId, "snapshot.ndjson.gz", output ->
                    snapshotWriter.write(output, metadata, sink ->
                            queryExecutor.stream(fullQuery, row -> {
                                sink.write(row);
                                long current = processed.incrementAndGet();
                                if (current % properties.getProgressBatchSize() == 0 || current == totalRows) {
                                    stateService.recordGenerationProgress(
                                            generationId,
                                            ReportGenerationPhase.READING_ROWS,
                                            readingProgress(current, totalRows),
                                            current,
                                            totalRows
                                    );
                                }
                            })
                    )
            );
            stateService.recordGenerationProgress(
                    generationId,
                    ReportGenerationPhase.FINALIZING,
                    99,
                    processed.get(),
                    totalRows
            );
            stateService.completeGeneration(generationId, location, totalRows);
        } catch (ReportDefinitionUnavailableException exception) {
            LOGGER.warn("Report definition became unavailable: generationId={}", generationId);
            stateService.failGeneration(generationId, "REPORT_DEFINITION_UNAVAILABLE");
        } catch (Exception exception) {
            LOGGER.error("Report generation failed: generationId={}", generationId, exception);
            stateService.failGeneration(generationId, "GENERATION_FAILED");
        }
    }
    ///  HELPERS

    private List<ReportSnapshotMetadata.Column> columns(List<ReportPreviewColumnResponse> columns) {
        return columns.stream()
                .map(column -> new ReportSnapshotMetadata.Column(
                        column.key(),
                        column.displayName(),
                        column.dataType()
                ))
                .toList();
    }

    private int readingProgress(long processed, long total) {
        if (total <= 0) {
            return 95;
        }
        return 20 + (int) Math.min(75, (processed * 75) / total);
    }
}
