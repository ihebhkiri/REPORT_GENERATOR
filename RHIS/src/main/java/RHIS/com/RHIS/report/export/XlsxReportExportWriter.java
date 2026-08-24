package RHIS.com.RHIS.report.export;

import RHIS.com.RHIS.dataset.model.DataSetFieldType;
import RHIS.com.RHIS.report.config.ReportJobProperties;
import RHIS.com.RHIS.report.model.ReportExportFormat;
import RHIS.com.RHIS.report.snapshot.ReportSnapshotMetadata;
import RHIS.com.RHIS.report.snapshot.ReportSnapshotReader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

@Component
public class XlsxReportExportWriter implements ReportExportWriter {

    private final ReportSnapshotReader snapshotReader;
    private final ReportJobProperties properties;

    public XlsxReportExportWriter(ReportSnapshotReader snapshotReader, ReportJobProperties properties) {
        this.snapshotReader = snapshotReader;
        this.properties = properties;
    }

    @Override
    public ReportExportFormat format() {
        return ReportExportFormat.XLSX;
    }

    @Override
    public void write(InputStream snapshot, OutputStream output, IntConsumer progress) throws Exception {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(properties.getXlsxRowWindow())) {
            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet("Rapport");
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle dateStyle = dateStyle(workbook, "yyyy-mm-dd");
            CellStyle dateTimeStyle = dateStyle(workbook, "yyyy-mm-dd hh:mm:ss");
            snapshotReader.read(snapshot, new ReportSnapshotReader.SnapshotConsumer() {
                private ReportSnapshotMetadata metadata;
                private long rowIndex;

                @Override
                public void start(ReportSnapshotMetadata value) {
                    metadata = value;
                    Row header = sheet.createRow(0);
                    for (int index = 0; index < metadata.columns().size(); index++) {
                        Cell cell = header.createCell(index, CellType.STRING);
                        cell.setCellValue(metadata.columns().get(index).displayName());
                        cell.setCellStyle(headerStyle);
                        int width = Math.min(40, Math.max(12, metadata.columns().get(index).displayName().length() + 2));
                        sheet.setColumnWidth(index, width * 256);
                    }
                    sheet.createFreezePane(0, 1);
                }

                @Override
                public void row(Map<String, Object> values) {
                    long current = ++rowIndex;
                    Row row = sheet.createRow(Math.toIntExact(current));
                    for (int index = 0; index < metadata.columns().size(); index++) {
                        ReportSnapshotMetadata.Column column = metadata.columns().get(index);
                        writeCell(row.createCell(index), values.get(column.key()), column.dataType(), dateStyle, dateTimeStyle);
                    }
                    progress.accept(exportProgress(current, metadata.rowCount()));
                }
            });
            workbook.write(output);
            progress.accept(99);
        }
    }

    private void writeCell(
            Cell cell,
            Object value,
            DataSetFieldType type,
            CellStyle dateStyle,
            CellStyle dateTimeStyle
    ) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        switch (type) {
            case INTEGER, DECIMAL -> cell.setCellValue(((Number) value).doubleValue());
            case BOOLEAN -> cell.setCellValue((Boolean) value);
            case DATE -> {
                cell.setCellValue(toLocalDate(value));
                cell.setCellStyle(dateStyle);
            }
            case DATE_TIME -> {
                cell.setCellValue(toLocalDateTime(value));
                cell.setCellStyle(dateTimeStyle);
            }
            case OFFSET_DATE_TIME -> {
                cell.setCellValue(toUtcLocalDateTime(value));
                cell.setCellStyle(dateTimeStyle);
            }
            case TEXT, TIME, UUID, UNSUPPORTED -> cell.setCellValue(value.toString());
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof List<?> components) {
            requireComponents(components, 3, "date");
            return LocalDate.of(
                    component(components, 0),
                    component(components, 1),
                    component(components, 2)
            );
        }
        return LocalDate.parse(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof List<?> components) {
            requireComponents(components, 5, "date-time");
            return LocalDateTime.of(
                    component(components, 0),
                    component(components, 1),
                    component(components, 2),
                    component(components, 3),
                    component(components, 4),
                    optionalComponent(components, 5),
                    optionalComponent(components, 6)
            );
        }
        return LocalDateTime.parse(value.toString());
    }

    private LocalDateTime toUtcLocalDateTime(Object value) {
        if (value instanceof Number epochValue) {
            BigDecimal epochSeconds = new BigDecimal(epochValue.toString());
            long seconds = epochSeconds.setScale(0, RoundingMode.FLOOR).longValueExact();
            int nanos = epochSeconds.subtract(BigDecimal.valueOf(seconds))
                    .movePointRight(9)
                    .intValueExact();
            return Instant.ofEpochSecond(seconds, nanos)
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDateTime();
        }
        if (value instanceof List<?> components) {
            LocalDateTime localDateTime = toLocalDateTime(components);
            ZoneOffset offset = components.size() > 7
                    ? ZoneOffset.ofTotalSeconds(component(components, 7))
                    : ZoneOffset.UTC;
            return localDateTime.atOffset(offset)
                    .withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
        }
        return OffsetDateTime.parse(value.toString())
                .withOffsetSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    private void requireComponents(List<?> components, int minimumSize, String type) {
        if (components.size() < minimumSize) {
            throw new IllegalArgumentException("Invalid legacy " + type + " value: " + components);
        }
    }

    private int optionalComponent(List<?> components, int index) {
        return components.size() > index ? component(components, index) : 0;
    }

    private int component(List<?> components, int index) {
        Object value = components.get(index);
        if (value instanceof Number number) {
            return new BigDecimal(number.toString()).intValueExact();
        }
        return Integer.parseInt(value.toString());
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle dateStyle(Workbook workbook, String pattern) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat(pattern));
        return style;
    }

    private int exportProgress(long current, long total) {
        return total <= 0 ? 95 : 5 + (int) Math.min(90, (current * 90) / total);
    }
}
