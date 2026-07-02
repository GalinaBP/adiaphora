package ru.adiaphora.platform.questionnaire.domain;

import org.junit.jupiter.api.Test;
import ru.adiaphora.platform.common.error.QuestionnaireValidationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerValidatorTest {

    private final AnswerValidator validator = new AnswerValidator();

    private QuestionDefinition question(QuestionType type, List<QuestionOption> options) {
        return new QuestionDefinition("q", "s", type, "label", null, true, 1, null, options);
    }

    @Test
    void moneyMustBeNonNegativeWithTwoDecimals() {
        QuestionDefinition q = question(QuestionType.MONEY, List.of());
        assertThat(validator.validate(q, "100000")).isEmpty();
        assertThat(validator.validate(q, "-5")).isPresent();
        assertThat(validator.validate(q, "10.999")).isPresent();
        assertThat(validator.normalise(q, "100.50")).isEqualTo("100.5");
    }

    @Test
    void integerMustParse() {
        QuestionDefinition q = question(QuestionType.INTEGER, List.of());
        assertThat(validator.validate(q, "42")).isEmpty();
        assertThat(validator.validate(q, "4.5")).isPresent();
    }

    @Test
    void booleanIsNormalisedToLowercase() {
        QuestionDefinition q = question(QuestionType.BOOLEAN, List.of());
        assertThat(validator.validate(q, "yes")).isPresent();
        assertThat(validator.normalise(q, "TRUE")).isEqualTo("true");
    }

    @Test
    void dateMustBeIso() {
        QuestionDefinition q = question(QuestionType.DATE, List.of());
        assertThat(validator.validate(q, "2026-07-01")).isEmpty();
        assertThat(validator.validate(q, "01/07/2026")).isPresent();
    }

    @Test
    void singleChoiceMustMatchAnOption() {
        QuestionDefinition q = question(QuestionType.SINGLE_CHOICE, List.of(
                new QuestionOption("none", "None", 1), new QuestionOption("sold", "Sold", 2)));
        assertThat(validator.validate(q, "sold")).isEmpty();
        assertThat(validator.validate(q, "unknown")).isPresent();
    }

    @Test
    void multipleChoiceValidatesEachValue() {
        QuestionDefinition q = question(QuestionType.MULTIPLE_CHOICE, List.of(
                new QuestionOption("a", "A", 1), new QuestionOption("b", "B", 2)));
        assertThat(validator.validate(q, "a,b")).isEmpty();
        assertThat(validator.validate(q, "a,x")).isPresent();
        assertThat(validator.normalise(q, " a , b ")).isEqualTo("a,b");
    }

    @Test
    void blankValueIsAcceptedForIncrementalSaves() {
        QuestionDefinition q = question(QuestionType.MONEY, List.of());
        assertThat(validator.validate(q, "")).isEmpty();
        assertThat(validator.validate(q, null)).isEmpty();
        assertThat(validator.normalise(q, "")).isEqualTo("");
    }

    @Test
    void normaliseThrowsOnInvalidValue() {
        QuestionDefinition q = question(QuestionType.INTEGER, List.of());
        assertThatThrownBy(() -> validator.normalise(q, "abc"))
                .isInstanceOf(QuestionnaireValidationException.class);
    }
}
