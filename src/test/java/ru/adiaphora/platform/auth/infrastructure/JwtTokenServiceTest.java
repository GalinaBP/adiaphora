package ru.adiaphora.platform.auth.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import ru.adiaphora.platform.auth.application.AuthTokens;
import ru.adiaphora.platform.auth.application.RefreshTokenClaims;
import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.auth.domain.UserRole;
import ru.adiaphora.platform.common.security.AuthenticatedUser;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);
    private final JwtProperties properties = new JwtProperties(
            "access-secret-0123456789-0123456789-0123456789",
            "refresh-secret-0123456789-0123456789-0123456789",
            Duration.ofMinutes(15),
            Duration.ofDays(30));
    private final JwtTokenService tokenService = new JwtTokenService(properties, CLOCK);

    private final User user = User.rehydrate(UUID.randomUUID(), "user@example.test",
            "hash", ru.adiaphora.platform.auth.domain.UserStatus.ACTIVE, UserRole.LAWYER, 3L);

    @Test
    void issuesAndParsesAccessToken() {
        AuthTokens tokens = tokenService.issueTokens(user);

        Optional<AuthenticatedUser> principal = tokenService.parseAccessToken(tokens.accessToken());

        assertThat(principal).isPresent();
        assertThat(principal.get().userId()).isEqualTo(user.id());
        assertThat(principal.get().email()).isEqualTo("user@example.test");
        assertThat(principal.get().role()).isEqualTo(UserRole.LAWYER.authority());
        assertThat(tokens.accessTokenExpiresInSeconds()).isEqualTo(Duration.ofMinutes(15).toSeconds());
    }

    @Test
    void issuesAndParsesRefreshTokenWithTokenVersion() {
        AuthTokens tokens = tokenService.issueTokens(user);

        RefreshTokenClaims claims = tokenService.parseRefreshToken(tokens.refreshToken());

        assertThat(claims.userId()).isEqualTo(user.id());
        assertThat(claims.tokenVersion()).isEqualTo(3L);
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshToken() {
        AuthTokens tokens = tokenService.issueTokens(user);

        assertThatThrownBy(() -> tokenService.parseRefreshToken(tokens.accessToken()))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshTokenIsNotAcceptedAsAccessToken() {
        AuthTokens tokens = tokenService.issueTokens(user);

        assertThat(tokenService.parseAccessToken(tokens.refreshToken())).isEmpty();
    }

    @Test
    void tamperedTokenIsRejected() {
        AuthTokens tokens = tokenService.issueTokens(user);

        assertThat(tokenService.parseAccessToken(tokens.accessToken() + "x")).isEmpty();
    }
}
