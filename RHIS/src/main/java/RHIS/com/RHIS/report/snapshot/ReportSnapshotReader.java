package RHIS.com.RHIS.report.snapshot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Component
public class ReportSnapshotReader {

    private static final TypeReference<Map<String, Object>> ROW_TYPE = new TypeReference<>() { };
    private final ObjectMapper objectMapper;

    public ReportSnapshotReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void read(InputStream input, SnapshotConsumer consumer) throws Exception {
        try (SnapshotCursor cursor = openCursor(input)) {
            consumer.start(cursor.metadata());
            while (cursor.next()) {
                consumer.row(cursor.currentRow());
            }
            consumer.end();
        }
    }

    public SnapshotCursor openCursor(InputStream input) throws IOException {
        return new SnapshotCursor(input);
    }

    /**
     * Forward-only cursor over a compressed NDJSON snapshot.
     * <p>
     * Metadata is read while the cursor is opened and is therefore available before the first
     * call to {@link #next()}. A successful {@code next()} positions the cursor on one row, which
     * can then be obtained with {@link #currentRow()}; after EOF, the cursor is not positioned on
     * a row. Closing this cursor closes the whole input chain, including the supplied input stream.
     */
    public final class SnapshotCursor implements AutoCloseable {
        private final BufferedReader reader;
        private final ReportSnapshotMetadata metadata;
        private Map<String, Object> currentRow;

        private SnapshotCursor(InputStream input) throws IOException {
            reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(input), StandardCharsets.UTF_8));
            boolean initialized = false;
            try {
                String metadataLine = reader.readLine();
                if (metadataLine == null) {
                    throw new IOException("Empty report snapshot");
                }
                metadata = objectMapper.readValue(metadataLine, ReportSnapshotMetadata.class);
                initialized = true;
            } finally {
                if (!initialized) {
                    reader.close();
                }
            }
        }

        /**
         * Returns the metadata read during cursor initialization, before any row is consumed.
         */
        public ReportSnapshotMetadata metadata() {
            return metadata;
        }

        /**
         * Advances to the next row.
         *
         * @return {@code true} when the cursor is positioned on a row, or {@code false} at EOF
         * @throws InterruptedIOException when the export thread has been interrupted
         */
        public boolean next() throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException("Report export cancelled");
            }
            String line = reader.readLine();
            if (line == null) {
                currentRow = null;
                return false;
            }
            currentRow = objectMapper.readValue(line, ROW_TYPE);
            return true;
        }

        /**
         * Returns the row at the current cursor position.
         *
         * @throws IllegalStateException when {@link #next()} has not returned {@code true}, or
         *                               after EOF
         */
        public Map<String, Object> currentRow() {
            if (currentRow == null) {
                throw new IllegalStateException("Snapshot cursor is not positioned on a row");
            }
            return currentRow;
        }

        /**
         * Closes the buffered reader, gzip stream, and the input stream supplied to
         * {@link ReportSnapshotReader#openCursor(InputStream)}.
         */
        @Override
        public void close() throws IOException {
            reader.close();
        }
    }

    public interface SnapshotConsumer {
        void start(ReportSnapshotMetadata metadata) throws Exception;
        void row(Map<String, Object> row) throws Exception;
        /**
         * Called only after metadata and every row have been consumed successfully. It is not
         * called when parsing, consumption, or cancellation fails.
         */
        default void end() throws Exception { }
    }
}
