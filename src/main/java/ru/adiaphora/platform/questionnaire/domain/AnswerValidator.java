package ru.adiaphora.platform.questionnaire.domain;

import ru.adiaphora.platform.common.error.ApiFieldError;
import ru.adiaphora.platform.common.error.QuestionnaireValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Validates and normalises a raw answer against its {@link QuestionDefinition}. Format checks are
 * applied to non-blank values; requiredness is enforced separately when validating the whole
 * questionnaire (so partial, incremental saves are allowed).
 *
 * <p><strong>Placeholder:</strong> only basic type/format rules are implemented. Product- and
 * legal-specific bounds (from {@code validationConfiguration}) must be defined and lawyer-approved.
 *
 * <p>A pure domain service (no framework dependencies); it is registered as a Spring bean by
 * {@code QuestionnaireBeans} in the infrastructure layer.
 */
public class AnswerValidator {

    /** Returns a validation error message for the value, or empty if it is acceptable. */
    public Optional<String> validate(QuestionDefinition question, String rawValue) {
        if (isBlank(rawValue)) {
            return Optional.empty();
        }
        String value = rawValue.trim();
        return switch (question.type()) {
            case TEXT, TEXTAREA -> Optional.empty();
            case INTEGER -> parsesAsLong(value) ? Optional.empty()
                    : Optional.of("должно быть целым числом");
            case MONEY -> validMoney(value) ? Optional.empty()
                    : Optional.of("должно быть неотрицательной суммой (до 2 знаков после запятой)");
            case BOOLEAN -> ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))
                    ? Optional.empty() : Optional.of("должно быть значением «да» или «нет»");
            case DATE -> parsesAsDate(value) ? Optional.empty()
                    : Optional.of("должна быть дата в формате ГГГГ-ММ-ДД");
            case SINGLE_CHOICE -> question.option(value).isPresent() ? Optional.empty()
                    : Optional.of("должно быть одним из допустимых вариантов");
            case MULTIPLE_CHOICE -> allOptionsValid(question, value) ? Optional.empty()
                    : Optional.of("должно быть списком допустимых вариантов через запятую");
        };
    }

    /** Validates then returns the normalised value to persist. Throws if the value is invalid. */
    public String normalise(QuestionDefinition question, String rawValue) {
        Optional<String> error = validate(question, rawValue);
        if (error.isPresent()) {
            throw new QuestionnaireValidationException(
                    "Invalid answer for question " + question.code(),
                    List.of(new ApiFieldError(question.code(), error.get())));
        }
        if (isBlank(rawValue)) {
            return "";
        }
        String value = rawValue.trim();
        return switch (question.type()) {
            case BOOLEAN -> value.toLowerCase();
            case MONEY -> new BigDecimal(value).stripTrailingZeros().toPlainString();
            case MULTIPLE_CHOICE -> String.join(",", splitChoices(value));
            default -> value;
        };
    }

    private boolean allOptionsValid(QuestionDefinition question, String value) {
        List<String> selected = splitChoices(value);
        return !selected.isEmpty() && selected.stream().allMatch(v -> question.option(v).isPresent());
    }

    private List<String> splitChoices(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private boolean parsesAsLong(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean validMoney(String value) {
        try {
            BigDecimal amount = new BigDecimal(value);
            return amount.signum() >= 0 && amount.scale() <= 2;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean parsesAsDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
