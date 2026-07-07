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
 *
 * <p>Two anti-brute-force measures: a {@link LoginAttemptTracker} locks an email after repeated
 * failures, and every code path performs exactly one password-hash comparison (against a fixed dummy
 * hash when the user is missing or inactive) so response timing does not reveal which emails exist.
 */
@Service
public class AuthenticateUserUseCase {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final ApplicationEventPublisher events;
    private final LoginAttemptTracker loginAttempts;
    private final Clock clock;

    /** Precomputed valid hash used to equalise timing when there is no real hash to check. */
    private final String dummyHash;

    public AuthenticateUserUseCase(UserRepository users, PasswordEncoder passwordEncoder,
                                   TokenService tokenService, ApplicationEventPublisher events,
                                   LoginAttemptTracker loginAttempts, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.events = events;
        this.loginAttempts = loginAttempts;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("timing-equalisation-placeholder");
    }

    @Transactional
    public AuthTokens login(LoginCommand command) {
        String email = command.email().trim().toLowerCase();

        if (loginAttempts.isLocked(email)) {
            // Still spend the hashing time so a locked email is not distinguishable by timing.
            passwordEncoder.matches(command.rawPassword(), dummyHash);
            throw failed(email);
        }

        Optional<User> maybeUser = users.findByEmail(email);
        boolean active = maybeUser.map(User::isActive).orElse(false);
        String hashToCheck = active ? maybeUser.get().passwordHash() : dummyHash;

        // Always run matches() (constant-ish timing); `active` guards the dummy-hash branch.
        if (!passwordEncoder.matches(command.rawPassword(), hashToCheck) || !active) {
            loginAttempts.recordFailure(email);
            throw failed(email);
        }

        loginAttempts.reset(email);
        User user = maybeUser.get();
        AuthTokens tokens = tokenService.issueTokens(user);
        events.publishEvent(new UserLoginSucceededEvent(user.id(), clock.instant()));
        return tokens;
    }

    private BadCredentialsException failed(String email) {
        events.publishEvent(new UserLoginFailedEvent(email, clock.instant()));
        return new BadCredentialsException("Invalid email or password");
    }
}
