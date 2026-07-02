package ru.adiaphora.platform.auth.application;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.auth.domain.UserRepository;

/**
 * Exchanges a valid refresh token for a new token pair. The token's {@code tokenVersion} must match
 * the user's current version, so a logout (which bumps the version) invalidates outstanding refresh
 * tokens.
 */
@Service
public class RefreshTokensUseCase {

    private final UserRepository users;
    private final TokenService tokenService;

    public RefreshTokensUseCase(UserRepository users, TokenService tokenService) {
        this.users = users;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public AuthTokens refresh(String refreshToken) {
        RefreshTokenClaims claims = tokenService.parseRefreshToken(refreshToken);
        User user = users.findById(claims.userId())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!user.isActive() || user.tokenVersion() != claims.tokenVersion()) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        return tokenService.issueTokens(user);
    }
}
