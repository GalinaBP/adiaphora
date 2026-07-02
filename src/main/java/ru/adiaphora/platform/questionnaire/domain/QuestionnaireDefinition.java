package ru.adiaphora.platform.questionnaire.domain;

import ru.adiaphora.platform.common.error.ResourceNotFoundException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A whole questionnaire version: its sections and question definitions. Read-mostly — assembled from
 * storage/seed and used to render the questionnaire and to validate/score answers.
 */
public record QuestionnaireDefinition(
        UUID versionId,
        String versionCode,
        String label,
        List<QuestionSection> sections,
        List<QuestionDefinition> questions
) {

    public QuestionnaireDefinition {
        sections = sections == null ? List.of() : List.copyOf(sections);
        questions = questions == null ? List.of() : List.copyOf(questions);
    }

    public Optional<QuestionDefinition> findQuestion(String code) {
        return questions.stream().filter(question -> question.code().equals(code)).findFirst();
    }

    public QuestionDefinition requireQuestion(String code) {
        return findQuestion(code)
                .orElseThrow(() -> ResourceNotFoundException.of("Question", code));
    }

    public List<QuestionDefinition> orderedQuestions() {
        return questions.stream()
                .sorted(Comparator.comparingInt(QuestionDefinition::displayOrder))
                .toList();
    }

    public List<String> requiredQuestionCodes() {
        return questions.stream()
                .filter(QuestionDefinition::required)
                .map(QuestionDefinition::code)
                .toList();
    }
}
