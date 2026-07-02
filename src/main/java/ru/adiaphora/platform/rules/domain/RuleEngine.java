package ru.adiaphora.platform.rules.domain;

import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.rules.api.RuleOutcome;

import java.util.List;

/**
 * Runs the configured rules over a {@link RuleContext} and derives a preliminary route. Pure and
 * deterministic — given the same rules and answers it always produces the same result.
 *
 * <p>Route priority (placeholder policy, pending legal review):
 * <ol>
 *   <li>any missing information → {@code INSUFFICIENT_INFORMATION};</li>
 *   <li>any manual-review/blocking trigger → {@code MANUAL_REVIEW};</li>
 *   <li>debt above the MFC upper bound → {@code COURT_PRELIMINARY};</li>
 *   <li>debt below the MFC lower bound → {@code NOT_CURRENTLY_RECOMMENDED};</li>
 *   <li>otherwise → {@code MFC_PRELIMINARY}.</li>
 * </ol>
 */
public class RuleEngine {

    private static final String MFC_LOWER = "MFC-AMOUNT-LOWER-BOUND";
    private static final String MFC_UPPER = "MFC-AMOUNT-UPPER-BOUND";

    private final List<BankruptcyRule> rules;

    public RuleEngine(List<BankruptcyRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public EngineResult evaluate(RuleContext context) {
        List<RuleEvaluation> evaluations = rules.stream()
                .map(rule -> rule.evaluate(context))
                .toList();

        List<String> missingInformation = evaluations.stream()
                .filter(RuleEvaluation::needsInformation)
                .map(RuleEvaluation::ruleCode)
                .toList();

        boolean manualReview = evaluations.stream().anyMatch(RuleEvaluation::requiresManualReview);

        BankruptcyRoute route;
        boolean manualReviewRequired;
        if (!missingInformation.isEmpty()) {
            route = BankruptcyRoute.INSUFFICIENT_INFORMATION;
            manualReviewRequired = false;
        } else if (manualReview) {
            route = BankruptcyRoute.MANUAL_REVIEW;
            manualReviewRequired = true;
        } else {
            route = deriveRouteFromBounds(evaluations);
            manualReviewRequired = false;
        }

        return new EngineResult(route, manualReviewRequired, evaluations, missingInformation);
    }

    private BankruptcyRoute deriveRouteFromBounds(List<RuleEvaluation> evaluations) {
        if (failed(evaluations, MFC_UPPER)) {
            return BankruptcyRoute.COURT_PRELIMINARY;
        }
        if (failed(evaluations, MFC_LOWER)) {
            return BankruptcyRoute.NOT_CURRENTLY_RECOMMENDED;
        }
        return BankruptcyRoute.MFC_PRELIMINARY;
    }

    private boolean failed(List<RuleEvaluation> evaluations, String ruleCode) {
        return evaluations.stream()
                .anyMatch(e -> e.ruleCode().equals(ruleCode) && e.outcome() == RuleOutcome.FAILED);
    }
}
