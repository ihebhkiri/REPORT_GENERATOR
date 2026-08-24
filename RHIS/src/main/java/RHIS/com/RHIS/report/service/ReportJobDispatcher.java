package RHIS.com.RHIS.report.service;

import java.util.UUID;

public interface ReportJobDispatcher {
    void dispatchGeneration(UUID generationId);

    void dispatchExport(UUID exportId);

    void cancelGeneration(UUID generationId);

    void cancelExport(UUID exportId);
}
