package ru.adiaphora.platform.rules.domain;

import ru.adiaphora.platform.application.api.BankruptcyRoute;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A persisted evaluation outcome for an application: the ruleset/questionnaire versions used, timing,
 * derived route, manual-review flag, the triggered rules (with internal reasons, for operators), and
 * which information was missing.
 */
public record RuleEvaluationRecord(
        UUID id,
        UUID applicationId,
        String rulesetVersion,
        String questionnaireVersion,
        Instant startedAt,
        Instant completedAt,
        BankruptcyRoute route,
        boolean manualReviewRequired,
        List<RuleEvaluation> triggeredRules,
        List<String> missingInformation
) {

    public static RuleEvaluationRecord create(UUID applicationId, String questionnaireVersion,
                                              Instant startedAt, Instant completedAt,
                                              EngineResult result) {
        return new RuleEvaluationRecord(
                UUID.randomUUID(),
                applicationId,
                RuleInputs.RULESET_VERSION,
                questionnaireVersion,
                startedAt,
                completedAt,
                result.route(),
                result.manualReviewRequired(),
                result.triggered(),
                result.missingInformation());
    }
}
