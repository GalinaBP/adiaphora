package ru.adiaphora.platform.common.security;

/**
 * Spring Security authority names, shared so modules can perform role checks without depending on the
 * {@code auth} module's internal {@code UserRole} enum. These are the {@code ROLE_*} strings that
 * appear as granted authorities on the authenticated principal.
 */
public final class Authorities {

    public static final String USER = "ROLE_USER";
    public static final String OPERATOR = "ROLE_OPERATOR";
    public static final String LAWYER = "ROLE_LAWYER";
    public static final String ADMIN = "ROLE_ADMIN";
    public static final String AUDITOR = "ROLE_AUDITOR";

    private Authorities() {
    }
}
