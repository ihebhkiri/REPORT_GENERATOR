package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.report.config.ReportJobProperties;
import RHIS.com.RHIS.report.export.ReportExportWriter;
import RHIS.com.RHIS.report.model.ReportExportFormat;
import RHIS.com.RHIS.report.storage.FileSystemReportArtifactStorage;
import RHIS.com.RHIS.report.storage.ReportArtifactStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportExportWorkerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void publishesTheCompletedExportAtomicallyForwardsWriterProgressAndKeepsTheSnapshot() throws Exception {
        UUID exportId = UUID.randomUUID();
        UUID generationId = UUID.randomUUID();
        ReportArtifactStorage storage = storage();
        String snapshotLocation = writeSnapshot(storage, generationId);
        ReportJobStateService stateService = mock(ReportJobStateService.class);
        List<Integer> progress = new ArrayList<>();
        when(stateService.startExport(exportId)).thenReturn(Optional.of(work(exportId, generationId, snapshotLocation)));
        doAnswer(invocation -> {
            progress.add(invocation.getArgument(1));
            return null;
        }).when(stateService).recordExportProgress(eq(exportId), anyInt());
        String expectedLocation = generationId + "/report-" + exportId + ".xlsx";
        doAnswer(invocation -> {
            assertThat(Files.exists(temporaryDirectory.resolve(expectedLocation))).isTrue();
            return null;
        }).when(stateService).completeExport(exportId, expectedLocation);

        worker(stateService, storage, List.of(copyingWriter())).run(exportId);

        assertThat(progress).containsExactly(70, 20, 99);
        assertThat(Files.readString(temporaryDirectory.resolve(expectedLocation))).isEqualTo("export");
        assertThat(Files.exists(temporaryDirectory.resolve(snapshotLocation))).isTrue();
        assertThat(partialFiles(generationId)).isEmpty();
        verify(stateService).completeExport(exportId, expectedLocation);
    }

    @Test
    void removesThePartialArtifactWhenTheWriterFailsAndKeepsTheSnapshot() throws Exception {
        UUID exportId = UUID.randomUUID();
        UUID generationId = UUID.randomUUID();
        ReportArtifactStorage storage = storage();
        String snapshotLocation = writeSnapshot(storage, generationId);
        ReportJobStateService stateService = mock(ReportJobStateService.class);
        when(stateService.startExport(exportId)).thenReturn(Optional.of(work(exportId, generationId, snapshotLocation)));

        worker(stateService, storage, List.of(failingWriter())).run(exportId);

        assertThat(partialFiles(generationId)).isEmpty();
        assertThat(Files.exists(temporaryDirectory.resolve(snapshotLocation))).isTrue();
        assertThat(Files.exists(temporaryDirectory.resolve(generationId + "/report-" + exportId + ".xlsx"))).isFalse();
        verify(stateService).failExport(exportId, "EXPORT_FAILED");
    }

    @Test
    void removesAnAlreadyPublishedArtifactWhenCompletionFailsAndKeepsTheSnapshot() throws Exception {
        UUID exportId = UUID.randomUUID();
        UUID generationId = UUID.randomUUID();
        ReportArtifactStorage storage = storage();
        String snapshotLocation = writeSnapshot(storage, generationId);
        ReportJobStateService stateService = mock(ReportJobStateService.class);
        when(stateService.startExport(exportId)).thenReturn(Optional.of(work(exportId, generationId, snapshotLocation)));
        doThrow(new IllegalStateException("database unavailable"))
                .when(stateService).completeExport(exportId, generationId + "/report-" + exportId + ".xlsx");

        worker(stateService, storage, List.of(copyingWriter())).run(exportId);

        assertThat(Files.exists(temporaryDirectory.resolve(generationId + "/report-" + exportId + ".xlsx"))).isFalse();
        assertThat(partialFiles(generationId)).isEmpty();
        assertThat(Files.exists(temporaryDirectory.resolve(snapshotLocation))).isTrue();
        verify(stateService).failExport(exportId, "EXPORT_FAILED");
    }

    private ReportArtifactStorage storage() {
        ReportJobProperties properties = new ReportJobProperties();
        properties.setStorageRoot(temporaryDirectory);
        return new FileSystemReportArtifactStorage(properties);
    }

    private String writeSnapshot(ReportArtifactStorage storage, UUID generationId) throws IOException {
        return storage.writeAtomically(generationId, "snapshot.ndjson.gz", output -> output.write("snapshot".getBytes(StandardCharsets.UTF_8)));
    }

    private ReportJobStateService.ReportExportWork work(UUID exportId, UUID generationId, String snapshotLocation) {
        return new ReportJobStateService.ReportExportWork(
                exportId,
                generationId,
                snapshotLocation,
                "{\"rootDatasetId\":1,\"selectedFieldIds\":[10],\"filters\":[],\"sorts\":[]}",
                ReportExportFormat.XLSX
        );
    }

    private ReportExportWorker worker(
            ReportJobStateService stateService,
            ReportArtifactStorage storage,
            List<ReportExportWriter> writers
    ) {
        return new ReportExportWorker(
                stateService,
                storage,
                mock(ReportDefinitionResolver.class),
                new ObjectMapper(),
                writers
        );
    }

    private ReportExportWriter copyingWriter() {
        return new ReportExportWriter() {
            @Override
            public ReportExportFormat format() {
                return ReportExportFormat.XLSX;
            }

            @Override
            public void write(InputStream snapshot, OutputStream output, java.util.function.IntConsumer progress) throws IOException {
                progress.accept(70);
                progress.accept(20);
                output.write("export".getBytes(StandardCharsets.UTF_8));
                progress.accept(99);
            }
        };
    }

    private ReportExportWriter failingWriter() {
        return new ReportExportWriter() {
            @Override
            public ReportExportFormat format() {
                return ReportExportFormat.XLSX;
            }

            @Override
            public void write(InputStream snapshot, OutputStream output, java.util.function.IntConsumer progress) throws IOException {
                output.write("incomplete".getBytes(StandardCharsets.UTF_8));
                throw new IOException("writer failed");
            }
        };
    }

    private List<Path> partialFiles(UUID generationId) throws IOException {
        try (var files = Files.list(temporaryDirectory.resolve(generationId.toString()))) {
            return files.filter(path -> path.getFileName().toString().endsWith(".partial")).toList();
        }
    }
}
