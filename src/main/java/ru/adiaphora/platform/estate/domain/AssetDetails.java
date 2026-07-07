package ru.adiaphora.platform.estate.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The editable attributes of an asset. {@code pledgeCreditorId} optionally links a pledged asset to
 * the secured creditor holding the collateral.
 */
public record AssetDetails(
        AssetType type,
        String description,
        String currency,
        BigDecimal estimatedValue,
        String ownershipShare,
        String registrationNumber,
        LocalDate acquiredOn,
        boolean pledged,
        UUID pledgeCreditorId
) {
    /** Normalised key used for duplicate detection: same type and description (case-insensitive). */
    public String duplicateKey() {
        String normalisedDescription = description == null ? "" : description.strip().toLowerCase();
        return type + "|" + normalisedDescription;
    }
}
