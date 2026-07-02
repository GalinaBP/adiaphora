package ru.adiaphora.platform.questionnaire.domain;

import java.util.List;
import java.util.Optional;

/**
 * The definition of a single question within a questionnaire version. {@code validationConfiguration}
 * is an opaque JSON string interpreted by {@link AnswerValidator} (e.g. min/max bounds); it is kept
 * as text so validation rules can evolve without a schema change.
 */
public record QuestionDefinition(
        String code,
        String sectionCode,
        QuestionType type,
        String label,
        String helpText,
        boolean required,
        int displayOrder,
        String validationConfiguration,
        List<QuestionOption> options
) {

    public QuestionDefinition {
        options = options == null ? List.of() : List.copyOf(options);
    }

    public boolean isChoice() {
        return type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE;
    }

    public Optional<QuestionOption> option(String value) {
        return options.stream().filter(option -> option.value().equals(value)).findFirst();
    }
}
