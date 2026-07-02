package ru.adiaphora.platform.rules.domain;

import ru.adiaphora.platform.application.api.BankruptcyRoute;

import java.util.List;

/**
 * The engine's internal output: the derived route, whether manual review is required, all rule
 * evaluations, and the codes of rules reporting missing information.
 */
public record EngineResult(
        BankruptcyRoute route,
        boolean manualReviewRequired,
        List<RuleEvaluation> evaluations,
        List<String> missingInformation
) {

    /** Only the triggered (non-passed, applicable) evaluations. */
    public List<RuleEvaluation> triggered() {
        return evaluations.stream().filter(RuleEvaluation::isTriggered).toList();
    }
}
