package ru.adiaphora.platform.application.api;

import java.time.Instant;
import java.util.UUID;

/** Read-only snapshot of a bankruptcy case for consumption by other modules and query endpoints. */
public record ApplicationView(
        UUID applicationId,
        UUID ownerId,
        BankruptcyApplicationStatus status,
        BankruptcyRoute route,
        Instant submittedAt
) {
}
