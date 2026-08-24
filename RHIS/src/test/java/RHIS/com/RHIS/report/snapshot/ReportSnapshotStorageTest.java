package RHIS.com.RHIS.report.snapshot;

import RHIS.com.RHIS.dataset.model.DataSetFieldType;
import RHIS.com.RHIS.report.config.ReportJobProperties;
import RHIS.com.RHIS.report.storage.FileSystemReportArtifactStorage;
import RHIS.com.RHIS.report.storage.ReportArtifactStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportSnapshotStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void streamsTenThousandRowsThroughAnAtomicCompressedSnapshot() throws Exception {
        ReportJobProperties properties = new ReportJobProperties();
        properties.setStorageRoot(temporaryDirectory);
        ReportArtifactStorage storage = new FileSystemReportArtifactStorage(properties);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ReportSnapshotWriter writer = new ReportSnapshotWriter(mapper);
        ReportSnapshotReader reader = new ReportSnapshotReader(mapper);
        UUID generationId = UUID.randomUUID();
        ReportSnapshotMetadata metadata = new ReportSnapshotMetadata(
                List.of(new ReportSnapshotMetadata.Column("field_1", "Identifiant", DataSetFieldType.INTEGER)),
                10_000
        );

        String location = storage.writeAtomically(generationId, "snapshot.ndjson.gz", output ->
                writer.write(output, metadata, sink -> {
                    for (long value = 1; value <= 10_000; value++) {
                        sink.write(Map.of("field_1", value));
                    }
                })
        );

        AtomicLong count = new AtomicLong();
        AtomicLong lastValue = new AtomicLong();
        try (InputStream input = storage.open(location)) {
            reader.read(input, new ReportSnapshotReader.SnapshotConsumer() {
                @Override
                public void start(ReportSnapshotMetadata actual) {
                    assertThat(actual).isEqualTo(metadata);
                }

                @Override
                public void row(Map<String, Object> row) {
                    count.incrementAndGet();
                    lastValue.set(((Number) row.get("field_1")).longValue());
                }
            });
        }

        assertThat(count).hasValue(10_000);
        assertThat(lastValue).hasValue(10_000);
        assertThat(Files.exists(temporaryDirectory.resolve(location))).isTrue();
        assertThat(Files.list(temporaryDirectory.resolve(generationId.toString())))
                .noneMatch(path -> path.getFileName().toString().endsWith(".partial"));
    }

    @Test
    void removesPartialFileWhenAtomicWriterFailsAndRejectsTraversal() {
        ReportJobProperties properties = new ReportJobProperties();
        properties.setStorageRoot(temporaryDirectory);
        ReportArtifactStorage storage = new FileSystemReportArtifactStorage(properties);
        UUID generationId = UUID.randomUUID();

        assertThatThrownBy(() -> storage.writeAtomically(generationId, "snapshot.ndjson.gz", output -> {
            output.write(1);
            throw new IllegalStateException("boom");
        })).isInstanceOf(java.io.IOException.class);
        assertThatThrownBy(() -> storage.open("../secret"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exposesMetadataAndRowsThroughTheStreamingCursor() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ReportSnapshotMetadata metadata = new ReportSnapshotMetadata(
                List.of(new ReportSnapshotMetadata.Column("field_1", "Nom", DataSetFieldType.TEXT)),
                2
        );
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        new ReportSnapshotWriter(mapper).write(output, metadata, sink -> {
            sink.write(Map.of("field_1", "Première"));
            sink.write(Map.of("field_1", "Deuxième"));
        });

        ReportSnapshotReader reader = new ReportSnapshotReader(mapper);
        try (ReportSnapshotReader.SnapshotCursor cursor = reader.openCursor(
                new java.io.ByteArrayInputStream(output.toByteArray())
        )) {
            assertThat(cursor.metadata()).isEqualTo(metadata);
            assertThat(cursor.next()).isTrue();
            assertThat(cursor.currentRow()).containsEntry("field_1", "Première");
            assertThat(cursor.next()).isTrue();
            assertThat(cursor.currentRow()).containsEntry("field_1", "Deuxième");
            assertThat(cursor.next()).isFalse();
            assertThatThrownBy(cursor::currentRow).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void consumesAnEmptySnapshotAndClosesTheSuppliedStreamAfterSuccess() throws Exception {
        ReportSnapshotMetadata metadata = metadata(0);
        CloseTrackingInputStream input = new CloseTrackingInputStream(snapshot(metadata, List.of()));
        AtomicBoolean started = new AtomicBoolean();
        AtomicBoolean ended = new AtomicBoolean();
        AtomicLong rows = new AtomicLong();

        new ReportSnapshotReader(new ObjectMapper().findAndRegisterModules()).read(input,
                new ReportSnapshotReader.SnapshotConsumer() {
                    @Override
                    public void start(ReportSnapshotMetadata actual) {
                        started.set(true);
                        assertThat(actual).isEqualTo(metadata);
                    }

                    @Override
                    public void row(Map<String, Object> row) {
                        rows.incrementAndGet();
                    }

                    @Override
                    public void end() {
                        ended.set(true);
                    }
                });

        assertThat(started).isTrue();
        assertThat(rows).hasValue(0);
        assertThat(ended).isTrue();
        assertThat(input.closed).isTrue();
    }

    @Test
    void consumesOneRowAndCallsEndOnlyAfterEof() throws Exception {
        ReportSnapshotMetadata metadata = metadata(1);
        AtomicLong rows = new AtomicLong();
        AtomicBoolean ended = new AtomicBoolean();

        new ReportSnapshotReader(new ObjectMapper().findAndRegisterModules()).read(
                new ByteArrayInputStream(snapshot(metadata, List.of(Map.of("field_1", "unique")))),
                new ReportSnapshotReader.SnapshotConsumer() {
                    @Override
                    public void start(ReportSnapshotMetadata actual) {
                        assertThat(actual).isEqualTo(metadata);
                    }

                    @Override
                    public void row(Map<String, Object> row) {
                        rows.incrementAndGet();
                        assertThat(row).containsEntry("field_1", "unique");
                        assertThat(ended).isFalse();
                    }

                    @Override
                    public void end() {
                        ended.set(true);
                    }
                });

        assertThat(rows).hasValue(1);
        assertThat(ended).isTrue();
    }

    @Test
    void doesNotCallEndWhenARowCannotBeParsed() throws Exception {
        ReportSnapshotMetadata metadata = metadata(1);
        AtomicBoolean started = new AtomicBoolean();
        AtomicBoolean ended = new AtomicBoolean();
        byte[] malformedSnapshot = gzipNdjson(
                new ObjectMapper().findAndRegisterModules().writeValueAsString(metadata),
                "{not-json}"
        );

        assertThatThrownBy(() -> new ReportSnapshotReader(new ObjectMapper().findAndRegisterModules()).read(
                new ByteArrayInputStream(malformedSnapshot), new ReportSnapshotReader.SnapshotConsumer() {
                    @Override
                    public void start(ReportSnapshotMetadata actual) {
                        started.set(true);
                    }

                    @Override
                    public void row(Map<String, Object> row) {
                    }

                    @Override
                    public void end() {
                        ended.set(true);
                    }
                }
        )).isInstanceOf(java.io.IOException.class);

        assertThat(started).isTrue();
        assertThat(ended).isFalse();
    }

    @Test
    void doesNotCallEndWhenTheThreadIsInterrupted() throws Exception {
        AtomicBoolean ended = new AtomicBoolean();
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> new ReportSnapshotReader(new ObjectMapper().findAndRegisterModules()).read(
                    new ByteArrayInputStream(snapshot(metadata(1), List.of(Map.of("field_1", "row")))),
                    new ReportSnapshotReader.SnapshotConsumer() {
                        @Override
                        public void start(ReportSnapshotMetadata metadata) {
                        }

                        @Override
                        public void row(Map<String, Object> row) {
                        }

                        @Override
                        public void end() {
                            ended.set(true);
                        }
                    }
            )).isInstanceOf(InterruptedIOException.class);
        } finally {
            Thread.interrupted();
        }

        assertThat(ended).isFalse();
    }

    private ReportSnapshotMetadata metadata(long rowCount) {
        return new ReportSnapshotMetadata(
                List.of(new ReportSnapshotMetadata.Column("field_1", "Nom", DataSetFieldType.TEXT)),
                rowCount
        );
    }

    private byte[] snapshot(ReportSnapshotMetadata metadata, List<Map<String, Object>> rows) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new ReportSnapshotWriter(new ObjectMapper().findAndRegisterModules()).write(output, metadata, sink -> {
            for (Map<String, Object> row : rows) {
                sink.write(row);
            }
        });
        return output.toByteArray();
    }

    private byte[] gzipNdjson(String... lines) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            for (String line : lines) {
                gzip.write(line.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                gzip.write('\n');
            }
        }
        return output.toByteArray();
    }

    private static final class CloseTrackingInputStream extends FilterInputStream {
        private boolean closed;

        private CloseTrackingInputStream(byte[] bytes) {
            super(new ByteArrayInputStream(bytes));
        }

        @Override
        public void close() throws java.io.IOException {
            closed = true;
            super.close();
        }
    }
}
