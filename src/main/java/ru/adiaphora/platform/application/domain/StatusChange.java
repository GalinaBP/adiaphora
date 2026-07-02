package ru.adiaphora.platform.application.domain;

import ru.adiaphora.platform.application.api.BankruptcyApplicationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * An immutable record of a single status transition, appended to the case's history. {@code from} is
 * {@code null} for the initial creation entry.
 */
public record StatusChange(
        BankruptcyApplicationStatus from,
        BankruptcyApplicationStatus to,
        String reason,
        UUID actorId,
        Instant changedAt
) {
}
