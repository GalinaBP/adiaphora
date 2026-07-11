package ru.adiaphora.platform.rules.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A persisted anonymous eligibility-check session: the submitted answers, the derived verdict/route,
 * the grounds that qualified and the legal-basis citations behind the decision. Stored for audit and
 * future repeat-application logic (per the approved eligibility-flow ticket); contains no personal
 * identifiers.
 */
public record EligibilityCheckRecord(
        UUID id,
        Instant createdAt,
        String answersJson,
        String verdict,
        String route,
        String qualifyingGrounds,
        String citations,
        String rulesetVersion
) {
}
