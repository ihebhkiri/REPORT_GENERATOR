package RHIS.com.RHIS.report.exception;

public record UnavailableReportElement(
        String kind,
        Long id,
        String displayName,
        String reason
) {
}
