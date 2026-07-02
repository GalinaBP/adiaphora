package ru.adiaphora.platform.auth.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Public, read-only lookup of users by id, for other modules that need to display or validate a user
 * reference without touching the {@code auth} module's internals.
 */
public interface UserDirectory {

    Optional<AuthUserView> findById(UUID userId);

    Optional<AuthUserView> findByEmail(String email);
}
