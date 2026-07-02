package ru.adiaphora.platform.common.error;

import org.springframework.http.HttpStatus;

/** Thrown when the rule engine cannot complete an evaluation for technical reasons. */
public class RulesEvaluationException extends BusinessException {

    public RulesEvaluationException(String message) {
        super(ErrorCode.RULES_EVALUATION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
