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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Anonymous MFC-eligibility estimate for the public home page (permitted without authentication in
 * the security matrix). Every field is optional — unanswered questions surface as a
 * NEEDS_INFORMATION verdict, exactly as the authenticated evaluation would report them. Nothing is
 * persisted; the response carries the ruleset version so estimates are traceable.
 */
@Tag(name = "Eligibility")
@RestController
class EligibilityController {

    private final EstimateEligibilityUseCase estimate;

    EligibilityController(EstimateEligibilityUseCase estimate) {
        this.estimate = estimate;
    }

    @Operation(summary = "Anonymous, non-binding MFC-eligibility estimate (nothing is persisted)")
    @PostMapping(ApiPaths.API_V1 + "/eligibility/estimate")
    EstimateResponse estimate(@Valid @RequestBody EstimateRequest request) {
        return EstimateResponse.from(estimate.estimate(request.toAnswers()));
    }

    record EstimateRequest(
            @DecimalMin(value = "0", message = "не может быть отрицательной")
            @Digits(integer = 17, fraction = 2, message = "сумма может иметь не более 2 знаков после запятой")
            BigDecimal totalDebtAmount,
            Boolean hasRegularIncome,
            Boolean ownsMortgagedHome,
            Boolean previousBankruptcy,
            @Pattern(regexp = "none|sold|gifted", message = "допустимые значения: none, sold, gifted")
            String recentPropertyTransaction) {

        Map<String, String> toAnswers() {
            Map<String, String> answers = new LinkedHashMap<>();
            if (totalDebtAmount != null) {
                answers.put(RuleInputs.TOTAL_DEBT_AMOUNT, totalDebtAmount.toPlainString());
            }
            if (hasRegularIncome != null) {
                answers.put(RuleInputs.HAS_REGULAR_INCOME, hasRegularIncome.toString());
            }
            if (ownsMortgagedHome != null) {
                answers.put(RuleInputs.OWNS_MORTGAGED_HOME, ownsMortgagedHome.toString());
            }
            if (previousBankruptcy != null) {
                answers.put(RuleInputs.PREVIOUS_BANKRUPTCY, previousBankruptcy.toString());
            }
            if (recentPropertyTransaction != null) {
                answers.put(RuleInputs.RECENT_PROPERTY_TRANSACTION, recentPropertyTransaction);
            }
            return answers;
        }
    }

    record EstimateResponse(
            String verdict,
            String route,
            List<String> messages,
            List<String> missingInformation,
            String rulesetVersion) {

        static EstimateResponse from(Estimate estimate) {
            return new EstimateResponse(
                    estimate.verdict().name(),
                    estimate.route().name(),
                    estimate.messages(),
                    estimate.missingInformation(),
                    estimate.rulesetVersion());
        }
    }
}
