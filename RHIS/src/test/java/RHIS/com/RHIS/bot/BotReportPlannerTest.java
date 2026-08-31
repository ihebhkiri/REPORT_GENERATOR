package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.bot.config.BotAiProperties;
import RHIS.com.RHIS.bot.controller.dto.BotReportRequest;
import RHIS.com.RHIS.bot.dto.BotReportPlan;
import RHIS.com.RHIS.bot.exception.BotLlmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotReportPlannerTest {

    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec callSpec;

    private BotReportPlanner planner;
    private BotAiProperties properties;

    @BeforeEach
    void setUp() {
        properties = new BotAiProperties();
        properties.setLlmTimeoutSeconds(1);
        planner = new BotReportPlanner(chatClientBuilder, properties);
    }

    private void stubChain() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }

    private BotReportPlan readyPlan() {
        return new BotReportPlan("READY", null, "Rapport", 1L, List.of(),
                List.of(10L), List.of(), List.of(), List.of());
    }

    @Test
    void returnsPlanFromModel() {
        stubChain();
        BotReportPlan plan = readyPlan();
        when(callSpec.entity(BotReportPlan.class)).thenReturn(plan);

        BotReportPlan result = planner.plan("[]",
                new BotReportRequest("rapport employés", null, null, null), null);

        assertSame(plan, result);
    }

    @Test
    void includesPreviousErrorsInUserText() {
        stubChain();
        when(callSpec.entity(BotReportPlan.class)).thenReturn(readyPlan());

        planner.plan("[]", new BotReportRequest("rapport", null, null, null),
                List.of("L'opérateur BETWEEN attend 2 valeur(s)."));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(captor.capture());
        assertTrue(captor.getValue().contains("L'opérateur BETWEEN attend 2 valeur(s)."));
    }

    @Test
    void promptDefinesMultiDatasetRules() {
        assertTrue(BotReportPlanner.SYSTEM_PROMPT.contains("d’apparition"));
        assertTrue(BotReportPlanner.SYSTEM_PROMPT.contains("relatedDatasetIds"));
        assertTrue(BotReportPlanner.SYSTEM_PROMPT.contains("dans les deux sens"));
    }

    @Test
    void includesStructuredClarificationInUserText() {
        stubChain();
        when(callSpec.entity(BotReportPlan.class)).thenReturn(readyPlan());

        planner.plan("[]", new BotReportRequest("Liste des contrats", null,
                "Contrats actifs ou tous ?", "Tous les contrats"), null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(captor.capture());
        assertTrue(captor.getValue().contains("Demande initiale : Liste des contrats"));
        assertTrue(captor.getValue().contains("Question déjà posée : Contrats actifs ou tous ?"));
        assertTrue(captor.getValue().contains("Réponse utilisateur : Tous les contrats"));
    }

    @Test
    void wrapsModelFailureInBotLlmException() {
        stubChain();
        when(callSpec.entity(BotReportPlan.class)).thenThrow(new IllegalStateException("json"));

        assertThrows(BotLlmException.class, () -> planner.plan("[]",
                new BotReportRequest("x", null, null, null), null));
    }

    @Test
    void timesOutWhenModelIsTooSlow() {
        stubChain();
        when(callSpec.entity(BotReportPlan.class)).thenAnswer(invocation -> {
            Thread.sleep(3000);
            return readyPlan();
        });

        assertThrows(BotLlmException.class, () -> planner.plan("[]",
                new BotReportRequest("x", null, null, null), null));
    }
}
