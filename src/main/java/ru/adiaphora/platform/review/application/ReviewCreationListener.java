package ru.adiaphora.platform.review.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.adiaphora.platform.review.domain.Review;
import ru.adiaphora.platform.review.domain.ReviewRepository;
import ru.adiaphora.platform.rules.api.RulesEvaluatedEvent;

import java.util.UUID;

/**
 * Creates an OPEN review when a rules evaluation flags an application for manual review. Runs
 * synchronously within the evaluation transaction (so review creation is atomic with the evaluation)
 * and is idempotent — it skips creation when an active review already exists for the application.
 */
@Component
class ReviewCreationListener {

    private final ReviewRepository reviews;

    ReviewCreationListener(ReviewRepository reviews) {
        this.reviews = reviews;
    }

    @EventListener
    void on(RulesEvaluatedEvent event) {
        if (!event.manualReviewRequired() || reviews.existsActiveByApplicationId(event.applicationId())) {
            return;
        }
        reviews.save(Review.open(UUID.randomUUID(), event.applicationId(), event.route(),
                event.rulesetVersion()));
    }
}
