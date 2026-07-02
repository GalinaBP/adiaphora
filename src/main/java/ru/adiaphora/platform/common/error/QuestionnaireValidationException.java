package ru.adiaphora.platform.common.error;

import org.springframework.http.HttpStatus;

import java.util.List;

/** Thrown when submitted questionnaire answers fail domain validation. */
public class QuestionnaireValidationException extends BusinessException {

    private final transient List<ApiFieldError> fieldErrors;

    public QuestionnaireValidationException(String message, List<ApiFieldError> fieldErrors) {
        super(ErrorCode.QUESTIONNAIRE_VALIDATION_ERROR, HttpStatus.UNPROCESSABLE_ENTITY, message);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<ApiFieldError> fieldErrors() {
        return fieldErrors;
    }
}
