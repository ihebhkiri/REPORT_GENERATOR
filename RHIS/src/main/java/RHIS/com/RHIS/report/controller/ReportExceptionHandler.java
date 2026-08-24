package RHIS.com.RHIS.report.controller;

import RHIS.com.RHIS.report.exception.ReportExecutionException;
import RHIS.com.RHIS.report.exception.ReportCapacityException;
import RHIS.com.RHIS.report.exception.ReportConflictException;
import RHIS.com.RHIS.report.exception.ReportQueryTimeoutException;
import RHIS.com.RHIS.report.exception.ReportResourceNotFoundException;
import RHIS.com.RHIS.report.exception.ReportValidationException;
import RHIS.com.RHIS.report.exception.ReportDefinitionUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        ReportController.class,
        ReportGenerationController.class,
        ReportExportController.class
})
public class ReportExceptionHandler {

    @ExceptionHandler(ReportDefinitionUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleUnavailable(ReportDefinitionUnavailableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Définition de rapport indisponible");
        problem.setProperty("unavailableElements", exception.getUnavailableElements());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(ReportValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(ReportValidationException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Définition de rapport invalide", exception.getMessage());
    }

    @ExceptionHandler(ReportResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ReportResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Ressource de rapport introuvable", exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ProblemDetail> handleInvalidRequest(Exception exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Requête de preview invalide",
                "Le corps de la requête ne respecte pas le contrat attendu."
        );
    }

    @ExceptionHandler(ReportQueryTimeoutException.class)
    public ResponseEntity<ProblemDetail> handleTimeout(ReportQueryTimeoutException exception) {
        return problem(HttpStatus.GATEWAY_TIMEOUT, "Preview expirée", exception.getMessage());
    }

    @ExceptionHandler(ReportExecutionException.class)
    public ResponseEntity<ProblemDetail> handleExecution(ReportExecutionException exception) {
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Échec de la preview",
                exception.getMessage()
        );
    }

    @ExceptionHandler(ReportConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ReportConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Opération de rapport impossible", exception.getMessage());
    }

    @ExceptionHandler(ReportCapacityException.class)
    public ResponseEntity<ProblemDetail> handleCapacity(ReportCapacityException exception) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "Capacité de génération atteinte", exception.getMessage());
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }
}
