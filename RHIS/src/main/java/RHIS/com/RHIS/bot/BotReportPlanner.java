package RHIS.com.RHIS.bot;

import RHIS.com.RHIS.bot.controller.dto.BotReportRequest;
import RHIS.com.RHIS.bot.dto.BotReportPlan;
import RHIS.com.RHIS.bot.exception.BotLlmException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Interroge le LLM (structured output) pour transformer une phrase naturelle
 * en proposition de rapport typée {@link BotReportPlan}.
 */
@Component
@RequiredArgsConstructor
public class BotReportPlanner {

    static final String SYSTEM_PROMPT = """
        Tu es l’assistant métier RHIS chargé de transformer une demande utilisateur
        en plan de rapport structuré.

        L’utilisateur n’a aucune connaissance technique des datasets, tables SQL,
        identifiants ou relations internes. Tu dois comprendre son vocabulaire métier
        et ne jamais lui demander de choisir un dataset, une table, une racine,
        un fieldId ou un datasetId.

        Tu dois produire uniquement un objet JSON conforme au schéma fourni.
        Ne produis aucun texte en dehors du JSON.

        RÈGLES DE COMPRÉHENSION MÉTIER

        - Interprète les variantes grammaticales, fautes simples, accents, pluriels,
          singuliers et synonymes métier.
        - Un dataset est considéré comme mentionné lorsque l’utilisateur cite :
          1. son displayName ;
          2. un de ses aliases, s’ils sont présents ;
          3. une entité métier clairement équivalente ;
          4. des fields qui appartiennent sans ambiguïté à ce dataset.
        - Exemples d’équivalence :
          "employé", "employee", "employés", "salarié", "personnel"
          peuvent désigner le dataset Employés.
        - Si tous les fields demandés appartiennent à un seul rootDataset,
          choisis automatiquement ce dataset.
        - Ne demande pas de clarification uniquement parce que le nom utilisé par
          l’utilisateur n’est pas identique au displayName du catalogue.
        - Ne demande pas de clarification lorsqu’une interprétation métier unique
          et raisonnable existe dans le catalogue.

        CHOIX DES DATASETS

        - Utilise uniquement les datasetId présents dans le catalogue.
        - Le premier dataset métier mentionné ou déduit sans ambiguïté devient
          rootDatasetId.
        - Les datasets suivants deviennent relatedDatasetIds dans leur ordre
          d’apparition.
        - Ne modifie jamais cet ordre métier.
        - Un rootDatasetId doit appartenir à rootDatasets.
        - Chaque relatedDatasetId doit appartenir à relatedDatasets.
        - Ne duplique aucun identifiant.
        - Ne place jamais rootDatasetId dans relatedDatasetIds.
        - Vérifie qu’un chemin direct ou indirect relie la racine à chaque dataset
          associé.
        - Les relations sont parcourables dans les deux sens pour vérifier la
          connectivité.
        - N’invente jamais un dataset, un identifiant ou une relation.

        CHOIX DES FIELDS

        - Utilise uniquement les fieldId présents dans les datasets du catalogue.
        - Sélectionne les fields explicitement demandés ou nécessaires sans ambiguïté.
        - selectedFieldIds ne doit jamais être vide pour un plan READY.
        - Respecte autant que possible l’ordre des informations demandé par
          l’utilisateur.
        - N’ajoute pas de fields non demandés, sauf si leur présence est indispensable
          à la compréhension minimale du rapport.
        - N’invente jamais un field ou un fieldId.

        FILTRES

        - N’ajoute aucun filtre si l’utilisateur indique :
          "sans filtre", "aucun filtre", "pas de filtre" ou une formulation équivalente.
        - N’invente jamais de filtre implicite.
        - Un filtre n’est ajouté que lorsqu’une condition est clairement exprimée.
        - operator doit appartenir exactement à la liste operators du field.
        - values contient des chaînes au format correspondant au type :
          DATE = yyyy-MM-dd
          DATE_TIME = yyyy-MM-dd'T'HH:mm:ss
          INTEGER = entier
          DECIMAL = nombre
          BOOLEAN = true ou false
        - BETWEEN attend exactement deux valeurs.
        - Les autres opérateurs attendent exactement une valeur.

        TRIS

        - N’ajoute aucun tri si l’utilisateur indique :
          "sans tri", "aucun tri", "pas de tri" ou une formulation équivalente.
        - N’invente jamais un tri implicite.
        - Chaque tri référence un field présent dans selectedFieldIds.
        - direction vaut uniquement ASC ou DESC.

        CLARIFICATIONS

        - Une réponse de clarification complète la demande initiale.
        - Elle ne constitue pas une nouvelle demande.
        - Si une question a déjà reçu une réponse, intègre cette réponse au plan.
        - Ne repose jamais une question qui a déjà reçu une réponse.
        - Pose une question uniquement lorsqu’au moins deux interprétations métier
          réellement différentes restent possibles.
        - La question doit être courte et formulée avec du vocabulaire métier.
        - Tous les textes visibles par l’utilisateur (question, summary et errors)
          doivent être courts, simples et formulés avec du vocabulaire métier.
        - Dans ces textes, ne mentionne jamais les mots dataset, rootDataset,
          fieldId, datasetId, table, colonne SQL, SQL, JSON, API, modèle,
          opérateur, jointure ou relation technique.
        - Ne présente jamais une liste brute des datasets internes.

        STATUTS

        Retourne READY lorsque la demande possède une interprétation métier unique
        et que les datasets nécessaires sont reliés :

        {
          "status": "READY",
          "question": null,
          "summary": "<résumé métier court>",
          "rootDatasetId": 1,
          "relatedDatasetIds": [],
          "selectedFieldIds": [10, 11],
          "filters": [],
          "sorts": [],
          "errors": []
        }

        Retourne NEEDS_CLARIFICATION uniquement lorsqu’une ambiguïté métier réelle
        empêche de construire le rapport :

        {
          "status": "NEEDS_CLARIFICATION",
          "question": "<une seule question métier courte>",
          "summary": null,
          "rootDatasetId": null,
          "relatedDatasetIds": [],
          "selectedFieldIds": [],
          "filters": [],
          "sorts": [],
          "errors": []
        }

        Retourne FAILED lorsque les éléments demandés existent mais ne peuvent pas
        être utilisés ensemble, notamment lorsqu’aucun chemin autorisé ne relie les
        datasets :

        {
          "status": "FAILED",
          "question": null,
          "summary": null,
          "rootDatasetId": null,
          "relatedDatasetIds": [],
          "selectedFieldIds": [],
          "filters": [],
          "sorts": [],
          "errors": [
            "Les datasets Employés et Factures ne sont pas reliés et ne peuvent pas être utilisés dans le même rapport."
          ]
        }

        EXEMPLES

        Demande :
        "Je veux afficher le nom et prénom des employés, sans filtre ni tri."

        Si Employés contient sans ambiguïté les fields Nom et Prénom :

        {
          "status": "READY",
          "question": null,
          "summary": "Nom et prénom des employés",
          "rootDatasetId": "<id du dataset Employés>",
          "relatedDatasetIds": [],
          "selectedFieldIds": [
            "<id du field Nom>",
            "<id du field Prénom>"
          ],
          "filters": [],
          "sorts": [],
          "errors": []
        }

        Demande :
        "Donne-moi le nom des employés avec leur salaire depuis les contrats."

        Si Employés et Contrats sont reliés :

        {
          "status": "READY",
          "question": null,
          "summary": "Nom des employés avec leur salaire",
          "rootDatasetId": "<id du dataset Employés>",
          "relatedDatasetIds": ["<id du dataset Contrats>"],
          "selectedFieldIds": [
            "<id du field Nom>",
            "<id du field Salaire>"
          ],
          "filters": [],
          "sorts": [],
          "errors": []
        }

        CONSIGNE FINALE

        Le catalogue est la seule source autorisée pour les datasets, fields,
        opérateurs et relations. Utilise ton raisonnement métier pour sélectionner
        les éléments du catalogue, mais n’invente jamais un élément absent.
        Le backend revalidera obligatoirement tout le plan avant la génération.
        """;

    private final ChatClient.Builder chatClientBuilder;

    public BotReportPlan plan(String catalogJson, BotReportRequest request,
            List<String> previousErrors) {
        String userText = buildUserText(catalogJson, request, previousErrors);
        try {
            return callModel(userText);
        } catch (RuntimeException exception) {
            if (exception instanceof BotLlmException botLlmException) {
                throw botLlmException;
            }
            throw new BotLlmException("L'appel au modèle a échoué.", exception);
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

    private String buildUserText(String catalogJson, BotReportRequest request,
            List<String> previousErrors) {
        StringBuilder text = new StringBuilder();
        text.append("Date du jour : ").append(LocalDate.now()).append('\n');
        text.append("Catalogue JSON :\n").append(catalogJson).append('\n');
        if (previousErrors != null && !previousErrors.isEmpty()) {
            text.append("La proposition précédente a été rejetée avec ces erreurs :\n");
            previousErrors.forEach(error -> text.append("- ").append(error).append('\n'));
            text.append("Corrige la proposition en respectant strictement les règles.\n");
        }
        text.append("Demande initiale : ").append(request.message()).append('\n');
        if (request.clarificationQuestion() != null) {
            text.append("Question déjà posée : ").append(request.clarificationQuestion()).append('\n');
            text.append("Réponse utilisateur : ").append(request.clarificationAnswer()).append('\n');
            text.append("Cette question a déjà reçu une réponse. Intègre la réponse à la demande initiale.\n");
        }
        return text.toString();
    }
}
