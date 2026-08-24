package RHIS.com.RHIS.report.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public interface ReportArtifactStorage {
    String writeAtomically(UUID generationId, String fileName, ArtifactWriter writer) throws IOException;

    InputStream open(String location) throws IOException;

    void delete(String location) throws IOException;

    void deleteGeneration(UUID generationId) throws IOException;

    @FunctionalInterface
    interface ArtifactWriter {
        void write(OutputStream outputStream) throws Exception;
    }
}
