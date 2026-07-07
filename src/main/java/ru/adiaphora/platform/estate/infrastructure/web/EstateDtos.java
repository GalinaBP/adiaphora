package ru.adiaphora.platform.estate.infrastructure.web;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ru.adiaphora.platform.estate.application.Saved;
import ru.adiaphora.platform.estate.domain.Asset;
import ru.adiaphora.platform.estate.domain.AssetDetails;
import ru.adiaphora.platform.estate.domain.AssetType;
import ru.adiaphora.platform.estate.domain.Creditor;
import ru.adiaphora.platform.estate.domain.CreditorDetails;
import ru.adiaphora.platform.estate.domain.CreditorType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Request/response payloads for the estate (creditor & asset) endpoints. */
public final class EstateDtos {

    private EstateDtos() {
    }

    private static String currencyOrDefault(String currency) {
        return (currency == null || currency.isBlank()) ? "RUB" : currency.trim().toUpperCase();
    }

    // ---- Creditors ----

    public record CreditorRequest(
            @NotBlank @Size(max = 300) String name,
            @NotNull CreditorType type,
            @Size(max = 12) String inn,
            @Size(max = 500) String claimBasis,
            @Size(max = 3) String currency,
            @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal totalAmount,
            @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal principalAmount,
            @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal interestAmount,
            @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal penaltyAmount,
            Boolean overdue,
            Boolean secured) {

        CreditorDetails toDetails() {
            return new CreditorDetails(name.trim(), type, inn, claimBasis, currencyOrDefault(currency),
                    totalAmount, principalAmount, interestAmount, penaltyAmount,
                    Boolean.TRUE.equals(overdue), Boolean.TRUE.equals(secured));
        }
    }

    public record CreditorResponse(
            UUID creditorId,
            UUID applicationId,
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
            boolean secured,
            boolean duplicateWarning) {

        static CreditorResponse of(Creditor c, boolean duplicateWarning) {
            CreditorDetails d = c.details();
            return new CreditorResponse(c.id(), c.applicationId(), d.name(), d.type(), d.inn(),
                    d.claimBasis(), d.currency(), d.totalAmount(), d.principalAmount(),
                    d.interestAmount(), d.penaltyAmount(), d.overdue(), d.secured(), duplicateWarning);
        }

        static CreditorResponse of(Creditor c) {
            return of(c, false);
        }

        static CreditorResponse of(Saved<Creditor> saved) {
            return of(saved.value(), saved.duplicateWarning());
        }
    }

    // ---- Assets ----

    public record AssetRequest(
            @NotNull AssetType type,
            @NotBlank @Size(max = 500) String description,
            @Size(max = 3) String currency,
            @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal estimatedValue,
            @Size(max = 16) String ownershipShare,
            @Size(max = 100) String registrationNumber,
            LocalDate acquiredOn,
            Boolean pledged,
            UUID pledgeCreditorId) {

        AssetDetails toDetails() {
            return new AssetDetails(type, description.trim(), currencyOrDefault(currency), estimatedValue,
                    ownershipShare, registrationNumber, acquiredOn, Boolean.TRUE.equals(pledged),
                    pledgeCreditorId);
        }
    }

    public record AssetResponse(
            UUID assetId,
            UUID applicationId,
            AssetType type,
            String description,
            String currency,
            BigDecimal estimatedValue,
            String ownershipShare,
            String registrationNumber,
            LocalDate acquiredOn,
            boolean pledged,
            UUID pledgeCreditorId,
            boolean duplicateWarning) {

        static AssetResponse of(Asset a, boolean duplicateWarning) {
            AssetDetails d = a.details();
            return new AssetResponse(a.id(), a.applicationId(), d.type(), d.description(), d.currency(),
                    d.estimatedValue(), d.ownershipShare(), d.registrationNumber(), d.acquiredOn(),
                    d.pledged(), d.pledgeCreditorId(), duplicateWarning);
        }

        static AssetResponse of(Asset a) {
            return of(a, false);
        }

        static AssetResponse of(Saved<Asset> saved) {
            return of(saved.value(), saved.duplicateWarning());
        }
    }
}
