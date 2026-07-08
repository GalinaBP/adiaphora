package ru.adiaphora.platform.review.domain;

import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.common.error.InvalidApplicationStateException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manual-review task aggregate. Enforces the review lifecycle and guarantees that any change of the
 * recommended route is documented: an override requires a reason and is recorded in the review's
 * history. Decisions (approve/reject) also require a reason.
 */
public class Review {

    private static final Set<ReviewStatus> ASSIGNABLE =
            EnumSet.of(ReviewStatus.OPEN, ReviewStatus.ASSIGNED, ReviewStatus.WAITING_FOR_INFORMATION,
                    ReviewStatus.IN_PROGRESS);
    private static final Set<ReviewStatus> DECIDABLE =
            EnumSet.of(ReviewStatus.ASSIGNED, ReviewStatus.IN_PROGRESS, ReviewStatus.WAITING_FOR_INFORMATION);

    private final UUID id;
    private final UUID applicationId;
    private final String rulesetVersion;
    private ReviewStatus status;
    private UUID assigneeId;
    private BankruptcyRoute route;
    private String lastDecisionReason;

    private final List<RouteOverride> newOverrides = new ArrayList<>();

    private Review(UUID id, UUID applicationId, String rulesetVersion, ReviewStatus status,
                   UUID assigneeId, BankruptcyRoute route) {
        this.id = id;
        this.applicationId = applicationId;
        this.rulesetVersion = rulesetVersion;
        this.status = status;
        this.assigneeId = assigneeId;
        this.route = route;
    }

    /** Opens a new review for an application flagged for manual review. */
    public static Review open(UUID id, UUID applicationId, BankruptcyRoute route, String rulesetVersion) {
        return new Review(id, applicationId, rulesetVersion, ReviewStatus.OPEN, null, route);
    }

    public static Review rehydrate(UUID id, UUID applicationId, String rulesetVersion, ReviewStatus status,
                                   UUID assigneeId, BankruptcyRoute route, String lastDecisionReason) {
        Review review = new Review(id, applicationId, rulesetVersion, status, assigneeId, route);
        review.lastDecisionReason = lastDecisionReason;
        return review;
    }

    public void assign(UUID assigneeId) {
        if (!ASSIGNABLE.contains(status)) {
            throw new InvalidApplicationStateException("Cannot assign a review in status " + status);
        }
        this.assigneeId = assigneeId;
        this.status = ReviewStatus.ASSIGNED;
    }

    public void requestInformation(String reason) {
        requireReason(reason, "Для запроса информации требуется причина");
        if (!DECIDABLE.contains(status) && status != ReviewStatus.ASSIGNED) {
            throw new InvalidApplicationStateException(
                    "Cannot request information for a review in status " + status);
        }
        this.lastDecisionReason = reason;
        this.status = ReviewStatus.WAITING_FOR_INFORMATION;
    }

    /**
     * Approves the review. If {@code newRoute} differs from the current route, a reason is mandatory
     * and the override is recorded.
     */
    public void approve(UUID reviewerId, BankruptcyRoute newRoute, String reason, Instant at) {
        ensureDecidable();
        if (newRoute != null && newRoute != route) {
            requireReason(reason, "Для изменения маршрута требуется причина");
            newOverrides.add(new RouteOverride(route, newRoute, reason, reviewerId, at, rulesetVersion));
            this.route = newRoute;
        }
        this.lastDecisionReason = reason;
        this.status = ReviewStatus.APPROVED;
    }

    public void reject(UUID reviewerId, String reason, Instant at) {
        ensureDecidable();
        requireReason(reason, "Для отклонения требуется причина");
        this.lastDecisionReason = reason;
        this.status = ReviewStatus.REJECTED;
    }

    public List<RouteOverride> drainNewOverrides() {
        List<RouteOverride> drained = List.copyOf(newOverrides);
        newOverrides.clear();
        return drained;
    }

    private void ensureDecidable() {
        if (!DECIDABLE.contains(status)) {
            throw new InvalidApplicationStateException(
                    "Cannot record a decision for a review in status " + status);
        }
    }

    private void requireReason(String reason, String message) {
        if (reason == null || reason.isBlank()) {
            throw new UndocumentedDecisionException(message);
        }
    }

    public UUID id() {
        return id;
    }

    public UUID applicationId() {
        return applicationId;
    }

    public String rulesetVersion() {
        return rulesetVersion;
    }

    public ReviewStatus status() {
        return status;
    }

    public UUID assigneeId() {
        return assigneeId;
    }

    public BankruptcyRoute route() {
        return route;
    }

    public String lastDecisionReason() {
        return lastDecisionReason;
    }
}
