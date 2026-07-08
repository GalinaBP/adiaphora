package ru.adiaphora.platform.rules.application;

import org.springframework.stereotype.Service;
import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.questionnaire.api.QuestionnaireSnapshot;
import ru.adiaphora.platform.rules.domain.EngineResult;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEngine;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Anonymous MFC-eligibility estimate for the public home page. Runs the very same {@link RuleEngine}
 * and rule classes as the authenticated evaluation over an ad-hoc snapshot of the submitted answers —
 * so the estimate always matches what the real evaluation would say. Deliberately persists
 * <strong>nothing</strong>: no application, no evaluation record, no audit trail of the answers.
 */
@Service
public class EstimateEligibilityUseCase {

    /** Plain-language buckets the public estimator distinguishes. */
    public enum Verdict {
        /** Debt inside the MFC band, no blocking answers: extrajudicial route looks available. */
        MFC_ELIGIBLE,
        /** Debt outside the 25,000–1,000,000 RUB extrajudicial band. */
        AMOUNT_OUT_OF_RANGE,
        /** Something in the answers needs a lawyer's look (mortgage, prior bankruptcy, …). */
        MANUAL_REVIEW,
        /** Not enough answers to estimate. */
        NEEDS_INFORMATION
    }

    public record Estimate(Verdict verdict, BankruptcyRoute route, List<String> messages,
                           List<String> missingInformation, String rulesetVersion) {
    }

    private final RuleEngine engine;

    public EstimateEligibilityUseCase(RuleEngine engine) {
        this.engine = engine;
    }

    public Estimate estimate(Map<String, String> answers) {
        EngineResult result = engine.evaluate(new RuleContext(
                new QuestionnaireSnapshot(UUID.randomUUID(), null, answers)));

        List<String> messages = result.triggered().stream()
                .map(RuleEvaluation::userMessage)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return new Estimate(verdictOf(result), result.route(), messages,
                result.missingInformation(), RuleInputs.RULESET_VERSION);
    }

    private Verdict verdictOf(EngineResult result) {
        if (!result.missingInformation().isEmpty()) {
            return Verdict.NEEDS_INFORMATION;
        }
        if (result.manualReviewRequired()) {
            return Verdict.MANUAL_REVIEW;
        }
        return switch (result.route()) {
            case MFC_PRELIMINARY -> Verdict.MFC_ELIGIBLE;
            case NOT_CURRENTLY_RECOMMENDED, COURT_PRELIMINARY -> Verdict.AMOUNT_OUT_OF_RANGE;
            default -> Verdict.NEEDS_INFORMATION;
        };
    }
}
