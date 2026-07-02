package ru.adiaphora.platform.auth.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT configuration bound from {@code app.security.jwt.*}. Secrets are supplied via environment
 * variables and must be at least 32 bytes (256 bits) for HS256.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String accessSecret,
        String refreshSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {
}
