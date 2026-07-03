package ru.adiaphora.platform.review.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.common.persistence.BaseEntity;
import ru.adiaphora.platform.review.domain.Review;
import ru.adiaphora.platform.review.domain.ReviewStatus;

import java.util.UUID;

/** JPA view of the {@link Review} aggregate (its override history is a child table). */
@Entity
@Table(name = "reviews")
class ReviewEntity extends BaseEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "application_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReviewStatus status;

    @Column(name = "assignee_id", columnDefinition = "BINARY(16)")
    private UUID assigneeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "route", nullable = false, length = 48)
    private BankruptcyRoute route;

    @Column(name = "ruleset_version", nullable = false, length = 64)
    private String rulesetVersion;

    @Column(name = "last_decision_reason", length = 1000)
    private String lastDecisionReason;

    protected ReviewEntity() {
    }

    static ReviewEntity fromDomain(Review review) {
        ReviewEntity entity = new ReviewEntity();
        entity.id = review.id();
        entity.applicationId = review.applicationId();
        entity.rulesetVersion = review.rulesetVersion();
        entity.applyFrom(review);
        return entity;
    }

    void applyFrom(Review review) {
        this.status = review.status();
        this.assigneeId = review.assigneeId();
        this.route = review.route();
        this.lastDecisionReason = review.lastDecisionReason();
    }

    Review toDomain() {
        return Review.rehydrate(id, applicationId, rulesetVersion, status, assigneeId, route,
                lastDecisionReason);
    }
}
