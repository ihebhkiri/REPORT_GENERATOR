package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.bot.exception.BotLlmException;
import RHIS.com.RHIS.bot.exception.BotRequestException;
import RHIS.com.RHIS.bot.controller.BotReportController;
import RHIS.com.RHIS.report.exception.ReportCapacityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = BotReportController.class)
public class BotExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotExceptionHandler.class);

    @ExceptionHandler(ReportCapacityException.class)
    public ProblemDetail handleCapacity(ReportCapacityException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Vous avez déjà plusieurs rapports en cours. Attendez qu’un rapport se termine, puis réessayez.");
        problem.setTitle("Rapports en cours");
        return problem;
    }

    @ExceptionHandler(BotRequestException.class)
    public ProblemDetail handleBotRequest(BotRequestException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Requête du bot invalide");
        return problem;
    }

    @ExceptionHandler(BotLlmException.class)
    public ProblemDetail handleBotLlm(BotLlmException exception) {
        LOGGER.error("Le provider IA est indisponible", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "L’assistant est temporairement indisponible. Réessayez.");
        problem.setTitle("Assistant indisponible");
        return problem;
    }
}
