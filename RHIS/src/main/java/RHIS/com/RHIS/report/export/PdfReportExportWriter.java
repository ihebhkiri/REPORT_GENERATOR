package RHIS.com.RHIS.report.export;

import RHIS.com.RHIS.report.config.ReportJobProperties;
import RHIS.com.RHIS.report.model.ReportExportFormat;
import RHIS.com.RHIS.report.snapshot.ReportSnapshotMetadata;
import RHIS.com.RHIS.report.snapshot.ReportSnapshotReader;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.fill.JRSwapFileVirtualizer;
import net.sf.jasperreports.engine.util.JRSwapFile;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.pdf.SimplePdfExporterConfiguration;
import net.sf.jasperreports.pdf.SimplePdfReportConfiguration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

@Component
public class PdfReportExportWriter implements ReportExportWriter {

    private static final String TEMPLATE_RESOURCE = "/reports/report-export-base.jrxml";
    private static final String LOGO_RESOURCE = "/reports/burger-king-logo.svg";
    private static final String FOOTER_RESOURCE = "/reports/burger-king-footer.svg";
    private static final String REPORT_TITLE = "Rapport Burger King";
    private static final int SWAP_BLOCK_SIZE = 4_096;
    private static final int SWAP_MIN_GROW_COUNT = 100;
    private static final Object JASPER_COMPILE_LOCK = new Object();

    private final ReportSnapshotReader snapshotReader;
    private final ReportJobProperties properties;

    public PdfReportExportWriter(ReportSnapshotReader snapshotReader, ReportJobProperties properties) {
        this.snapshotReader = snapshotReader;
        this.properties = properties;
    }

    @Override
    public ReportExportFormat format() {
        return ReportExportFormat.PDF;
    }

    @Override
    public void write(InputStream snapshot, OutputStream output, IntConsumer progress) throws Exception {
        Path swapDirectory = properties.getStorageRoot().resolve("jasper-swap");
        Files.createDirectories(swapDirectory);
        JRSwapFile swapFile = new JRSwapFile(swapDirectory.toString(), SWAP_BLOCK_SIZE, SWAP_MIN_GROW_COUNT);
        JRSwapFileVirtualizer virtualizer = new JRSwapFileVirtualizer(
                properties.getJasperVirtualizerMaxPages(),
                swapFile,
                true
        );

        try (ReportSnapshotReader.SnapshotCursor cursor = snapshotReader.openCursor(snapshot);
             InputStream template = requiredResource(TEMPLATE_RESOURCE);
             InputStream logo = requiredResource(LOGO_RESOURCE);
             InputStream footer = requiredResource(FOOTER_RESOURCE)) {
            JasperDesign design = JRXmlLoader.load(template);
            JasperDynamicTableConfigurer.configure(design, cursor.metadata());

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("REPORT_TITLE", REPORT_TITLE);
            parameters.put("REPORT_LOGO", logo);
            parameters.put("REPORT_FOOTER", footer);
            parameters.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);

            JasperReport report;
            synchronized (JASPER_COMPILE_LOCK) {
                report = JasperCompileManager.compileReport(design);
            }
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    report,
                    parameters,
                    new SnapshotDataSource(cursor, progress)
            );
            ensureNotInterrupted();
            export(jasperPrint, output);
            progress.accept(99);
        } finally {
            virtualizer.cleanup();
        }
    }

    private void export(JasperPrint jasperPrint, OutputStream output) throws JRException {
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(output));

        SimplePdfReportConfiguration reportConfiguration = new SimplePdfReportConfiguration();
        reportConfiguration.setForceLineBreakPolicy(true);
        exporter.setConfiguration(reportConfiguration);

        SimplePdfExporterConfiguration exporterConfiguration = new SimplePdfExporterConfiguration();
        exporterConfiguration.setCompressed(true);
        exporterConfiguration.setMetadataTitle(REPORT_TITLE);
        exporterConfiguration.setMetadataCreator("Burger King");
        exporter.setConfiguration(exporterConfiguration);
        exporter.exportReport();
    }

    private InputStream requiredResource(String resource) throws IOException {
        InputStream input = PdfReportExportWriter.class.getResourceAsStream(resource);
        if (input == null) {
            throw new IOException("Missing report resource: " + resource);
        }
        return input;
    }

    private void ensureNotInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Report export cancelled");
        }
    }

    private static final class SnapshotDataSource implements JRDataSource {
        private final ReportSnapshotReader.SnapshotCursor cursor;
        private final IntConsumer progress;
        private final Map<String, ReportSnapshotMetadata.Column> columnsByField = new HashMap<>();
        private final long rowCount;
        private long currentRow;

        private SnapshotDataSource(ReportSnapshotReader.SnapshotCursor cursor, IntConsumer progress) {
            this.cursor = cursor;
            this.progress = progress;
            this.rowCount = cursor.metadata().rowCount();
            for (int index = 0; index < cursor.metadata().columns().size(); index++) {
                columnsByField.put(JasperDynamicTableConfigurer.fieldName(index), cursor.metadata().columns().get(index));
            }
        }

        @Override
        public boolean next() throws JRException {
            try {
                boolean hasNext = cursor.next();
                if (hasNext) {
                    currentRow++;
                    progress.accept(exportProgress(currentRow, rowCount));
                }
                return hasNext;
            } catch (IOException exception) {
                throw new JRException("Unable to read the report snapshot", exception);
            }
        }

        @Override
        public Object getFieldValue(JRField field) throws JRException {
            ReportSnapshotMetadata.Column column = columnsByField.get(field.getName());
            if (column == null) {
                throw new JRException("Unknown report field: " + field.getName());
            }
            Object value = cursor.currentRow().get(column.key());
            return value == null ? "" : value.toString();
        }

        private static int exportProgress(long current, long total) {
            return total <= 0 ? 95 : 5 + (int) Math.min(90, (current * 90) / total);
        }
    }
}
