package ru.adiaphora.platform.application.api;

/**
 * Lifecycle status of a bankruptcy case ("application" = the customer's case, not the Spring app).
 * Part of the public contract, so it lives in {@code api}.
 */
public enum BankruptcyApplicationStatus {
    DRAFT,
    QUESTIONNAIRE_IN_PROGRESS,
    READY_FOR_EVALUATION,
    NEEDS_INFORMATION,
    MANUAL_REVIEW_REQUIRED,
    UNDER_REVIEW,
    APPROVED_FOR_DOCUMENT_GENERATION,
    DOCUMENTS_READY,
    COMPLETED,
    CANCELLED
}
