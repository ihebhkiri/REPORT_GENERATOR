package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.bot.exception.BotLlmException;
import RHIS.com.RHIS.bot.exception.BotRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BotExceptionHandler {

    @ExceptionHandler(BotRequestException.class)
    public ProblemDetail handleBotRequest(BotRequestException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Requête du bot invalide");
        return problem;
    }

    @ExceptionHandler(BotLlmException.class)
    public ProblemDetail handleBotLlm(BotLlmException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, exception.getMessage());
        problem.setTitle("Le modèle de langage est indisponible");
        return problem;
    }
}