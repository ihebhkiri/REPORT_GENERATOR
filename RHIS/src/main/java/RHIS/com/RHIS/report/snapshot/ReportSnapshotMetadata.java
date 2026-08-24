package RHIS.com.RHIS.report.snapshot;

import RHIS.com.RHIS.dataset.model.DataSetFieldType;

import java.util.List;

public record ReportSnapshotMetadata(List<Column> columns, long rowCount) {
    public ReportSnapshotMetadata {
        columns = List.copyOf(columns);
    }

    public record Column(String key, String displayName, DataSetFieldType dataType) {
    }
}
