package ru.adiaphora.platform.auth.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory login lockout: after {@code maxAttempts} consecutive failures for an email, further
 * attempts are locked for {@code lockDuration}. Keyed on the submitted (normalised) email whether or
 * not the account exists, so it does not become an account-enumeration oracle.
 *
 * <p>Deliberately simple and per-instance. It raises the cost of online brute-force / credential
 * stuffing without external infrastructure. Known trade-offs for a future, distributed version:
 * (1) state is not shared across app instances; (2) an attacker can lock a victim out temporarily
 * (mitigated by the automatic, time-boxed unlock). See {@code docs/security.md}.
 */
@Component
public class LoginAttemptTracker {

    static final int DEFAULT_MAX_ATTEMPTS = 5;
    static final Duration DEFAULT_LOCK_DURATION = Duration.ofMinutes(15);

    private final Clock clock;
    private final int maxAttempts;
    private final Duration lockDuration;
    private final Map<String, Attempts> byEmail = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptTracker(Clock clock) {
        this(clock, DEFAULT_MAX_ATTEMPTS, DEFAULT_LOCK_DURATION);
    }

    LoginAttemptTracker(Clock clock, int maxAttempts, Duration lockDuration) {
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
    }

    /** Whether the email is currently locked out. */
    public boolean isLocked(String email) {
        Attempts attempts = byEmail.get(key(email));
        return attempts != null && attempts.lockedUntil != null
                && clock.instant().isBefore(attempts.lockedUntil);
    }

    /** Record a failed attempt, locking the email once the threshold is reached. */
    public void recordFailure(String email) {
        Instant now = clock.instant();
        byEmail.compute(key(email), (ignored, existing) -> {
            int prior = (existing == null || existing.isExpired(now)) ? 0 : existing.failures;
            int failures = prior + 1;
            Instant lockedUntil = failures >= maxAttempts ? now.plus(lockDuration) : null;
            return new Attempts(failures, lockedUntil);
        });
    }

    /** Clear all recorded failures for the email (called on a successful login). */
    public void reset(String email) {
        byEmail.remove(key(email));
    }

    private static String key(String email) {
        return email.trim().toLowerCase();
    }

    private record Attempts(int failures, Instant lockedUntil) {
        boolean isExpired(Instant now) {
            return lockedUntil != null && !now.isBefore(lockedUntil);
        }
    }
}
