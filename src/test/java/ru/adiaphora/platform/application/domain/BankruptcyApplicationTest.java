package ru.adiaphora.platform.application.domain;

import org.junit.jupiter.api.Test;
import ru.adiaphora.platform.application.api.BankruptcyApplicationStatus;
import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.common.error.InvalidApplicationStateException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BankruptcyApplicationTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final Instant AT = Instant.parse("2026-07-01T10:15:30Z");

    @Test
    void createStartsInDraftWithInitialHistory() {
        BankruptcyApplication app = BankruptcyApplication.create(UUID.randomUUID(), OWNER, AT);

        assertThat(app.status()).isEqualTo(BankruptcyApplicationStatus.DRAFT);
        assertThat(app.route()).isEqualTo(BankruptcyRoute.NOT_EVALUATED);
        assertThat(app.submittedAt()).isNull();

        List<StatusChange> changes = app.drainNewStatusChanges();
        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).from()).isNull();
        assertThat(changes.get(0).to()).isEqualTo(BankruptcyApplicationStatus.DRAFT);
    }

    @Test
    void submitMovesToReadyForEvaluationAndSetsSubmittedAt() {
        BankruptcyApplication app = BankruptcyApplication.create(UUID.randomUUID(), OWNER, AT);
        app.drainNewStatusChanges();

        app.submit(OWNER, AT);

        assertThat(app.status()).isEqualTo(BankruptcyApplicationStatus.READY_FOR_EVALUATION);
        assertThat(app.submittedAt()).isEqualTo(AT);
        assertThat(app.drainNewStatusChanges())
                .singleElement()
                .satisfies(change -> {
                    assertThat(change.from()).isEqualTo(BankruptcyApplicationStatus.DRAFT);
                    assertThat(change.to()).isEqualTo(BankruptcyApplicationStatus.READY_FOR_EVALUATION);
                });
    }

    @Test
    void cancelMovesToCancelled() {
        BankruptcyApplication app = BankruptcyApplication.create(UUID.randomUUID(), OWNER, AT);

        app.cancel("no longer needed", OWNER, AT);

        assertThat(app.status()).isEqualTo(BankruptcyApplicationStatus.CANCELLED);
    }

    @Test
    void drainClearsAccumulatedChanges() {
        BankruptcyApplication app = BankruptcyApplication.create(UUID.randomUUID(), OWNER, AT);

        assertThat(app.drainNewStatusChanges()).hasSize(1);
        assertThat(app.drainNewStatusChanges()).isEmpty();
    }

    @Test
    void illegalTransitionIsRejected() {
        BankruptcyApplication app = BankruptcyApplication.create(UUID.randomUUID(), OWNER, AT);

        assertThatThrownBy(() ->
                app.transitionTo(BankruptcyApplicationStatus.COMPLETED, "nope", OWNER, AT))
                .isInstanceOf(InvalidApplicationStateException.class);
    }

    @Test
    void cannotSubmitATerminalCase() {
        BankruptcyApplication app = BankruptcyApplication.create(UUID.randomUUID(), OWNER, AT);
        app.cancel("done", OWNER, AT);

        assertThatThrownBy(() -> app.submit(OWNER, AT))
                .isInstanceOf(InvalidApplicationStateException.class);
    }

    @Test
    void ownershipCheck() {
        BankruptcyApplication app = BankruptcyApplication.create(UUID.randomUUID(), OWNER, AT);

        assertThat(app.isOwnedBy(OWNER)).isTrue();
        assertThat(app.isOwnedBy(UUID.randomUUID())).isFalse();
    }
}
