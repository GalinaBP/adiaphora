package ru.adiaphora.platform.rules.domain;

import java.math.BigDecimal;
import java.time.Period;

/**
 * Shared constants for the MFC (extrajudicial) eligibility ruleset: the question codes rules read,
 * the debt bounds, the repeat-filing bar, and the legal-basis citations (127-ФЗ, ст. 223.2).
 *
 * <p><strong>Wording pending legal review.</strong> Thresholds and the five statutory grounds follow
 * the approved eligibility-flow ticket (ст. 223.2–223.6 127-ФЗ); citations are displayed to users and
 * must be confirmed by a lawyer before production use.
 */
public final class RuleInputs {

    public static final String RULESET_VERSION = "ruleset-2026.07-MFC-223.2";

    // Question codes (must match the seeded questionnaire definition).
    public static final String TOTAL_DEBT_AMOUNT = "totalDebtAmount";
    public static final String HAS_REGULAR_INCOME = "hasRegularIncome";
    public static final String OWNS_MORTGAGED_HOME = "ownsMortgagedHome";
    public static final String PREVIOUS_BANKRUPTCY = "previousBankruptcy";
    public static final String PREVIOUS_BANKRUPTCY_ENDED_ON = "previousBankruptcyEndedOn";
    public static final String RECENT_PROPERTY_TRANSACTION = "recentPropertyTransaction";
    public static final String MFC_STATUTORY_GROUNDS = "mfcStatutoryGrounds";

    // Stage 3.2 follow-up questions (tri-state answers: yes / no / not_sure).
    public static final String BAILIFFS_CASE_CLOSED_NO_NEW = "bailiffsCaseClosedNoNew";
    public static final String CHILD_BENEFIT_CONFIRMED = "childBenefitConfirmed";
    public static final String WRIT_UNPAID_OVER_ONE_YEAR = "writUnpaidOverOneYear";
    public static final String OWNS_SELLABLE_PROPERTY = "ownsSellableProperty";
    public static final String WRIT_ISSUED_OVER_SEVEN_YEARS = "writIssuedOverSevenYears";

    // Tri-state answer values for the stage 3.2 questions.
    public static final String VALUE_YES = "yes";
    public static final String VALUE_NO = "no";
    public static final String VALUE_NOT_SURE = "not_sure";

    // Answer values for the statutory-grounds question (multi-select): the five categories
    // recognised for the extrajudicial (MFC) procedure plus "none of these".
    public static final String GROUND_ENFORCEMENT_ENDED = "enforcement_ended";
    public static final String GROUND_PENSIONER = "pensioner";
    public static final String GROUND_CHILD_BENEFIT = "child_benefit";
    public static final String GROUND_SVO_PARTICIPANT = "svo_participant";
    public static final String GROUND_LONG_ENFORCEMENT = "long_enforcement";
    public static final String GROUND_NONE = "none";

    // MFC (extrajudicial) debt bounds in RUB, inclusive (п. 1 ст. 223.2 127-ФЗ).
    public static final BigDecimal MFC_LOWER_BOUND = new BigDecimal("25000");
    public static final BigDecimal MFC_UPPER_BOUND = new BigDecimal("1000000");

    // Repeat extrajudicial filing is barred for 5 years from the end of the previous procedure
    // (п. 8 ст. 223.2 127-ФЗ).
    public static final Period REPEAT_FILING_BAR = Period.ofYears(5);

    // Legal-basis citations, displayed on result screens and persisted with findings.
    public static final String BASIS_DEBT_BOUNDS = "п. 1 ст. 223.2 Закона № 127-ФЗ";
    public static final String BASIS_STATUTORY_GROUNDS = "п. 1 ст. 223.2 Закона № 127-ФЗ";
    public static final String BASIS_REPEAT_FILING = "п. 8 ст. 223.2 Закона № 127-ФЗ";
    public static final String BASIS_BAILIFFS_CLOSED = "пп. 1 п. 1 ст. 223.2 Закона № 127-ФЗ";
    public static final String BASIS_PENSIONER_SVO = "пп. 2 п. 1 ст. 223.2 Закона № 127-ФЗ";
    public static final String BASIS_CHILD_BENEFIT = "пп. 3 п. 1 ст. 223.2 Закона № 127-ФЗ";
    public static final String BASIS_OLD_DEBT = "пп. 4 п. 1 ст. 223.2 Закона № 127-ФЗ";

    private RuleInputs() {
    }
}
