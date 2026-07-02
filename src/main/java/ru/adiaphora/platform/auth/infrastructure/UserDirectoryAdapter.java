package ru.adiaphora.platform.auth.infrastructure;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.auth.api.AuthUserView;
import ru.adiaphora.platform.auth.api.UserDirectory;

import java.util.Optional;
import java.util.UUID;

/** Public {@link UserDirectory} implementation for other modules. Never exposes credentials. */
@Component
class UserDirectoryAdapter implements UserDirectory {

    private final UserJpaRepository jpa;

    UserDirectoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<AuthUserView> findById(UUID userId) {
        return jpa.findById(userId).map(this::toView);
    }

    @Override
    public Optional<AuthUserView> findByEmail(String email) {
        return jpa.findByEmail(email == null ? null : email.trim().toLowerCase()).map(this::toView);
    }

    private AuthUserView toView(UserEntity entity) {
        return new AuthUserView(entity.getId(), entity.getEmail(), entity.getRole().authority());
    }
}
