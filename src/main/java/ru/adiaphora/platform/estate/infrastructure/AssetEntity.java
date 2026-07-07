package ru.adiaphora.platform.estate.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.common.persistence.BaseEntity;
import ru.adiaphora.platform.estate.domain.Asset;
import ru.adiaphora.platform.estate.domain.AssetDetails;
import ru.adiaphora.platform.estate.domain.AssetType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** JPA persistence view of the {@link Asset} aggregate ({@code assets} table, V011). */
@Entity
@Table(name = "assets")
class AssetEntity extends BaseEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "application_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 48)
    private AssetType assetType;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "estimated_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "ownership_share", length = 16)
    private String ownershipShare;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "acquired_on")
    private LocalDate acquiredOn;

    @Column(name = "pledged", nullable = false)
    private boolean pledged;

    @Column(name = "pledge_creditor_id", columnDefinition = "BINARY(16)")
    private UUID pledgeCreditorId;

    protected AssetEntity() {
        // for JPA
    }

    static AssetEntity fromDomain(Asset asset) {
        AssetEntity entity = new AssetEntity();
        entity.id = asset.id();
        entity.applicationId = asset.applicationId();
        entity.applyFrom(asset);
        return entity;
    }

    void applyFrom(Asset asset) {
        AssetDetails d = asset.details();
        this.assetType = d.type();
        this.description = d.description();
        this.currency = d.currency();
        this.estimatedValue = d.estimatedValue();
        this.ownershipShare = d.ownershipShare();
        this.registrationNumber = d.registrationNumber();
        this.acquiredOn = d.acquiredOn();
        this.pledged = d.pledged();
        this.pledgeCreditorId = d.pledgeCreditorId();
    }

    Asset toDomain() {
        return Asset.rehydrate(id, applicationId, new AssetDetails(
                assetType, description, currency, estimatedValue, ownershipShare,
                registrationNumber, acquiredOn, pledged, pledgeCreditorId));
    }
}
