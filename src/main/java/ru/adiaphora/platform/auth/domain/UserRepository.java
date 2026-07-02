package ru.adiaphora.platform.auth.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository abstraction for {@link User}. Implemented by an infrastructure adapter over
 * Spring Data JPA; the domain and application layers depend only on this interface.
 */
public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);
}
