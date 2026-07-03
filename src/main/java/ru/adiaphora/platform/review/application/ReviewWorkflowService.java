package ru.adiaphora.platform.review.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.application.api.ApplicationCommandService;
import ru.adiaphora.platform.application.api.ApplicationQueryService;
import ru.adiaphora.platform.application.api.BankruptcyApplicationStatus;
import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.auth.api.UserDirectory;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.common.security.CurrentUser;
import ru.adiaphora.platform.review.api.ReviewAssignedEvent;
import ru.adiaphora.platform.review.api.ReviewDecisionRecordedEvent;
import ru.adiaphora.platform.review.domain.Review;
import ru.adiaphora.platform.review.domain.ReviewRepository;

import java.time.Clock;
import java.util.UUID;

/**
 * Manual-review workflow: assign, request information, approve, reject. Each action mutates the review
 * aggregate (which enforces documented decisions/overrides) and drives the application's lifecycle
 * through {@code application.api}. Role-based authorization is enforced on the controller endpoints.
 */
@Service
public class ReviewWorkflowService {

    private final ReviewRepository reviews;
    private final ApplicationQueryService applications;
    private final ApplicationCommandService applicationCommands;
    private final UserDirectory users;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ReviewWorkflowService(ReviewRepository reviews, ApplicationQueryService applications,
                                 ApplicationCommandService applicationCommands, UserDirectory users,
                                 CurrentUser currentUser, ApplicationEventPublisher events, Clock clock) {
        this.reviews = reviews;
        this.applications = applications;
        this.applicationCommands = applicationCommands;
        this.users = users;
        this.currentUser = currentUser;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public ReviewView assign(UUID reviewId, UUID assigneeId) {
        if (users.findById(assigneeId).isEmpty()) {
            throw ResourceNotFoundException.of("User", assigneeId);
        }
        Review review = load(reviewId);
        review.assign(assigneeId);
        reviews.save(review);

        transitionApplicationIf(review.applicationId(), BankruptcyApplicationStatus.MANUAL_REVIEW_REQUIRED,
                BankruptcyApplicationStatus.UNDER_REVIEW, "assigned for manual review");

        events.publishEvent(new ReviewAssignedEvent(review.id(), review.applicationId(), assigneeId,
                clock.instant()));
        return ReviewView.from(review);
    }

    @Transactional
    public ReviewView requestInformation(UUID reviewId, String reason) {
        Review review = load(reviewId);
        review.requestInformation(reason);
        reviews.save(review);
        return ReviewView.from(review);
    }

    @Transactional
    public ReviewView approve(UUID reviewId, BankruptcyRoute newRoute, String reason) {
        UUID reviewerId = currentUser.require().userId();
        Review review = load(reviewId);
        review.approve(reviewerId, newRoute, reason, clock.instant());
        reviews.save(review);

        applicationCommands.recordRoute(review.applicationId(), review.route(),
                reason == null ? "review approved" : reason);
        transitionApplicationIf(review.applicationId(), BankruptcyApplicationStatus.UNDER_REVIEW,
                BankruptcyApplicationStatus.APPROVED_FOR_DOCUMENT_GENERATION, "review approved");

        events.publishEvent(new ReviewDecisionRecordedEvent(review.id(), review.applicationId(),
                "APPROVED", reviewerId, clock.instant()));
        return ReviewView.from(review);
    }

    @Transactional
    public ReviewView reject(UUID reviewId, String reason) {
        UUID reviewerId = currentUser.require().userId();
        Review review = load(reviewId);
        review.reject(reviewerId, reason, clock.instant());
        reviews.save(review);

        transitionApplicationIf(review.applicationId(), BankruptcyApplicationStatus.UNDER_REVIEW,
                BankruptcyApplicationStatus.NEEDS_INFORMATION, "review rejected");

        events.publishEvent(new ReviewDecisionRecordedEvent(review.id(), review.applicationId(),
                "REJECTED", reviewerId, clock.instant()));
        return ReviewView.from(review);
    }

    private void transitionApplicationIf(UUID applicationId, BankruptcyApplicationStatus expected,
                                         BankruptcyApplicationStatus target, String reason) {
        applications.findById(applicationId)
                .filter(app -> app.status() == expected)
                .ifPresent(app -> applicationCommands.transitionStatus(applicationId, target, reason,
                        currentUser.require().userId()));
    }

    private Review load(UUID reviewId) {
        return reviews.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.of("Review", reviewId));
    }
}
