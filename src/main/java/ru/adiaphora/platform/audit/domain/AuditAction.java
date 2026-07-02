package ru.adiaphora.platform.audit.domain;

/** The catalogue of security- and user-sensitive actions recorded in the audit log. */
public enum AuditAction {
    USER_REGISTERED,
    USER_LOGIN_SUCCEEDED,
    USER_LOGIN_FAILED,
    APPLICATION_CREATED,
    APPLICATION_VIEWED,
    APPLICATION_SUBMITTED,
    APPLICATION_CANCELLED,
    APPLICATION_STATUS_CHANGED,
    ANSWER_UPDATED,
    RULES_EVALUATED,
    REVIEW_ASSIGNED,
    REVIEW_DECISION_RECORDED,
    DOCUMENT_REQUESTED,
    DOCUMENT_DOWNLOADED,
    ACCESS_DENIED
}
