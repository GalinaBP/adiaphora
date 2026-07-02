package ru.adiaphora.platform.auth.domain;

import java.util.UUID;

/**
 * User aggregate — a plain domain object with behaviour and no persistence/framework dependencies.
 * Persistence is handled by an infrastructure JPA entity + adapter that reconstitute this type.
 *
 * <p>Passwords are only ever held as an already-computed hash; hashing itself is an application/infra
 * concern. {@code tokenVersion} is bumped on logout so previously issued refresh tokens are rejected.
 */
public class User {

    private final UUID id;
    private final String email;
    private String passwordHash;
    private UserStatus status;
    private UserRole role;
    private long tokenVersion;

    private User(UUID id, String email, String passwordHash, UserStatus status, UserRole role,
                 long tokenVersion) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.role = role;
        this.tokenVersion = tokenVersion;
    }

    /** Registers a new, active user. The caller supplies an already-hashed password. */
    public static User register(UUID id, String email, String passwordHash, UserRole role) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash is required");
        }
        return new User(id, email.toLowerCase(), passwordHash, UserStatus.ACTIVE, role, 0L);
    }

    /** Reconstitutes an existing user from storage. */
    public static User rehydrate(UUID id, String email, String passwordHash, UserStatus status,
                                 UserRole role, long tokenVersion) {
        return new User(id, email, passwordHash, status, role, tokenVersion);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public void disable() {
        this.status = UserStatus.DISABLED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void changePasswordHash(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash is required");
        }
        this.passwordHash = newPasswordHash;
    }

    /** Invalidates all currently issued refresh tokens (used on logout). */
    public void revokeRefreshTokens() {
        this.tokenVersion++;
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public UserStatus status() {
        return status;
    }

    public UserRole role() {
        return role;
    }

    public long tokenVersion() {
        return tokenVersion;
    }
}
