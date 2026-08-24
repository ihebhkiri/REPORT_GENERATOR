package RHIS.com.RHIS.report.storage;

import RHIS.com.RHIS.report.config.ReportJobProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Comparator;
import java.util.UUID;

@Component
public class FileSystemReportArtifactStorage implements ReportArtifactStorage {

    private final Path root;

    public FileSystemReportArtifactStorage(ReportJobProperties properties) {
        this.root = properties.getStorageRoot().toAbsolutePath().normalize();
    }

    @Override
    public String writeAtomically(UUID generationId, String fileName, ArtifactWriter writer) throws IOException {
        if (!fileName.equals(Path.of(fileName).getFileName().toString())) {
            throw new IllegalArgumentException("Invalid report artifact name");
        }
        Path directory = resolve(generationId.toString());
        Files.createDirectories(directory);
        Path target = resolve(generationId + "/" + fileName);
        Path partial = Files.createTempFile(directory, fileName + '.', ".partial");
        try {
            try (OutputStream output = Files.newOutputStream(partial, StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write(output);
            }
            try {
                Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return generationId + "/" + fileName;
        } catch (Exception exception) {
            Files.deleteIfExists(partial);
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Unable to write report artifact", exception);
        }
    }

    @Override
    public InputStream open(String location) throws IOException {
        return Files.newInputStream(resolve(location), StandardOpenOption.READ);
    }

    @Override
    public void delete(String location) throws IOException {
        Files.deleteIfExists(resolve(location));
    }

    @Override
    public void deleteGeneration(UUID generationId) throws IOException {
        Path directory = resolve(generationId.toString());
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new StorageDeleteException(exception);
                }
            });
        } catch (StorageDeleteException exception) {
            throw exception.cause;
        }
    }

    private Path resolve(String relativeLocation) {
        Path resolved = root.resolve(relativeLocation).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid report artifact location");
        }
        return resolved;
    }

    private static final class StorageDeleteException extends RuntimeException {
        private final IOException cause;

        private StorageDeleteException(IOException cause) {
            super(cause);
            this.cause = cause;
        }
    }
}
