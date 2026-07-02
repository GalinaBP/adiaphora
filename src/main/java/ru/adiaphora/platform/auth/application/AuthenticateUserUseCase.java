package ru.adiaphora.platform.auth.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.auth.api.UserLoginFailedEvent;
import ru.adiaphora.platform.auth.api.UserLoginSucceededEvent;
import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.auth.domain.UserRepository;

import java.time.Clock;
import java.util.Optional;

/**
 * Authenticates email + password and issues tokens. Uses a single generic failure message and
 * publishes a login-failed event without leaking whether the email exists.
 */
@Service
public class AuthenticateUserUseCase {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AuthenticateUserUseCase(UserRepository users, PasswordEncoder passwordEncoder,
                                   TokenService tokenService, ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public AuthTokens login(LoginCommand command) {
        String email = command.email().trim().toLowerCase();
        Optional<User> maybeUser = users.findByEmail(email);

        if (maybeUser.isEmpty()
                || !maybeUser.get().isActive()
                || !passwordEncoder.matches(command.rawPassword(), maybeUser.get().passwordHash())) {
            events.publishEvent(new UserLoginFailedEvent(email, clock.instant()));
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = maybeUser.get();
        AuthTokens tokens = tokenService.issueTokens(user);
        events.publishEvent(new UserLoginSucceededEvent(user.id(), clock.instant()));
        return tokens;
    }
}
