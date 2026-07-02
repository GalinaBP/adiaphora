package ru.adiaphora.platform.auth.application;

import java.util.UUID;

/** The claims extracted from a validated refresh token. */
public record RefreshTokenClaims(UUID userId, long tokenVersion) {
}
