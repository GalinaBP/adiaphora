package ru.adiaphora.platform.questionnaire.application;

import ru.adiaphora.platform.questionnaire.domain.QuestionnaireDefinition;

import java.util.List;
import java.util.Map;

/** How complete a response is, based on which required questions have a non-blank answer. */
public record CompletionSummary(
        int requiredTotal,
        int requiredAnswered,
        List<String> missingRequired,
        boolean complete
) {

    static CompletionSummary of(QuestionnaireDefinition definition, Map<String, String> answers) {
        List<String> required = definition.requiredQuestionCodes();
        List<String> missing = required.stream()
                .filter(code -> isBlank(answers.get(code)))
                .toList();
        int answered = required.size() - missing.size();
        return new CompletionSummary(required.size(), answered, missing, missing.isEmpty());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
