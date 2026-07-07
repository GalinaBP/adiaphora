package ru.adiaphora.platform.estate.domain;

import java.math.BigDecimal;

/**
 * The editable attributes of a creditor, carried together so {@code create}/{@code update} share one
 * shape. Amounts are {@link BigDecimal} (never floating point); {@code currency} is an ISO-4217 code.
 */
public record CreditorDetails(
        String name,
        CreditorType type,
        String inn,
        String claimBasis,
        String currency,
        BigDecimal totalAmount,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal penaltyAmount,
        boolean overdue,
        boolean secured
) {
    /** Normalised key used for duplicate detection: same creditor name (case-insensitive) and INN. */
    public String duplicateKey() {
        String normalisedName = name == null ? "" : name.strip().toLowerCase();
        String normalisedInn = inn == null ? "" : inn.strip();
        return normalisedName + "|" + normalisedInn;
    }
}
