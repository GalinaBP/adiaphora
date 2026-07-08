package ru.adiaphora.platform.rules.application;

import ru.adiaphora.platform.rules.api.RuleFinding;
import ru.adiaphora.platform.rules.api.RulesEvaluationResult;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleEvaluationRecord;

/** Maps the persisted {@link RuleEvaluationRecord} to the public {@link RulesEvaluationResult}, hiding
 * each rule's internal reason. */
final class RulesResultMapper {

    private RulesResultMapper() {
    }

    static RulesEvaluationResult toResult(RuleEvaluationRecord record) {
        return new RulesEvaluationResult(
                record.id(),
                record.applicationId(),
                record.rulesetVersion(),
                record.questionnaireVersion(),
                record.inputSnapshotHash(),
                record.route(),
                record.manualReviewRequired(),
                record.triggeredRules().stream().map(RulesResultMapper::toFinding).toList(),
                record.missingInformation(),
                record.startedAt(),
                record.completedAt());
    }

    private static RuleFinding toFinding(RuleEvaluation evaluation) {
        return new RuleFinding(
                evaluation.ruleCode(),
                evaluation.outcome(),
                evaluation.severity(),
                evaluation.userMessage(),
                evaluation.blocksAutomaticDecision());
    }
}
