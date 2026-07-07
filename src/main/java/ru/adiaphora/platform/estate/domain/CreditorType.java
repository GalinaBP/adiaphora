package ru.adiaphora.platform.estate.domain;

/**
 * Kind of creditor. Placeholder taxonomy pending lawyer review — the real set of creditor categories
 * for Russian personal bankruptcy must be confirmed.
 */
public enum CreditorType {
    BANK,
    MICROFINANCE,
    INDIVIDUAL,
    TAX_AUTHORITY,
    UTILITY,
    OTHER
}
