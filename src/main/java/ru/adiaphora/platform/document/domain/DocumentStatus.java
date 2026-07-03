package ru.adiaphora.platform.document.domain;

/** Lifecycle of a generated document. */
public enum DocumentStatus {
    REQUESTED,
    GENERATING,
    NEEDS_INFORMATION,
    GENERATED,
    UNDER_REVIEW,
    APPROVED,
    READY_FOR_DOWNLOAD,
    FAILED,
    DELETED
}
