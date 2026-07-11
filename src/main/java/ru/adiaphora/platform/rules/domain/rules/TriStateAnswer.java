package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleInputs;

import java.util.Optional;

/** The yes / no / not-sure answers of the stage-3.2 follow-up questions. */
enum TriStateAnswer {
    YES,
    NO,
    NOT_SURE;

    /** Empty when unanswered or not one of the recognised values. */
    static Optional<TriStateAnswer> read(RuleContext context, String questionCode) {
        return context.text(questionCode).flatMap(value -> switch (value) {
            case RuleInputs.VALUE_YES -> Optional.of(YES);
            case RuleInputs.VALUE_NO -> Optional.of(NO);
            case RuleInputs.VALUE_NOT_SURE -> Optional.of(NOT_SURE);
            default -> Optional.empty();
        });
    }
}
