package ru.adiaphora.platform.rules.domain;

import ru.adiaphora.platform.questionnaire.api.QuestionnaireSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read-only, typed view over a {@link QuestionnaireSnapshot} that rules use to inspect answers. All
 * accessors return {@link Optional} so a rule can distinguish "unanswered" from a concrete value.
 */
public final class RuleContext {

    private final QuestionnaireSnapshot snapshot;

    public RuleContext(QuestionnaireSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public boolean isAnswered(String questionCode) {
        return snapshot.answer(questionCode).filter(value -> !value.isBlank()).isPresent();
    }

    public Optional<String> text(String questionCode) {
        return snapshot.answer(questionCode).filter(value -> !value.isBlank());
    }

    public Optional<BigDecimal> money(String questionCode) {
        return text(questionCode).flatMap(value -> {
            try {
                return Optional.of(new BigDecimal(value));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        });
    }

    public Optional<Boolean> bool(String questionCode) {
        return text(questionCode).map(value -> value.equalsIgnoreCase("true"));
    }

    /** ISO-8601 date answer (yyyy-MM-dd); empty when unanswered or unparseable. */
    public Optional<LocalDate> date(String questionCode) {
        return text(questionCode).flatMap(value -> {
            try {
                return Optional.of(LocalDate.parse(value.trim()));
            } catch (DateTimeParseException ex) {
                return Optional.empty();
            }
        });
    }

    /** Multi-choice answer stored as comma-separated values; empty set when unanswered. */
    public Set<String> multi(String questionCode) {
        return text(questionCode)
                .<Set<String>>map(value -> Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .orElseGet(Set::of);
    }
}
