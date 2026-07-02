package ru.adiaphora.platform.questionnaire.application;

import ru.adiaphora.platform.questionnaire.domain.QuestionDefinition;
import ru.adiaphora.platform.questionnaire.domain.QuestionSection;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** An application's questionnaire: the active definition plus its saved answers and completion state. */
public record QuestionnaireForm(
        UUID applicationId,
        String versionCode,
        String label,
        List<QuestionSection> sections,
        List<QuestionDefinition> questions,
        Map<String, String> answers,
        CompletionSummary completion
) {
}
