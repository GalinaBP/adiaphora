package ru.adiaphora.platform.auth.application;

import ru.adiaphora.platform.auth.domain.UserRole;
import ru.adiaphora.platform.auth.domain.UserStatus;

import java.util.UUID;

/** The authenticated user's own profile, returned by {@code GET /auth/me}. Never includes the hash. */
public record UserProfile(UUID userId, String email, UserRole role, UserStatus status) {
}
