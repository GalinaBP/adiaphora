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
    public static final String MFC_STATUTORY_GROUND = "mfcStatutoryGround";

    // Answer values for the statutory-ground question: the five categories recognised for the
    // extrajudicial (MFC) procedure (127-ФЗ, ст. 223.2), plus "none of these" and "unknown —
    // the enforcement paperwork still has to be checked".
    public static final String GROUND_ENFORCEMENT_ENDED = "enforcement_ended";
    public static final String GROUND_PENSIONER = "pensioner";
    public static final String GROUND_CHILD_BENEFIT = "child_benefit";
    public static final String GROUND_SVO_PARTICIPANT = "svo_participant";
    public static final String GROUND_LONG_ENFORCEMENT = "long_enforcement";
    public static final String GROUND_NONE = "none";
    public static final String GROUND_UNKNOWN = "unknown";

    // Placeholder MFC (extrajudicial) debt bounds in RUB.
    public static final BigDecimal MFC_LOWER_BOUND = new BigDecimal("25000");
    public static final BigDecimal MFC_UPPER_BOUND = new BigDecimal("1000000");

    private RuleInputs() {
    }
}
