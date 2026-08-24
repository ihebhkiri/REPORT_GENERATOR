package RHIS.com.RHIS.report.export;

import RHIS.com.RHIS.report.model.ReportExportFormat;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.IntConsumer;

public interface ReportExportWriter {
    ReportExportFormat format();

    void write(InputStream snapshot, OutputStream output, IntConsumer progress) throws Exception;
}
