package ru.adiaphora.platform.review.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.review.domain.RouteOverride;

import java.time.Instant;
import java.util.UUID;

/** Immutable, append-only record of a documented route override made during review. */
@Entity
@Table(name = "review_route_overrides")
class ReviewRouteOverrideEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "review_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID reviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_route", nullable = false, length = 48)
    private BankruptcyRoute previousRoute;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_route", nullable = false, length = 48)
    private BankruptcyRoute newRoute;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "reviewer_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID reviewerId;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    @Column(name = "ruleset_version", nullable = false, length = 64)
    private String rulesetVersion;

    protected ReviewRouteOverrideEntity() {
    }

    static ReviewRouteOverrideEntity of(UUID reviewId, RouteOverride override) {
        ReviewRouteOverrideEntity entity = new ReviewRouteOverrideEntity();
        entity.id = UUID.randomUUID();
        entity.reviewId = reviewId;
        entity.previousRoute = override.previousRoute();
        entity.newRoute = override.newRoute();
        entity.reason = override.reason();
        entity.reviewerId = override.reviewerId();
        entity.reviewedAt = override.reviewedAt();
        entity.rulesetVersion = override.rulesetVersion();
        return entity;
    }

    RouteOverride toDomain() {
        return new RouteOverride(previousRoute, newRoute, reason, reviewerId, reviewedAt, rulesetVersion);
    }
}
