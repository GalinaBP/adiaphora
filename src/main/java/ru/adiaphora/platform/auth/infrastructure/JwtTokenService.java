package ru.adiaphora.platform.auth.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import ru.adiaphora.platform.auth.application.AuthTokens;
import ru.adiaphora.platform.auth.application.RefreshTokenClaims;
import ru.adiaphora.platform.auth.application.TokenService;
import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.common.security.AuthenticatedUser;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * jjwt-based {@link TokenService}. Access tokens carry the principal (id/email/role); refresh tokens
 * carry the user id and {@code tokenVersion} used for logout-based revocation. Access and refresh
 * tokens are signed with separate secrets, and each token's {@code type} claim is checked on parse so
 * a refresh token can never be used as an access token or vice versa.
 */
@Service
public class JwtTokenService implements TokenService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_VERSION = "tokenVersion";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final JwtProperties properties;
    private final Clock clock;
    /** jjwt clock so token expiry is validated against the same (injectable) time source as issuing. */
    private final io.jsonwebtoken.Clock jwtClock;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.jwtClock = () -> Date.from(clock.instant());
        this.accessKey = Keys.hmacShaKeyFor(properties.accessSecret().getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(properties.refreshSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public AuthTokens issueTokens(User user) {
        Instant now = clock.instant();
        Instant accessExpiry = now.plus(properties.accessTokenTtl());
        Instant refreshExpiry = now.plus(properties.refreshTokenTtl());

        String accessToken = Jwts.builder()
                .subject(user.id().toString())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_EMAIL, user.email())
                .claim(CLAIM_ROLE, user.role().authority())
                .issuedAt(Date.from(now))
                .expiration(Date.from(accessExpiry))
                .signWith(accessKey)
                .compact();

        String refreshToken = Jwts.builder()
                .subject(user.id().toString())
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim(CLAIM_TOKEN_VERSION, user.tokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(refreshExpiry))
                .signWith(refreshKey)
                .compact();

        return AuthTokens.bearer(accessToken, refreshToken, properties.accessTokenTtl().toSeconds());
    }

    @Override
    public Optional<AuthenticatedUser> parseAccessToken(String token) {
        try {
            Claims claims = parse(token, accessKey);
            if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get(CLAIM_EMAIL, String.class);
            String role = claims.get(CLAIM_ROLE, String.class);
            return Optional.of(new AuthenticatedUser(userId, email, role));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    public RefreshTokenClaims parseRefreshToken(String token) {
        try {
            Claims claims = parse(token, refreshKey);
            if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new BadCredentialsException("Invalid refresh token");
            }
            UUID userId = UUID.fromString(claims.getSubject());
            long tokenVersion = claims.get(CLAIM_TOKEN_VERSION, Number.class).longValue();
            return new RefreshTokenClaims(userId, tokenVersion);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    private Claims parse(String token, SecretKey key) {
        return Jwts.parser()
                .clock(jwtClock)
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
