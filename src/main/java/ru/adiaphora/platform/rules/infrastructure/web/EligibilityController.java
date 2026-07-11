package ru.adiaphora.platform.rules.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.adiaphora.platform.common.web.ApiPaths;
import ru.adiaphora.platform.rules.application.EstimateEligibilityUseCase;
import ru.adiaphora.platform.rules.application.EstimateEligibilityUseCase.Estimate;
import ru.adiaphora.platform.rules.domain.RuleInputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Anonymous MFC-eligibility check for the public home page (permitted without authentication in the
 * security matrix). Implements the staged eligibility flow (127-ФЗ, ст. 223.2): debt amount, the
 * 5-year repeat-filing bar, multi-select statutory grounds and their follow-up blocks. Unanswered
 * questions surface as a NEEDS_INFORMATION verdict. Every check session is persisted (without
 * personal identifiers) for audit; the response carries the ruleset version and the legal-basis
 * citations behind the decision.
 */
@Tag(name = "Eligibility")
@RestController
class EligibilityController {

    private static final String TRI_STATE = "yes|no|not_sure";
    private static final String TRI_STATE_MESSAGE = "допустимые значения: yes, no, not_sure";

    private final EstimateEligibilityUseCase estimate;

    EligibilityController(EstimateEligibilityUseCase estimate) {
        this.estimate = estimate;
    }

    @Operation(summary = "Anonymous, non-binding MFC-eligibility check "
            + "(session persisted without personal identifiers)")
    @PostMapping(ApiPaths.API_V1 + "/eligibility/estimate")
    EstimateResponse estimate(@Valid @RequestBody EstimateRequest request) {
        return EstimateResponse.from(estimate.estimate(request.toAnswers()));
    }

    record EstimateRequest(
            @DecimalMin(value = "0", message = "не может быть отрицательной")
            @Digits(integer = 17, fraction = 2, message = "сумма может иметь не более 2 знаков после запятой")
            BigDecimal totalDebtAmount,
            Boolean previousBankruptcy,
            LocalDate previousBankruptcyEndedOn,
            List<@Pattern(regexp = "enforcement_ended|pensioner|child_benefit|svo_participant"
                    + "|long_enforcement|none",
                    message = "допустимые значения: enforcement_ended, pensioner, child_benefit, "
                            + "svo_participant, long_enforcement, none") String> mfcStatutoryGrounds,
            @Pattern(regexp = TRI_STATE, message = TRI_STATE_MESSAGE) String bailiffsCaseClosedNoNew,
            @Pattern(regexp = TRI_STATE, message = TRI_STATE_MESSAGE) String childBenefitConfirmed,
            @Pattern(regexp = TRI_STATE, message = TRI_STATE_MESSAGE) String writUnpaidOverOneYear,
            @Pattern(regexp = TRI_STATE, message = TRI_STATE_MESSAGE) String ownsSellableProperty,
            @Pattern(regexp = TRI_STATE, message = TRI_STATE_MESSAGE) String writIssuedOverSevenYears) {

        Map<String, String> toAnswers() {
            Map<String, String> answers = new LinkedHashMap<>();
            if (totalDebtAmount != null) {
                answers.put(RuleInputs.TOTAL_DEBT_AMOUNT, totalDebtAmount.toPlainString());
            }
            if (previousBankruptcy != null) {
                answers.put(RuleInputs.PREVIOUS_BANKRUPTCY, previousBankruptcy.toString());
            }
            if (previousBankruptcyEndedOn != null) {
                answers.put(RuleInputs.PREVIOUS_BANKRUPTCY_ENDED_ON, previousBankruptcyEndedOn.toString());
            }
            if (mfcStatutoryGrounds != null && !mfcStatutoryGrounds.isEmpty()) {
                answers.put(RuleInputs.MFC_STATUTORY_GROUNDS, String.join(",", mfcStatutoryGrounds));
            }
            putIfPresent(answers, RuleInputs.BAILIFFS_CASE_CLOSED_NO_NEW, bailiffsCaseClosedNoNew);
            putIfPresent(answers, RuleInputs.CHILD_BENEFIT_CONFIRMED, childBenefitConfirmed);
            putIfPresent(answers, RuleInputs.WRIT_UNPAID_OVER_ONE_YEAR, writUnpaidOverOneYear);
            putIfPresent(answers, RuleInputs.OWNS_SELLABLE_PROPERTY, ownsSellableProperty);
            putIfPresent(answers, RuleInputs.WRIT_ISSUED_OVER_SEVEN_YEARS, writIssuedOverSevenYears);
            return answers;
        }

        private static void putIfPresent(Map<String, String> answers, String code, String value) {
            if (value != null) {
                answers.put(code, value);
            }
        }
    }

    record QualifyingGroundResponse(String code, String message, String legalBasis) {
    }

    record EstimateResponse(
            String verdict,
            String route,
            List<String> messages,
            List<QualifyingGroundResponse> qualifyingGrounds,
            List<String> citations,
            List<String> missingInformation,
            String rulesetVersion) {

        static EstimateResponse from(Estimate estimate) {
            return new EstimateResponse(
                    estimate.verdict().name(),
                    estimate.route().name(),
                    estimate.messages(),
                    estimate.qualifyingGrounds().stream()
                            .map(g -> new QualifyingGroundResponse(g.ruleCode(), g.message(), g.legalBasis()))
                            .toList(),
                    estimate.citations(),
                    estimate.missingInformation(),
                    estimate.rulesetVersion());
        }
    }
}
