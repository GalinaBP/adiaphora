package ru.adiaphora.platform.auth.api;

import java.util.UUID;

/**
 * Read-only projection of a user that other modules may consume (e.g. {@code review} showing an
 * assignee). Deliberately excludes password hashes and any credential material.
 */
public record AuthUserView(UUID userId, String email, String role) {
}
