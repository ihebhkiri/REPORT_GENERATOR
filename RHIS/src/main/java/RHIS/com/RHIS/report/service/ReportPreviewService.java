package RHIS.com.RHIS.report.service;

import RHIS.com.RHIS.report.controller.dto.ReportPreviewRequest;
import RHIS.com.RHIS.report.controller.dto.ReportPreviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Orchestre la résolution, la construction SQL limitée et l'exécution de la preview. */
@Service
@RequiredArgsConstructor
public class ReportPreviewService {

    private final ReportDefinitionResolver definitionResolver;
    private final ReportSqlBuilder reportSqlBuilder;
    private final ReportPreviewExecutor reportPreviewExecutor;


    public ReportPreviewResponse preview(ReportPreviewRequest request) {
        ResolvedReportDefinition definition = definitionResolver.resolve(request);
        return reportPreviewExecutor.execute(reportSqlBuilder.buildPreview(definition));
    }
}
