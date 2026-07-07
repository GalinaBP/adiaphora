package ru.adiaphora.platform.auth.application;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for the in-memory login lockout, using an advanceable clock to exercise expiry. */
class LoginAttemptTrackerTest {

    private static final String EMAIL = "victim@example.test";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    // Small, deterministic policy: lock after 3 failures for 10 minutes.
    private final LoginAttemptTracker tracker =
            new LoginAttemptTracker(clock, 3, Duration.ofMinutes(10));

    @Test
    void notLockedInitially() {
        assertThat(tracker.isLocked(EMAIL)).isFalse();
    }

    @Test
    void locksOnlyAfterReachingTheThreshold() {
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);
        assertThat(tracker.isLocked(EMAIL)).isFalse();

        tracker.recordFailure(EMAIL);
        assertThat(tracker.isLocked(EMAIL)).isTrue();
    }

    @Test
    void resetClearsFailures() {
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);
        tracker.reset(EMAIL);
        tracker.recordFailure(EMAIL);

        assertThat(tracker.isLocked(EMAIL)).isFalse();
    }

    @Test
    void lockExpiresAfterDurationAndCounterRestarts() {
        for (int i = 0; i < 3; i++) {
            tracker.recordFailure(EMAIL);
        }
        assertThat(tracker.isLocked(EMAIL)).isTrue();

        clock.advance(Duration.ofMinutes(10));
        assertThat(tracker.isLocked(EMAIL)).isFalse();

        // A single failure after expiry must not immediately re-lock.
        tracker.recordFailure(EMAIL);
        assertThat(tracker.isLocked(EMAIL)).isFalse();
    }

    @Test
    void keyIsCaseAndWhitespaceInsensitive() {
        tracker.recordFailure("  Victim@Example.Test ");
        tracker.recordFailure("victim@example.test");
        tracker.recordFailure("VICTIM@EXAMPLE.TEST");

        assertThat(tracker.isLocked(EMAIL)).isTrue();
    }

    /** Minimal advanceable clock for expiry tests. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            this.instant = this.instant.plus(duration);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
