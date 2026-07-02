package ru.adiaphora.platform.auth.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.auth.domain.UserRepository;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;

import java.util.UUID;

/**
 * Logs the user out by revoking outstanding refresh tokens (bumping {@code tokenVersion}). Already
 * issued short-lived access tokens remain valid until they expire, by design.
 */
@Service
public class LogoutUseCase {

    private final UserRepository users;

    public LogoutUseCase(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public void logout(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        user.revokeRefreshTokens();
        users.save(user);
    }
}
