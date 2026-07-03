package ru.adiaphora.platform.review.domain;

import org.junit.jupiter.api.Test;
import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.common.error.InvalidApplicationStateException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewTest {

    private static final Instant AT = Instant.parse("2026-07-02T09:00:00Z");
    private static final UUID REVIEWER = UUID.randomUUID();

    private Review openReview() {
        return Review.open(UUID.randomUUID(), UUID.randomUUID(), BankruptcyRoute.MANUAL_REVIEW, "rs-1");
    }

    @Test
    void opensInOpenStatus() {
        Review review = openReview();
        assertThat(review.status()).isEqualTo(ReviewStatus.OPEN);
        assertThat(review.assigneeId()).isNull();
    }

    @Test
    void assignSetsAssigneeAndStatus() {
        Review review = openReview();
        UUID assignee = UUID.randomUUID();
        review.assign(assignee);
        assertThat(review.status()).isEqualTo(ReviewStatus.ASSIGNED);
        assertThat(review.assigneeId()).isEqualTo(assignee);
    }

    @Test
    void approveWithoutRouteChangeRecordsNoOverride() {
        Review review = openReview();
        review.assign(UUID.randomUUID());
        review.approve(REVIEWER, null, "looks fine", AT);
        assertThat(review.status()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(review.drainNewOverrides()).isEmpty();
    }

    @Test
    void routeOverrideRequiresAReason() {
        Review review = openReview();
        review.assign(UUID.randomUUID());
        assertThatThrownBy(() -> review.approve(REVIEWER, BankruptcyRoute.COURT_PRELIMINARY, "  ", AT))
                .isInstanceOf(UndocumentedDecisionException.class);
    }

    @Test
    void documentedRouteOverrideIsRecorded() {
        Review review = openReview();
        review.assign(UUID.randomUUID());
        review.approve(REVIEWER, BankruptcyRoute.COURT_PRELIMINARY, "assets exceed MFC", AT);

        assertThat(review.route()).isEqualTo(BankruptcyRoute.COURT_PRELIMINARY);
        assertThat(review.drainNewOverrides()).singleElement().satisfies(override -> {
            assertThat(override.previousRoute()).isEqualTo(BankruptcyRoute.MANUAL_REVIEW);
            assertThat(override.newRoute()).isEqualTo(BankruptcyRoute.COURT_PRELIMINARY);
            assertThat(override.reason()).isEqualTo("assets exceed MFC");
            assertThat(override.reviewerId()).isEqualTo(REVIEWER);
        });
    }

    @Test
    void rejectRequiresAReason() {
        Review review = openReview();
        review.assign(UUID.randomUUID());
        assertThatThrownBy(() -> review.reject(REVIEWER, null, AT))
                .isInstanceOf(UndocumentedDecisionException.class);
    }

    @Test
    void cannotDecideAnUnassignedReview() {
        Review review = openReview();
        assertThatThrownBy(() -> review.approve(REVIEWER, null, "x", AT))
                .isInstanceOf(InvalidApplicationStateException.class);
    }
}
