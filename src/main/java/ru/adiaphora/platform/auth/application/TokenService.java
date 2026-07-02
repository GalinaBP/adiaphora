package ru.adiaphora.platform.auth.application;

import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.common.security.AuthenticatedUser;

import java.util.Optional;

/**
 * Abstraction over token issuing/parsing so the application layer stays free of the concrete JWT
 * library. Implemented in infrastructure by {@code JwtTokenService}.
 */
public interface TokenService {

    /** Issues a fresh access + refresh token pair for the given user. */
    AuthTokens issueTokens(User user);

    /**
     * Validates an access token and maps it to the module-neutral principal, or empty if the token is
     * missing, malformed, expired, or otherwise invalid.
     */
    Optional<AuthenticatedUser> parseAccessToken(String token);

    /**
     * Validates a refresh token and returns its claims.
     *
     * @throws org.springframework.security.core.AuthenticationException if the token is invalid
     */
    RefreshTokenClaims parseRefreshToken(String token);
}
