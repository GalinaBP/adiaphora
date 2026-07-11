package ru.adiaphora.platform.rules.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleEngine;
import ru.adiaphora.platform.rules.domain.rules.BailiffsClosedGroundRule;
import ru.adiaphora.platform.rules.domain.rules.DebtAmountMissingRule;
import ru.adiaphora.platform.rules.domain.rules.MfcLowerBoundRule;
import ru.adiaphora.platform.rules.domain.rules.MfcUpperBoundRule;
import ru.adiaphora.platform.rules.domain.rules.MortgageManualReviewRule;
import ru.adiaphora.platform.rules.domain.rules.NoStatutoryGroundRule;
import ru.adiaphora.platform.rules.domain.rules.OldDebtGroundRule;
import ru.adiaphora.platform.rules.domain.rules.PriorBankruptcyFiveYearRule;
import ru.adiaphora.platform.rules.domain.rules.RecentPropertyTransactionManualReviewRule;
import ru.adiaphora.platform.rules.domain.rules.StatutoryGroundsMissingRule;
import ru.adiaphora.platform.rules.domain.rules.VulnerableCategoryGroundRule;

import java.time.Clock;
import java.util.List;

/**
 * Registers the (framework-free) rules and the engine as beans. Adding a rule is a one-line change
 * here; the engine receives all {@link BankruptcyRule} beans.
 */
@Configuration
class RulesConfig {

    @Bean
    BankruptcyRule debtAmountMissingRule() {
        return new DebtAmountMissingRule();
    }

    @Bean
    BankruptcyRule statutoryGroundsMissingRule() {
        return new StatutoryGroundsMissingRule();
    }

    @Bean
    BankruptcyRule mfcLowerBoundRule() {
        return new MfcLowerBoundRule();
    }

    @Bean
    BankruptcyRule mfcUpperBoundRule() {
        return new MfcUpperBoundRule();
    }

    @Bean
    BankruptcyRule priorBankruptcyFiveYearRule(Clock clock) {
        return new PriorBankruptcyFiveYearRule(clock);
    }

    @Bean
    BankruptcyRule noStatutoryGroundRule() {
        return new NoStatutoryGroundRule();
    }

    @Bean
    BankruptcyRule bailiffsClosedGroundRule() {
        return new BailiffsClosedGroundRule();
    }

    @Bean
    BankruptcyRule vulnerableCategoryGroundRule() {
        return new VulnerableCategoryGroundRule();
    }

    @Bean
    BankruptcyRule oldDebtGroundRule() {
        return new OldDebtGroundRule();
    }

    @Bean
    BankruptcyRule mortgageManualReviewRule() {
        return new MortgageManualReviewRule();
    }

    @Bean
    BankruptcyRule recentPropertyTransactionManualReviewRule() {
        return new RecentPropertyTransactionManualReviewRule();
    }

    @Bean
    RuleEngine ruleEngine(List<BankruptcyRule> rules) {
        return new RuleEngine(rules);
    }
}
