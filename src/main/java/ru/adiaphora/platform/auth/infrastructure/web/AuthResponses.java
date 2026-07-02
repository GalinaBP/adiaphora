package ru.adiaphora.platform.auth.infrastructure.web;

import ru.adiaphora.platform.auth.application.AuthTokens;
import ru.adiaphora.platform.auth.application.UserProfile;

import java.util.UUID;

/** Response payloads for the auth endpoints. Never contain password hashes. */
public final class AuthResponses {

    private AuthResponses() {
    }

    public record RegisterResponse(UUID userId) {
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType,
                                long expiresIn) {
        public static TokenResponse from(AuthTokens tokens) {
            return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.tokenType(),
                    tokens.accessTokenExpiresInSeconds());
        }
    }

    public record MeResponse(UUID userId, String email, String role, String status) {
        public static MeResponse from(UserProfile profile) {
            return new MeResponse(profile.userId(), profile.email(), profile.role().name(),
                    profile.status().name());
        }
    }
}
