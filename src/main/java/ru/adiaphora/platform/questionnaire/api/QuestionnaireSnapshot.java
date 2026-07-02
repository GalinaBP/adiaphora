package ru.adiaphora.platform.questionnaire.api;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable point-in-time view of an application's answers, passed to the rules engine for
 * deterministic evaluation. Answers are normalised strings keyed by question code.
 */
public record QuestionnaireSnapshot(UUID applicationId, String versionCode, Map<String, String> answers) {

    public QuestionnaireSnapshot {
        answers = answers == null ? Map.of() : Map.copyOf(answers);
    }

    public Optional<String> answer(String questionCode) {
        return Optional.ofNullable(answers.get(questionCode));
    }
}
