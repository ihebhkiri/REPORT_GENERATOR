package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.report.export.ReportExportWriter;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewRequest;
import RHIS.com.RHIS.report.exception.ReportDefinitionUnavailableException;
import RHIS.com.RHIS.report.model.ReportExportFormat;
import RHIS.com.RHIS.report.storage.ReportArtifactStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ReportExportWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportExportWorker.class);
    private final ReportJobStateService stateService;
    private final ReportArtifactStorage storage;
    private final ReportDefinitionResolver definitionResolver;
    private final ObjectMapper objectMapper;
    private final Map<ReportExportFormat, ReportExportWriter> writers;

    public ReportExportWorker(
            ReportJobStateService stateService,
            ReportArtifactStorage storage,
            ReportDefinitionResolver definitionResolver,
            ObjectMapper objectMapper,
            List<ReportExportWriter> writers
    ) {
        this.stateService = stateService;
        this.storage = storage;
        this.definitionResolver = definitionResolver;
        this.objectMapper = objectMapper;
        this.writers = new EnumMap<>(ReportExportFormat.class);
        writers.forEach(writer -> this.writers.put(writer.format(), writer));
    }

    public void run(UUID exportId) {
        ReportJobStateService.ReportExportWork work = stateService.startExport(exportId).orElse(null);
        if (work == null) {
            return;
        }
        String location = null;
        try {
            ReportPreviewRequest request = objectMapper.readValue(work.definitionJson(), ReportPreviewRequest.class);
            definitionResolver.resolve(request);
            ReportExportWriter writer = writers.get(work.format());
            if (writer == null) {
                throw new IllegalStateException("No writer for format " + work.format());
            }
            String fileName = "report-" + exportId + '.' + work.format().extension();
            location = storage.writeAtomically(work.generationId(), fileName, output -> {
                try (InputStream snapshot = storage.open(work.snapshotLocation())) {
                    writer.write(
                            snapshot,
                            output,
                            progress -> stateService.recordExportProgress(exportId, progress)
                    );
                }
            });
            stateService.completeExport(exportId, location);
        } catch (ReportDefinitionUnavailableException exception) {
            LOGGER.warn("Report definition became unavailable: exportId={}", exportId);
            stateService.failExport(exportId, "REPORT_DEFINITION_UNAVAILABLE");
        } catch (Exception exception) {
            LOGGER.error("Report export failed: exportId={}", exportId, exception);
            if (location != null) {
                try {
                    storage.delete(location);
                } catch (Exception cleanupException) {
                    LOGGER.warn("Unable to remove failed export artifact: exportId={}", exportId, cleanupException);
                }
            }
            stateService.failExport(exportId, "EXPORT_FAILED");
        }
    }
}
