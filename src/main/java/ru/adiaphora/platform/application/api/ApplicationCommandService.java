package ru.adiaphora.platform.application.api;

import java.util.UUID;

/**
 * Public commands other modules use to advance a case's lifecycle. For example, {@code rules} records
 * the evaluated route and moves the case to manual review; {@code review} approves for document
 * generation. These enforce the same status-transition rules as the owner-facing endpoints.
 */
public interface ApplicationCommandService {

    /** Records the preliminary route computed by the rules module. */
    void recordRoute(UUID applicationId, BankruptcyRoute route, String reason);

    /** Moves the case to the given status, validating the transition. */
    void transitionStatus(UUID applicationId, BankruptcyApplicationStatus target, String reason,
                          UUID actorId);
}
