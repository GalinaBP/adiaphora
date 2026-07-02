package ru.adiaphora.platform.rules.domain;

import ru.adiaphora.platform.questionnaire.api.QuestionnaireSnapshot;

import java.math.BigDecimal;
import java.util.Optional;

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
}
