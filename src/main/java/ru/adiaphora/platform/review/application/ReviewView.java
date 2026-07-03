package ru.adiaphora.platform.review.application;

import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.review.domain.Review;
import ru.adiaphora.platform.review.domain.ReviewStatus;

import java.util.UUID;

/** Read model of a review for the review endpoints. */
public record ReviewView(
        UUID reviewId,
        UUID applicationId,
        ReviewStatus status,
        UUID assigneeId,
        BankruptcyRoute route,
        String rulesetVersion,
        String lastDecisionReason
) {

    static ReviewView from(Review review) {
        return new ReviewView(review.id(), review.applicationId(), review.status(), review.assigneeId(),
                review.route(), review.rulesetVersion(), review.lastDecisionReason());
    }
}
