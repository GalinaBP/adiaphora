package ru.adiaphora.platform.rules.domain;

import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.rules.api.RuleOutcome;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs the configured rules over a {@link RuleContext} and derives a preliminary route. Pure and
 * deterministic — given the same rules and answers it always produces the same result, including
 * evaluation order: rules are sorted by {@link BankruptcyRule#order()} (ties broken by code), so the
 * sequence never depends on how the rule list was assembled.
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
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(BankruptcyRule::order)
                        .thenComparing(BankruptcyRule::code))
                .toList();
        requireUniqueCodes(this.rules);
    }

    /** Persisted findings are keyed by rule code, so two rules sharing a code would be indistinguishable. */
    private static void requireUniqueCodes(List<BankruptcyRule> rules) {
        Set<String> codes = new HashSet<>();
        for (BankruptcyRule rule : rules) {
            if (!codes.add(rule.code())) {
                throw new IllegalArgumentException("Duplicate rule code: " + rule.code());
            }
        }
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
