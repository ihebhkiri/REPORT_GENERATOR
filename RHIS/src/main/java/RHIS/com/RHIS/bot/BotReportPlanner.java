package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.bot.config.BotAiProperties;
import RHIS.com.RHIS.bot.dto.BotReportPlan;
import RHIS.com.RHIS.bot.exception.BotLlmException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Interroge le LLM (structured output) pour transformer une phrase naturelle
 * en proposition de rapport typée {@link BotReportPlan}.
 */
@Component
@RequiredArgsConstructor
public class BotReportPlanner {

    static final String SYSTEM_PROMPT = """
            Tu es l'assistant RHIS. À partir du catalogue JSON des datasets et de la demande
            de l'utilisateur, produis uniquement un objet JSON conforme au schéma fourni.
            Règles :
            - n'utilise que les datasetId et fieldId présents dans le catalogue ;
            - "operator" appartient exactement à la liste "operators" du champ choisi ;
            - "values" contient des chaînes au format du type du champ (DATE = yyyy-MM-dd,
              DATE_TIME = yyyy-MM-dd'T'HH:mm:ss, INTEGER = entier, DECIMAL = nombre,
              BOOLEAN = true/false) ;
            - BETWEEN attend exactement 2 valeurs, les autres opérateurs exactement 1 ;
            - "selectedFieldIds" ne doit jamais être vide ;
            - "sorts" référence uniquement des champs de "selectedFieldIds",
              "direction" vaut ASC ou DESC ;
            - si la demande est ambiguë, incomplète ou impossible avec ce catalogue :
              {"status":"NEEDS_CLARIFICATION","question":"<une seule question courte>"} ;
            - sinon {"status":"READY","summary":"<une phrase>", ...la définition...}.
            """;

    private final ChatClient.Builder chatClientBuilder;
    private final BotAiProperties properties;

    public BotReportPlan plan(String catalogJson, String message, List<String> previousErrors) {
        String userText = buildUserText(catalogJson, message, previousErrors);
        try {
            return CompletableFuture.supplyAsync(() -> callModel(userText))
                    .get(properties.getLlmTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            throw new BotLlmException("Le modèle n'a pas répondu dans le délai imparti.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BotLlmException("L'appel au modèle a été interrompu.");
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof BotLlmException botLlmException) {
                throw botLlmException;
            }
            throw new BotLlmException("L'appel au modèle a échoué.", exception.getCause());
        }
    }

    private BotReportPlan callModel(String userText) {
        ChatClient chatClient = chatClientBuilder.build();
        BotReportPlan plan = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userText)
                .call()
                .entity(BotReportPlan.class);
        if (plan == null) {
            throw new BotLlmException("La réponse du modèle est illisible.");
        }
        return plan;
    }

    private String buildUserText(String catalogJson, String message, List<String> previousErrors) {
        StringBuilder text = new StringBuilder();
        text.append("Date du jour : ").append(LocalDate.now()).append('\n');
        text.append("Catalogue JSON :\n").append(catalogJson).append('\n');
        if (previousErrors != null && !previousErrors.isEmpty()) {
            text.append("La proposition précédente a été rejetée avec ces erreurs :\n");
            previousErrors.forEach(error -> text.append("- ").append(error).append('\n'));
            text.append("Corrige la proposition en respectant strictement les règles.\n");
        }
        text.append("Demande de l'utilisateur : ").append(message);
        return text.toString();
    }
}
