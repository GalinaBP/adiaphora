package ru.adiaphora.platform.rules.api;

/** The outcome of evaluating a single rule against a questionnaire snapshot. */
public enum RuleOutcome {
    PASSED,
    FAILED,
    NOT_APPLICABLE,
    NEEDS_INFORMATION
}
