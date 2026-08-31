package RHIS.com.RHIS.bot.exception;

/**
 * Signalée lorsque l'appel au LLM échoue (indisponibilité, timeout, réponse illisible).
 */
public class BotLlmException extends RuntimeException {
    public BotLlmException(String message) {
        super(message);
    }

    public BotLlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
