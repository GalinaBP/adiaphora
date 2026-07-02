package ru.adiaphora.platform.rules.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleEngine;
import ru.adiaphora.platform.rules.domain.rules.DebtAmountMissingRule;
import ru.adiaphora.platform.rules.domain.rules.MfcLowerBoundRule;
import ru.adiaphora.platform.rules.domain.rules.MfcUpperBoundRule;
import ru.adiaphora.platform.rules.domain.rules.MortgageManualReviewRule;
import ru.adiaphora.platform.rules.domain.rules.PaymentAbilityMissingRule;
import ru.adiaphora.platform.rules.domain.rules.PreviousBankruptcyManualReviewRule;
import ru.adiaphora.platform.rules.domain.rules.RecentPropertyTransactionManualReviewRule;

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
    BankruptcyRule paymentAbilityMissingRule() {
        return new PaymentAbilityMissingRule();
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
    BankruptcyRule mortgageManualReviewRule() {
        return new MortgageManualReviewRule();
    }

    @Bean
    BankruptcyRule previousBankruptcyManualReviewRule() {
        return new PreviousBankruptcyManualReviewRule();
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
