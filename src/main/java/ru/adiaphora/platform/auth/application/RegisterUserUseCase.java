package ru.adiaphora.platform.auth.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.auth.api.UserRegisteredEvent;
import ru.adiaphora.platform.auth.domain.EmailAlreadyUsedException;
import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.auth.domain.UserRepository;
import ru.adiaphora.platform.auth.domain.UserRole;

import java.time.Clock;
import java.util.UUID;

/** Registers a new self-service {@code USER}, hashing the password and publishing a domain event. */
@Service
public class RegisterUserUseCase {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RegisterUserUseCase(UserRepository users, PasswordEncoder passwordEncoder,
                               ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public UUID register(RegisterCommand command) {
        String email = command.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        String passwordHash = passwordEncoder.encode(command.rawPassword());
        User user = User.register(UUID.randomUUID(), email, passwordHash, UserRole.USER);
        User saved = users.save(user);
        events.publishEvent(new UserRegisteredEvent(saved.id(), saved.email(), clock.instant()));
        return saved.id();
    }
}
