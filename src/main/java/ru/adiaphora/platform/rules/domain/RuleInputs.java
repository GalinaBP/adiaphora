package ru.adiaphora.platform.rules.domain;

import java.math.BigDecimal;

/**
 * Shared constants for the placeholder ruleset: the question codes rules read and the MFC
 * (extrajudicial) debt bounds.
 *
 * <p><strong>Placeholder values pending legal review.</strong> The MFC bounds here are illustrative
 * and must be confirmed against current law before production use.
 */
public final class RuleInputs {

    public static final String RULESET_VERSION = "ruleset-2026.07-PLACEHOLDER";

    // Question codes (must match the seeded questionnaire definition).
    public static final String TOTAL_DEBT_AMOUNT = "totalDebtAmount";
    public static final String HAS_REGULAR_INCOME = "hasRegularIncome";
    public static final String OWNS_MORTGAGED_HOME = "ownsMortgagedHome";
    public static final String PREVIOUS_BANKRUPTCY = "previousBankruptcy";
    public static final String RECENT_PROPERTY_TRANSACTION = "recentPropertyTransaction";

    // Placeholder MFC (extrajudicial) debt bounds in RUB.
    public static final BigDecimal MFC_LOWER_BOUND = new BigDecimal("25000");
    public static final BigDecimal MFC_UPPER_BOUND = new BigDecimal("1000000");

    private RuleInputs() {
    }
}
