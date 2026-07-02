package ru.adiaphora.platform.auth.domain;

/** Coarse authorization role assigned to a user. Maps to a Spring Security {@code ROLE_*} authority. */
public enum UserRole {
    USER,
    OPERATOR,
    LAWYER,
    ADMIN,
    AUDITOR;

    /** The Spring Security authority name, e.g. {@code ROLE_LAWYER}. */
    public String authority() {
        return "ROLE_" + name();
    }
}
