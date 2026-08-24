package RHIS.com.RHIS.report.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

@Component
public class ReportSnapshotWriter {

    private final ObjectMapper objectMapper;

    public ReportSnapshotWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(OutputStream output, ReportSnapshotMetadata metadata, RowsSource rows) throws Exception {
        try (GZIPOutputStream gzip = new GZIPOutputStream(output);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzip, StandardCharsets.UTF_8))) {
            writeLine(writer, metadata);
            rows.writeTo(row -> writeLine(writer, row));
        }
    }

    private void writeLine(BufferedWriter writer, Object value) throws IOException {
        writer.write(objectMapper.writeValueAsString(value));
        writer.newLine();
    }

    @FunctionalInterface
    public interface RowsSource {
        void writeTo(RowSink sink) throws Exception;
    }

    @FunctionalInterface
    public interface RowSink {
        void write(Map<String, Object> row) throws IOException;
    }
}
