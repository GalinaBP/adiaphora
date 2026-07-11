package ru.adiaphora.platform.rules.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.questionnaire.api.QuestionnaireSnapshot;
import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.domain.EligibilityCheckRecord;
import ru.adiaphora.platform.rules.domain.EligibilityCheckRepository;
import ru.adiaphora.platform.rules.domain.EngineResult;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEngine;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Anonymous MFC-eligibility check for the public home page. Runs the very same {@link RuleEngine}
 * and rule classes as the authenticated evaluation over an ad-hoc snapshot of the submitted answers —
 * so the estimate always matches what the real evaluation would say. Each check session is persisted
 * (answers, verdict, qualifying grounds and legal citations) for audit and future repeat-application
 * logic; no personal identifiers are stored.
 */
@Service
public class EstimateEligibilityUseCase {

    /** Plain-language buckets the public estimator distinguishes. */
    public enum Verdict {
        /** Debt inside the MFC band, prior-bankruptcy bar clear, at least one ground confirmed. */
        MFC_ELIGIBLE,
        /** Debt outside the 25,000–1,000,000 RUB extrajudicial band (п. 1 ст. 223.2 127-ФЗ). */
        AMOUNT_OUT_OF_RANGE,
        /** Extrajudicial route unavailable (5-year bar, no ground applies or confirmed) — judicial procedure instead. */
        JUDICIAL_ROUTE,
        /** A "not sure" answer needs the applicant to check documents (or a lawyer's look). */
        MANUAL_REVIEW,
        /** Not enough answers to estimate. */
        NEEDS_INFORMATION
    }

    /** A statutory ground whose block passed, with the citation to show on the result screen. */
    public record QualifyingGround(String ruleCode, String message, String legalBasis) {
    }

    public record Estimate(Verdict verdict, BankruptcyRoute route, List<String> messages,
                           List<QualifyingGround> qualifyingGrounds, List<String> citations,
                           List<String> missingInformation, String rulesetVersion) {
    }

    private static final String MFC_LOWER = "MFC-AMOUNT-LOWER-BOUND";
    private static final String MFC_UPPER = "MFC-AMOUNT-UPPER-BOUND";

    private final RuleEngine engine;
    private final EligibilityCheckRepository checks;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public EstimateEligibilityUseCase(RuleEngine engine, EligibilityCheckRepository checks,
                                      ObjectMapper objectMapper, Clock clock) {
        this.engine = engine;
        this.checks = checks;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public Estimate estimate(Map<String, String> answers) {
        EngineResult result = engine.evaluate(new RuleContext(
                new QuestionnaireSnapshot(UUID.randomUUID(), null, answers)));

        List<String> messages = result.triggered().stream()
                .map(RuleEvaluation::userMessage)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<QualifyingGround> qualifyingGrounds = result.evaluations().stream()
                .filter(e -> e.ruleCode().startsWith(RuleEngine.GROUND_RULE_PREFIX))
                .filter(e -> e.outcome() == RuleOutcome.PASSED)
                .map(e -> new QualifyingGround(e.ruleCode(), e.userMessage(), e.legalBasis()))
                .toList();

        List<String> citations = result.evaluations().stream()
                .filter(e -> e.outcome() != RuleOutcome.NOT_APPLICABLE)
                .map(RuleEvaluation::legalBasis)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Estimate estimate = new Estimate(verdictOf(result), result.route(), messages,
                qualifyingGrounds, citations, result.missingInformation(), RuleInputs.RULESET_VERSION);
        persist(answers, estimate);
        return estimate;
    }

    private void persist(Map<String, String> answers, Estimate estimate) {
        checks.save(new EligibilityCheckRecord(
                UUID.randomUUID(),
                clock.instant(),
                objectMapper.writeValueAsString(answers),
                estimate.verdict().name(),
                estimate.route().name(),
                estimate.qualifyingGrounds().isEmpty() ? null : estimate.qualifyingGrounds().stream()
                        .map(QualifyingGround::ruleCode)
                        .collect(Collectors.joining(",")),
                estimate.citations().isEmpty() ? null : String.join("; ", estimate.citations()),
                estimate.rulesetVersion()));
    }

    private Verdict verdictOf(EngineResult result) {
        boolean amountOutOfRange = result.evaluations().stream()
                .anyMatch(e -> (e.ruleCode().equals(MFC_LOWER) || e.ruleCode().equals(MFC_UPPER))
                        && e.outcome() == RuleOutcome.FAILED);
        if (amountOutOfRange) {
            return Verdict.AMOUNT_OUT_OF_RANGE;
        }
        return switch (result.route()) {
            case MFC_PRELIMINARY -> Verdict.MFC_ELIGIBLE;
            case COURT_PRELIMINARY, NOT_CURRENTLY_RECOMMENDED -> Verdict.JUDICIAL_ROUTE;
            case MANUAL_REVIEW -> Verdict.MANUAL_REVIEW;
            default -> Verdict.NEEDS_INFORMATION;
        };
    }
}
