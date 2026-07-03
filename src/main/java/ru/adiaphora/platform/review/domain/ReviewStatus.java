package ru.adiaphora.platform.review.domain;

/** Lifecycle of a manual-review task. */
public enum ReviewStatus {
    OPEN,
    ASSIGNED,
    WAITING_FOR_INFORMATION,
    IN_PROGRESS,
    APPROVED,
    REJECTED,
    CLOSED
}
