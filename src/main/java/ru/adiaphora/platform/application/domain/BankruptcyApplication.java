package ru.adiaphora.platform.application.domain;

import ru.adiaphora.platform.application.api.BankruptcyApplicationStatus;
import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.common.error.InvalidApplicationStateException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static ru.adiaphora.platform.application.api.BankruptcyApplicationStatus.APPROVED_FOR_DOCUMENT_GENERATION;
import static ru.adiaphora.platform.application.api.BankruptcyApplicationStatus.CANCELLED;
import static ru.adiaphora.platform.application.api.BankruptcyApplicationStatus.COMPLETED;
import static ru.adiaphora.platform.application.api.BankruptcyApplicationStatus.DOCUMENTS_READY;
import static ru.adiaphora.platform.application.api.BankruptcyApplicationStatus.DRAFT;
import static ru.adiaphora.platform.application.api.BankruptcyApplicationStatus.MANUAL_REVIEW_REQUIRED;
import static ru.adiaphora.platform.application.api.BankruptcyApplicationStatus.NEEDS_INFORMATION;
import static ru.adiaphora.platform.application.api.BankruptcyApplicationStatus.QUESTIONNAIRE_IN_PROGRESS;
import static ru.adiaphora.platform.application.api.BankruptcyApplicationStatus.READY_FOR_EVALUATION;
import static ru.adiaphora.platform.application.api.BankruptcyApplicationStatus.UNDER_REVIEW;

/**
 * Bankruptcy case aggregate root. Encapsulates the lifecycle: it validates every status transition
 * against {@link #ALLOWED_TRANSITIONS} and records each one into an append-only history that the
 * persistence adapter drains and stores. No persistence/framework dependencies live here.
 *
 * <p>Time is passed in by callers (from the injectable {@code Clock}) rather than read here, so the
 * aggregate stays deterministic and unit-testable.
 */
public class BankruptcyApplication {

    private static final Map<BankruptcyApplicationStatus, Set<BankruptcyApplicationStatus>> ALLOWED_TRANSITIONS =
            buildAllowedTransitions();

    private final UUID id;
    private final UUID ownerId;
    private BankruptcyApplicationStatus status;
    private BankruptcyRoute route;
    private Instant submittedAt;

    private final List<StatusChange> newStatusChanges = new ArrayList<>();

    private BankruptcyApplication(UUID id, UUID ownerId, BankruptcyApplicationStatus status,
                                  BankruptcyRoute route, Instant submittedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.status = status;
        this.route = route;
        this.submittedAt = submittedAt;
    }

    /** Creates a new case in {@link BankruptcyApplicationStatus#DRAFT}. */
    public static BankruptcyApplication create(UUID id, UUID ownerId, Instant at) {
        BankruptcyApplication application =
                new BankruptcyApplication(id, ownerId, DRAFT, BankruptcyRoute.NOT_EVALUATED, null);
        application.newStatusChanges.add(new StatusChange(null, DRAFT, "created", ownerId, at));
        return application;
    }

    /** Reconstitutes an existing case from storage (without replaying history). */
    public static BankruptcyApplication rehydrate(UUID id, UUID ownerId, BankruptcyApplicationStatus status,
                                                  BankruptcyRoute route, Instant submittedAt) {
        return new BankruptcyApplication(id, ownerId, status, route, submittedAt);
    }

    /** Submits the case for evaluation, moving it to {@code READY_FOR_EVALUATION}. */
    public void submit(UUID actorId, Instant at) {
        transitionTo(READY_FOR_EVALUATION, "submitted for evaluation", actorId, at);
        this.submittedAt = at;
    }

    /** Cancels the case. Allowed from any non-terminal status. */
    public void cancel(String reason, UUID actorId, Instant at) {
        transitionTo(CANCELLED, reason, actorId, at);
    }

    /** Records the preliminary route computed by the rules module. Does not change status. */
    public void assignRoute(BankruptcyRoute newRoute) {
        this.route = newRoute;
    }

    /** Performs a validated status transition and records it in history. */
    public void transitionTo(BankruptcyApplicationStatus target, String reason, UUID actorId, Instant at) {
        if (status == target) {
            return;
        }
        Set<BankruptcyApplicationStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(status, Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidApplicationStateException(
                    "Cannot transition case from " + status + " to " + target);
        }
        BankruptcyApplicationStatus previous = this.status;
        this.status = target;
        this.newStatusChanges.add(new StatusChange(previous, target, reason, actorId, at));
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerId.equals(userId);
    }

    /** Returns and clears the status changes accumulated since the last drain (for persistence). */
    public List<StatusChange> drainNewStatusChanges() {
        List<StatusChange> drained = List.copyOf(newStatusChanges);
        newStatusChanges.clear();
        return drained;
    }

    public UUID id() {
        return id;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public BankruptcyApplicationStatus status() {
        return status;
    }

    public BankruptcyRoute route() {
        return route;
    }

    public Instant submittedAt() {
        return submittedAt;
    }

    private static Map<BankruptcyApplicationStatus, Set<BankruptcyApplicationStatus>> buildAllowedTransitions() {
        Map<BankruptcyApplicationStatus, Set<BankruptcyApplicationStatus>> map = new EnumMap<>(BankruptcyApplicationStatus.class);
        map.put(DRAFT, Set.of(QUESTIONNAIRE_IN_PROGRESS, READY_FOR_EVALUATION, CANCELLED));
        map.put(QUESTIONNAIRE_IN_PROGRESS, Set.of(READY_FOR_EVALUATION, DRAFT, CANCELLED));
        map.put(READY_FOR_EVALUATION, Set.of(NEEDS_INFORMATION, MANUAL_REVIEW_REQUIRED,
                APPROVED_FOR_DOCUMENT_GENERATION, CANCELLED));
        map.put(NEEDS_INFORMATION, Set.of(QUESTIONNAIRE_IN_PROGRESS, READY_FOR_EVALUATION, CANCELLED));
        map.put(MANUAL_REVIEW_REQUIRED, Set.of(UNDER_REVIEW, CANCELLED));
        map.put(UNDER_REVIEW, Set.of(APPROVED_FOR_DOCUMENT_GENERATION, NEEDS_INFORMATION, CANCELLED));
        map.put(APPROVED_FOR_DOCUMENT_GENERATION, Set.of(DOCUMENTS_READY, CANCELLED));
        map.put(DOCUMENTS_READY, Set.of(COMPLETED, CANCELLED));
        map.put(COMPLETED, Set.of());
        map.put(CANCELLED, Set.of());
        return map;
    }
}
