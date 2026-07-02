package ru.adiaphora.platform.auth.domain;

/** Account activation state. Only {@link #ACTIVE} users may authenticate. */
public enum UserStatus {
    PENDING_ACTIVATION,
    ACTIVE,
    DISABLED
}
