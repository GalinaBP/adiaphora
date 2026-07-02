package ru.adiaphora.platform.questionnaire.application;

import ru.adiaphora.platform.common.error.ApiFieldError;

import java.util.List;

/**
 * Outcome of validating a whole questionnaire: whether all required questions are answered, which are
 * still missing, and any per-field format errors among the answers already provided.
 */
public record ValidationResult(
        boolean complete,
        List<String> missingRequired,
        List<ApiFieldError> fieldErrors
) {
}
