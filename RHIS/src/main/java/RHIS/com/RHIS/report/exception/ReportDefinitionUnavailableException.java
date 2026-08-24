package RHIS.com.RHIS.report.exception;

import java.util.List;

public class ReportDefinitionUnavailableException extends RuntimeException {

    private final List<UnavailableReportElement> unavailableElements;

    public ReportDefinitionUnavailableException(List<UnavailableReportElement> unavailableElements) {
        super("La définition référence des tables ou des champs indisponibles.");
        this.unavailableElements = List.copyOf(unavailableElements);
    }

    public List<UnavailableReportElement> getUnavailableElements() {
        return unavailableElements;
    }
}
