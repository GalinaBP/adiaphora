package ru.adiaphora.platform.auth.infrastructure;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.auth.domain.UserRepository;

import java.util.Optional;
import java.util.UUID;

/** Adapts Spring Data JPA to the domain {@link UserRepository} contract. */
@Component
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(UserEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserEntity entity = jpa.findById(user.id())
                .map(existing -> {
                    existing.applyFrom(user);
                    return existing;
                })
                .orElseGet(() -> UserEntity.fromDomain(user));
        return jpa.save(entity).toDomain();
    }
}
