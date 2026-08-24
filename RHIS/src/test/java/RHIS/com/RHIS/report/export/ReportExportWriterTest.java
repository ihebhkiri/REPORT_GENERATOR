package RHIS.com.RHIS.report.export;

import RHIS.com.RHIS.dataset.model.DataSetFieldType;
import RHIS.com.RHIS.report.config.ReportJobProperties;
import RHIS.com.RHIS.report.snapshot.ReportSnapshotMetadata;
import RHIS.com.RHIS.report.snapshot.ReportSnapshotReader;
import RHIS.com.RHIS.report.snapshot.ReportSnapshotWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportExportWriterTest {

    private static final int CONCURRENT_EXPORT_COUNT = 4;

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final ReportSnapshotReader reader = new ReportSnapshotReader(mapper);

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesOrderedTypedXlsxCellsFromSnapshot() throws Exception {
        byte[] snapshot = snapshot(2);
        ReportJobProperties properties = new ReportJobProperties();
        properties.setXlsxRowWindow(1);
        XlsxReportExportWriter writer = new XlsxReportExportWriter(reader, properties);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        writer.write(new ByteArrayInputStream(snapshot), output, ignored -> { });

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Nom");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Actif");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Ligne 1");
            assertThat(sheet.getRow(1).getCell(1).getBooleanCellValue()).isTrue();
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    void writesLegacyTemporalRepresentationsAsTypedXlsxCells() throws Exception {
        ReportSnapshotMetadata metadata = new ReportSnapshotMetadata(
                List.of(
                        new ReportSnapshotMetadata.Column("field_1", "Date", DataSetFieldType.DATE),
                        new ReportSnapshotMetadata.Column("field_2", "Date et heure", DataSetFieldType.DATE_TIME),
                        new ReportSnapshotMetadata.Column("field_3", "Date avec zone", DataSetFieldType.OFFSET_DATE_TIME)
                ),
                1
        );
        ByteArrayOutputStream snapshot = new ByteArrayOutputStream();
        new ReportSnapshotWriter(mapper).write(snapshot, metadata, sink -> sink.write(Map.of(
                "field_1", LocalDate.of(2022, 1, 3),
                "field_2", LocalDateTime.of(2022, 1, 3, 14, 30, 15),
                "field_3", OffsetDateTime.parse("2022-01-03T14:30:15+02:00")
        )));
        XlsxReportExportWriter writer = new XlsxReportExportWriter(reader, new ReportJobProperties());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        writer.write(new ByteArrayInputStream(snapshot.toByteArray()), output, ignored -> { });

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            var row = workbook.getSheetAt(0).getRow(1);
            assertThat(row.getCell(0).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(2022, 1, 3));
            assertThat(row.getCell(1).getLocalDateTimeCellValue())
                    .isEqualTo(LocalDateTime.of(2022, 1, 3, 14, 30, 15));
            assertThat(row.getCell(2).getLocalDateTimeCellValue())
                    .isEqualTo(LocalDateTime.of(2022, 1, 3, 12, 30, 15));
        }
    }

    @Test
    void writesAnEmptyXlsxWithoutClosingTheCallerOutput() throws Exception {
        ReportSnapshotMetadata metadata = new ReportSnapshotMetadata(
                List.of(new ReportSnapshotMetadata.Column("field_1", "Nom", DataSetFieldType.TEXT)),
                0
        );
        ByteArrayOutputStream snapshot = new ByteArrayOutputStream();
        new ReportSnapshotWriter(mapper).write(snapshot, metadata, ignored -> { });
        CloseTrackingOutputStream output = new CloseTrackingOutputStream();
        List<Integer> progress = new ArrayList<>();

        new XlsxReportExportWriter(reader, new ReportJobProperties()).write(
                new ByteArrayInputStream(snapshot.toByteArray()), output, progress::add
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("Nom");
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isZero();
        }
        assertThat(progress).containsExactly(99);
        assertThat(output.closed).isFalse();
    }

    @Test
    void writesOneXlsxRowWithNullAsABlankCellAndMonotonicProgress() throws Exception {
        ReportSnapshotMetadata metadata = new ReportSnapshotMetadata(
                List.of(
                        new ReportSnapshotMetadata.Column("field_1", "Nom", DataSetFieldType.TEXT),
                        new ReportSnapshotMetadata.Column("field_2", "Nombre", DataSetFieldType.INTEGER)
                ),
                1
        );
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("field_1", "Unique");
        row.put("field_2", null);
        ByteArrayOutputStream snapshot = new ByteArrayOutputStream();
        new ReportSnapshotWriter(mapper).write(snapshot, metadata, sink -> sink.write(row));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        List<Integer> progress = new ArrayList<>();

        new XlsxReportExportWriter(reader, new ReportJobProperties()).write(
                new ByteArrayInputStream(snapshot.toByteArray()), output, progress::add
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            var xlsxRow = workbook.getSheetAt(0).getRow(1);
            assertThat(xlsxRow.getCell(0).getStringCellValue()).isEqualTo("Unique");
            assertThat(xlsxRow.getCell(1).getCellType()).isEqualTo(org.apache.poi.ss.usermodel.CellType.BLANK);
        }
        assertThat(progress).isSorted().endsWith(99);
    }

    @Test
    void stopsXlsxExportWhenTheThreadIsInterrupted() throws Exception {
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> new XlsxReportExportWriter(reader, new ReportJobProperties()).write(
                    new ByteArrayInputStream(snapshot(1)), new ByteArrayOutputStream(), ignored -> { }
            )).isInstanceOf(InterruptedIOException.class);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void writesEveryPdfRowAcrossPagesAndRepeatsHeaders() throws Exception {
        byte[] snapshot = snapshot(120);
        PdfReportExportWriter writer = pdfWriter();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        List<Integer> progressValues = new ArrayList<>();

        writer.write(new ByteArrayInputStream(snapshot), output, progressValues::add);

        try (PdfReader document = new PdfReader(output.toByteArray())) {
            String text = extractText(document);
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(text).contains("Ligne 1", "Ligne 120");
            assertThat(text.split("Nom", -1).length - 1).isEqualTo(document.getNumberOfPages());
            assertThat(text).contains("Rapport RHIS", "Page 1");

            PdfDictionary resources = document.getPageN(1).getAsDict(PdfName.RESOURCES);
            assertThat(resources.getAsDict(PdfName.XOBJECT).size()).isGreaterThan(0);
        }
        assertThat(progressValues).isSorted().endsWith(99);
        assertSwapDirectoryHasNoFiles(reportProperties());
    }

    @Test
    void writesEmptyPdfWithBrandingAndPreservesWrappedUnicodeText() throws Exception {
        ByteArrayOutputStream emptySnapshot = new ByteArrayOutputStream();
        new ReportSnapshotWriter(mapper).write(
                emptySnapshot,
                new ReportSnapshotMetadata(
                        List.of(new ReportSnapshotMetadata.Column("field_1", "Libellé", DataSetFieldType.TEXT)),
                        0
                ),
                ignored -> { }
        );
        ByteArrayOutputStream emptyPdf = new ByteArrayOutputStream();
        pdfWriter().write(new ByteArrayInputStream(emptySnapshot.toByteArray()), emptyPdf, ignored -> { });

        try (PdfReader document = new PdfReader(emptyPdf.toByteArray())) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            assertThat(extractText(document)).contains("Rapport RHIS", "Libellé", "Page 1");
        }

        String longValue = "Élève déjà inscrit à l’établissement — données complètes ".repeat(40)
                + "MARQUEUR_UNICODE_FINAL";
        ByteArrayOutputStream wrappedSnapshot = new ByteArrayOutputStream();
        new ReportSnapshotWriter(mapper).write(
                wrappedSnapshot,
                new ReportSnapshotMetadata(
                        List.of(new ReportSnapshotMetadata.Column("field_1", "Description", DataSetFieldType.TEXT)),
                        1
                ),
                sink -> sink.write(Map.of("field_1", longValue))
        );
        ByteArrayOutputStream wrappedPdf = new ByteArrayOutputStream();
        pdfWriter().write(new ByteArrayInputStream(wrappedSnapshot.toByteArray()), wrappedPdf, ignored -> { });

        try (PdfReader document = new PdfReader(wrappedPdf.toByteArray())) {
            String text = extractText(document);
            assertThat(text).contains("Élève", "établissement", "MARQUEUR_UNICODE_FINAL");
            assertThat(text).doesNotContain("...");
        }
    }

    @Test
    void writesNullPdfValuesAsBlankCells() throws Exception {
        ReportSnapshotMetadata metadata = new ReportSnapshotMetadata(
                List.of(
                        new ReportSnapshotMetadata.Column("field_1", "Nom", DataSetFieldType.TEXT),
                        new ReportSnapshotMetadata.Column("field_2", "Commentaire", DataSetFieldType.TEXT)
                ),
                1
        );
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("field_1", "Valeur prÃ©sente");
        row.put("field_2", null);
        ByteArrayOutputStream snapshot = new ByteArrayOutputStream();
        new ReportSnapshotWriter(mapper).write(snapshot, metadata, sink -> sink.write(row));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        pdfWriter().write(new ByteArrayInputStream(snapshot.toByteArray()), output, ignored -> { });

        try (PdfReader document = new PdfReader(output.toByteArray())) {
            assertThat(extractText(document)).contains("Valeur prÃ©sente").doesNotContain("null");
        }
    }

    @Test
    void preservesTheOrderOfDistinctPdfRows() throws Exception {
        ReportSnapshotMetadata metadata = new ReportSnapshotMetadata(
                List.of(new ReportSnapshotMetadata.Column("field_1", "Nom", DataSetFieldType.TEXT)),
                3
        );
        ByteArrayOutputStream snapshot = new ByteArrayOutputStream();
        new ReportSnapshotWriter(mapper).write(snapshot, metadata, sink -> {
            sink.write(Map.of("field_1", "PremiÃ¨re ligne distincte"));
            sink.write(Map.of("field_1", "DeuxiÃ¨me ligne distincte"));
            sink.write(Map.of("field_1", "TroisiÃ¨me ligne distincte"));
        });
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        pdfWriter().write(new ByteArrayInputStream(snapshot.toByteArray()), output, ignored -> { });

        try (PdfReader document = new PdfReader(output.toByteArray())) {
            String text = extractText(document);
            assertThat(text).contains(
                    "PremiÃ¨re ligne distincte",
                    "DeuxiÃ¨me ligne distincte",
                    "TroisiÃ¨me ligne distincte"
            );
            assertThat(text.indexOf("PremiÃ¨re ligne distincte"))
                    .isLessThan(text.indexOf("DeuxiÃ¨me ligne distincte"));
            assertThat(text.indexOf("DeuxiÃ¨me ligne distincte"))
                    .isLessThan(text.indexOf("TroisiÃ¨me ligne distincte"));
        }
    }

    @Test
    void supportsConcurrentPdfExportsSharingTheSameStorageRoot() throws Exception {
        byte[] snapshot = snapshot(1);
        ReportJobProperties properties = reportProperties();
        PdfReportExportWriter writer = new PdfReportExportWriter(reader, properties);
        CountDownLatch workersReady = new CountDownLatch(CONCURRENT_EXPORT_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_EXPORT_COUNT);
        try {
            List<Future<byte[]>> exports = new ArrayList<>();
            for (int index = 0; index < CONCURRENT_EXPORT_COUNT; index++) {
                exports.add(executor.submit(() -> exportPdf(writer, snapshot, workersReady, start)));
            }
            assertThat(workersReady.await(30, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            for (Future<byte[]> export : exports) {
                try (PdfReader document = new PdfReader(export.get(60, TimeUnit.SECONDS))) {
                    assertThat(extractText(document)).contains("Ligne 1");
                }
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
        assertSwapDirectoryHasNoFiles(properties);
    }

    @Test
    void cleansJasperSwapFilesAfterFailureAndCancellation() throws Exception {
        ReportJobProperties properties = reportProperties();
        PdfReportExportWriter writer = new PdfReportExportWriter(reader, properties);

        assertThatThrownBy(() -> writer.write(
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                new ByteArrayOutputStream(),
                ignored -> { }
        )).isInstanceOf(Exception.class);
        assertSwapDirectoryHasNoFiles(properties);

        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> writer.write(
                    new ByteArrayInputStream(snapshot(1)),
                    new ByteArrayOutputStream(),
                    ignored -> { }
            )).isInstanceOf(Exception.class);
        } finally {
            Thread.interrupted();
        }
        assertSwapDirectoryHasNoFiles(properties);
    }

    @Test
    void exportsFiftyThousandRowsWithoutTruncationAndRecordsPeakHeap() throws Exception {
        int rowCount = 50_000;
        Path snapshot = temporaryDirectory.resolve("load-test.ndjson.gz");
        Path xlsx = temporaryDirectory.resolve("load-test.xlsx");
        Path pdf = temporaryDirectory.resolve("load-test.pdf");

        long snapshotPeak = measurePeakHeap(() -> {
            try (OutputStream output = Files.newOutputStream(snapshot)) {
                writeSnapshot(output, rowCount);
            }
        });

        ReportJobProperties properties = new ReportJobProperties();
        properties.setXlsxRowWindow(100);
        long xlsxPeak = measurePeakHeap(() -> {
            try (InputStream input = Files.newInputStream(snapshot);
                 OutputStream output = Files.newOutputStream(xlsx)) {
                new XlsxReportExportWriter(reader, properties).write(input, output, ignored -> { });
            }
        });
        long pdfPeak = measurePeakHeap(() -> {
            try (InputStream input = Files.newInputStream(snapshot);
                 OutputStream output = Files.newOutputStream(pdf)) {
                pdfWriter().write(input, output, ignored -> { });
            }
        });

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(xlsx))) {
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isEqualTo(rowCount);
        }
        try (PdfReader document = new PdfReader(pdf.toString())) {
            assertThat(new PdfTextExtractor(document).getTextFromPage(document.getNumberOfPages()))
                    .contains("Ligne 50000");
        }

        System.out.printf(
                "REPORT_LOAD_HEAP rows=%d snapshotPeakBytes=%d xlsxPeakBytes=%d pdfPeakBytes=%d%n",
                rowCount,
                snapshotPeak,
                xlsxPeak,
                pdfPeak
        );
    }

    private byte[] snapshot(int rowCount) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeSnapshot(output, rowCount);
        return output.toByteArray();
    }

    private PdfReportExportWriter pdfWriter() {
        return new PdfReportExportWriter(reader, reportProperties());
    }

    private ReportJobProperties reportProperties() {
        ReportJobProperties properties = new ReportJobProperties();
        properties.setStorageRoot(temporaryDirectory);
        properties.setJasperVirtualizerMaxPages(2);
        return properties;
    }

    private String extractText(PdfReader document) throws Exception {
        PdfTextExtractor extractor = new PdfTextExtractor(document);
        StringBuilder text = new StringBuilder();
        for (int page = 1; page <= document.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }
        return text.toString();
    }

    private void assertSwapDirectoryHasNoFiles(ReportJobProperties properties) throws Exception {
        Path swapDirectory = properties.getStorageRoot().resolve("jasper-swap");
        if (Files.notExists(swapDirectory)) {
            return;
        }
        try (var paths = Files.list(swapDirectory)) {
            assertThat(paths).isEmpty();
        }
    }

    private byte[] exportPdf(
            PdfReportExportWriter writer,
            byte[] snapshot,
            CountDownLatch workersReady,
            CountDownLatch start
    ) {
        try {
            workersReady.countDown();
            if (!start.await(30, TimeUnit.SECONDS)) {
                throw new AssertionError("Concurrent exports were not started");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            writer.write(new ByteArrayInputStream(snapshot), output, ignored -> { });
            return output.toByteArray();
        } catch (Exception exception) {
            throw new AssertionError("Concurrent PDF export failed", exception);
        }
    }

    private void writeSnapshot(OutputStream output, int rowCount) throws Exception {
        ReportSnapshotMetadata metadata = new ReportSnapshotMetadata(
                List.of(
                        new ReportSnapshotMetadata.Column("field_1", "Nom", DataSetFieldType.TEXT),
                        new ReportSnapshotMetadata.Column("field_2", "Actif", DataSetFieldType.BOOLEAN)
                ),
                rowCount
        );
        new ReportSnapshotWriter(mapper).write(output, metadata, sink -> {
            for (int row = 1; row <= rowCount; row++) {
                sink.write(Map.of("field_1", "Ligne " + row, "field_2", row % 2 == 1));
            }
        });
    }

    private long measurePeakHeap(ThrowingRunnable action) throws Exception {
        System.gc();
        long baseline = usedHeap();
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicLong peak = new AtomicLong(baseline);
        Thread sampler = new Thread(() -> {
            while (running.get()) {
                peak.accumulateAndGet(usedHeap(), Math::max);
                try {
                    Thread.sleep(5);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "report-load-heap-sampler");
        sampler.setDaemon(true);
        sampler.start();
        try {
            action.run();
        } finally {
            peak.accumulateAndGet(usedHeap(), Math::max);
            running.set(false);
            sampler.interrupt();
            sampler.join();
        }
        return Math.max(0, peak.get() - baseline);
    }

    private long usedHeap() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class CloseTrackingOutputStream extends ByteArrayOutputStream {
        private boolean closed;

        @Override
        public void close() throws java.io.IOException {
            closed = true;
            super.close();
        }
    }
}
