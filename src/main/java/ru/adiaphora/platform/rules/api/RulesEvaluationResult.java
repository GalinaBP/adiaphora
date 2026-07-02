package ru.adiaphora.platform.rules.api;

import ru.adiaphora.platform.application.api.BankruptcyRoute;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The public result of a rules evaluation: the preliminary route, whether manual review is required,
 * the triggered findings, and what information is still missing. {@code questionnaireVersion} is null
 * when no questionnaire answers exist yet.
 */
public record RulesEvaluationResult(
        UUID evaluationId,
        UUID applicationId,
        String rulesetVersion,
        String questionnaireVersion,
        BankruptcyRoute route,
        boolean manualReviewRequired,
        List<RuleFinding> triggeredRules,
        List<String> missingInformation,
        Instant startedAt,
        Instant completedAt
) {
}
