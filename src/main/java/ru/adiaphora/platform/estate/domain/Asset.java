package ru.adiaphora.platform.estate.domain;

import java.util.UUID;

/**
 * An asset in the debtor's estate. Identity ({@code id}, {@code applicationId}) is fixed; the editable
 * attributes live in {@link AssetDetails} and are replaced wholesale on update.
 */
public class Asset {

    private final UUID id;
    private final UUID applicationId;
    private AssetDetails details;

    private Asset(UUID id, UUID applicationId, AssetDetails details) {
        this.id = id;
        this.applicationId = applicationId;
        this.details = details;
    }

    public static Asset create(UUID id, UUID applicationId, AssetDetails details) {
        return new Asset(id, applicationId, details);
    }

    public static Asset rehydrate(UUID id, UUID applicationId, AssetDetails details) {
        return new Asset(id, applicationId, details);
    }

    public void update(AssetDetails newDetails) {
        this.details = newDetails;
    }

    public boolean belongsTo(UUID applicationId) {
        return this.applicationId.equals(applicationId);
    }

    public UUID id() {
        return id;
    }

    public UUID applicationId() {
        return applicationId;
    }

    public AssetDetails details() {
        return details;
    }
}
