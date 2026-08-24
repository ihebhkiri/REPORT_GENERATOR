package RHIS.com.RHIS.dataset.controller;

import RHIS.com.RHIS.dataset.exception.DataSetConfigurationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = DataSetAdministrationController.class)
public class DataSetAdministrationExceptionHandler {

    @ExceptionHandler(DataSetConfigurationException.class)
    public ResponseEntity<ProblemDetail> handleConfiguration(DataSetConfigurationException exception) {
        return problem(exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ProblemDetail> handleInvalidRequest(Exception exception) {
        return problem("Le corps de la requête ne respecte pas le contrat attendu.");
    }

    private ResponseEntity<ProblemDetail> problem(String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Configuration des datasets invalide");
        return ResponseEntity.badRequest().body(problem);
    }
}
