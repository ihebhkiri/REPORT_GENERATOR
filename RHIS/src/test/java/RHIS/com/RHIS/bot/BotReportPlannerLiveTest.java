package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.bot.controller.dto.BotReportRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in : appelle le fournisseur configuré avec uniquement des métadonnées fictives. */
@EnabledIfSystemProperty(named = "rhis.bot.live-test", matches = "true")
class BotReportPlannerLiveTest {
    @Test
    @Timeout(120)
    void understandsListsExplicitAliasesAndFilterOnlyRelations() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("src/main/resources/application.yaml"));
        var properties = yaml.getObject();
        var options = OpenAiChatOptions.builder().baseUrl(properties.getProperty("spring.ai.openai.base-url"))
                .apiKey(properties.getProperty("spring.ai.openai.api-key"))
                .model(properties.getProperty("spring.ai.openai.chat.options.model")).temperature(0.0).build();
        var model = OpenAiChatModel.builder().options(options).build();
        var planner = new BotReportPlanner(ChatClient.builder(model));
        String catalog = """
                {"rootDatasets":[{"datasetId":1,"displayName":"Employés","description":"Personnel du restaurant",
                 "aliases":["Salariés","Personnel"],"fields":[
                  {"fieldId":10,"displayName":"Nom","description":"Nom de famille","aliases":["Patronyme"],"type":"TEXT","operators":["EQUALS"]},
                  {"fieldId":11,"displayName":"Prénom","description":"Prénom du salarié","aliases":[],"type":"TEXT","operators":["EQUALS"]}]}],
                 "relatedDatasets":[{"datasetId":2,"displayName":"Contrats","description":"Conditions d'emploi","aliases":[],"fields":[
                  {"fieldId":20,"displayName":"Salaire","description":"Salaire mensuel","aliases":[],"type":"DECIMAL","operators":["GREATER_THAN"]}]}],
                 "relations":[{"sourceDatasetId":1,"targetDatasetId":2}]}
                """;
        var all = planner.plan(catalog, new BotReportRequest("Je veux la liste des salariés", null, null, null), null);
        assertThat(all.status()).isEqualTo("READY");
        assertThat(all.allFieldsDatasetIds()).containsExactly(1L);
        var explicit = planner.plan(catalog, new BotReportRequest("Prénom puis patronyme des salariés", null, null, null), null);
        assertThat(explicit.status()).isEqualTo("READY");
        assertThat(explicit.selectedFieldIds()).containsExactly(11L, 10L);
        assertThat(explicit.allFieldsDatasetIds()).isNullOrEmpty();
        var filtered = planner.plan(catalog, new BotReportRequest("Liste des employés dont le salaire dépasse 2000", null, null, null), null);
        assertThat(filtered.status()).isEqualTo("READY");
        assertThat(filtered.rootDatasetId()).isEqualTo(1L);
        assertThat(filtered.allFieldsDatasetIds()).isNotNull().isSubsetOf(1L).doesNotHaveDuplicates();
        var displayed = new ArrayList<>(filtered.selectedFieldIds());
        // Même expansion que le serveur : seuls les champs de l'entité demandée sont ajoutés.
        if (filtered.allFieldsDatasetIds().contains(1L)) {
            for (Long fieldId : List.of(10L, 11L)) {
                if (!displayed.contains(fieldId)) displayed.add(fieldId);
            }
        }
        assertThat(displayed).as("Champs du rapport filtré : %s", filtered).containsExactly(10L, 11L);
        assertThat(filtered.relatedDatasetIds()).containsExactly(2L);
        assertThat(filtered.filters()).singleElement().satisfies(filter -> {
            assertThat(filter.fieldId()).isEqualTo(20L);
            assertThat(filter.operator()).isEqualTo("GREATER_THAN");
            assertThat(filter.values()).isEqualTo(List.of("2000"));
        });
    }
}



