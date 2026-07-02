package ru.adiaphora.platform.auth.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.auth.domain.UserRepository;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;

import java.util.UUID;

/** Loads the authenticated user's own profile for {@code GET /auth/me}. */
@Service
public class GetCurrentUserUseCase {

    private final UserRepository users;

    public GetCurrentUserUseCase(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserProfile currentProfile(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        return new UserProfile(user.id(), user.email(), user.role(), user.status());
    }
}
