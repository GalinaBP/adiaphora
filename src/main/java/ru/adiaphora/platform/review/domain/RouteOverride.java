package ru.adiaphora.platform.review.domain;

import ru.adiaphora.platform.application.api.BankruptcyRoute;

import java.time.Instant;
import java.util.UUID;

/**
 * An immutable, documented change of the recommended route made by a reviewer. Every field is
 * required so a route override can never be undocumented.
 */
public record RouteOverride(
        BankruptcyRoute previousRoute,
        BankruptcyRoute newRoute,
        String reason,
        UUID reviewerId,
        Instant reviewedAt,
        String rulesetVersion
) {
}
