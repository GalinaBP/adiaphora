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
 * <p>Route priority mirrors the staged eligibility flow (127-ФЗ, ст. 223.2), where the amount check,
 * the 5-year repeat-filing bar and the "no statutory ground" answer are hard gates that decide the
 * route even when later-stage answers are missing:
 * <ol>
 *   <li>debt above the MFC upper bound → {@code COURT_PRELIMINARY};</li>
 *   <li>debt below the MFC lower bound → {@code NOT_CURRENTLY_RECOMMENDED};</li>
 *   <li>previous bankruptcy inside the 5-year bar → {@code COURT_PRELIMINARY};</li>
 *   <li>"none of the categories" selected alone → {@code COURT_PRELIMINARY};</li>
 *   <li>any missing information → {@code INSUFFICIENT_INFORMATION};</li>
 *   <li>any manual-review/blocking trigger → {@code MANUAL_REVIEW};</li>
 *   <li>at least one statutory-ground rule evaluated and none passed → {@code COURT_PRELIMINARY}
 *       (eligibility is OR across grounds);</li>
 *   <li>otherwise → {@code MFC_PRELIMINARY}.</li>
 * </ol>
 */
public class RuleEngine {

    private static final String MFC_LOWER = "MFC-AMOUNT-LOWER-BOUND";
    private static final String MFC_UPPER = "MFC-AMOUNT-UPPER-BOUND";
    private static final String PRIOR_BANKRUPTCY_5Y = "MFC-PRIOR-BANKRUPTCY-5Y";
    private static final String NO_STATUTORY_GROUND = "MFC-NO-STATUTORY-GROUND";

    /** Rule-code prefix shared by the statutory-ground rules aggregated with OR semantics. */
    public static final String GROUND_RULE_PREFIX = "MFC-GROUND-";

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
        boolean manualReviewRequired = false;
        if (failed(evaluations, MFC_UPPER)) {
            route = BankruptcyRoute.COURT_PRELIMINARY;
        } else if (failed(evaluations, MFC_LOWER)) {
            route = BankruptcyRoute.NOT_CURRENTLY_RECOMMENDED;
        } else if (failed(evaluations, PRIOR_BANKRUPTCY_5Y)) {
            route = BankruptcyRoute.COURT_PRELIMINARY;
        } else if (failed(evaluations, NO_STATUTORY_GROUND)) {
            route = BankruptcyRoute.COURT_PRELIMINARY;
        } else if (!missingInformation.isEmpty()) {
            route = BankruptcyRoute.INSUFFICIENT_INFORMATION;
        } else if (manualReview) {
            route = BankruptcyRoute.MANUAL_REVIEW;
            manualReviewRequired = true;
        } else if (noSelectedGroundPassed(evaluations)) {
            route = BankruptcyRoute.COURT_PRELIMINARY;
        } else {
            route = BankruptcyRoute.MFC_PRELIMINARY;
        }

        return new EngineResult(route, manualReviewRequired, evaluations, missingInformation);
    }

    /** OR across the statutory grounds: eligible only when at least one evaluated ground passed. */
    private boolean noSelectedGroundPassed(List<RuleEvaluation> evaluations) {
        List<RuleEvaluation> groundEvaluations = evaluations.stream()
                .filter(e -> e.ruleCode().startsWith(GROUND_RULE_PREFIX))
                .filter(e -> e.outcome() != RuleOutcome.NOT_APPLICABLE)
                .toList();
        return !groundEvaluations.isEmpty()
                && groundEvaluations.stream().noneMatch(e -> e.outcome() == RuleOutcome.PASSED);
    }

    private boolean failed(List<RuleEvaluation> evaluations, String ruleCode) {
        return evaluations.stream()
                .anyMatch(e -> e.ruleCode().equals(ruleCode) && e.outcome() == RuleOutcome.FAILED);
    }
}
