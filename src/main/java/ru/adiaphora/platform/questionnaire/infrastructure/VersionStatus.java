package ru.adiaphora.platform.questionnaire.infrastructure;

/** Lifecycle of a questionnaire version. Exactly one version should be {@code ACTIVE} at a time. */
enum VersionStatus {
    DRAFT,
    ACTIVE,
    RETIRED
}
