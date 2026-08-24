package RHIS.com.RHIS.report.exception;

public class ReportQueryTimeoutException extends RuntimeException {
    public ReportQueryTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
