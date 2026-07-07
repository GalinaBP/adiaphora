package ru.adiaphora.platform.estate.domain;

import java.util.UUID;

/**
 * A creditor claim against a bankruptcy case. Identity ({@code id}, {@code applicationId}) is fixed;
 * the editable attributes live in {@link CreditorDetails} and are replaced wholesale on update.
 */
public class Creditor {

    private final UUID id;
    private final UUID applicationId;
    private CreditorDetails details;

    private Creditor(UUID id, UUID applicationId, CreditorDetails details) {
        this.id = id;
        this.applicationId = applicationId;
        this.details = details;
    }

    public static Creditor create(UUID id, UUID applicationId, CreditorDetails details) {
        return new Creditor(id, applicationId, details);
    }

    public static Creditor rehydrate(UUID id, UUID applicationId, CreditorDetails details) {
        return new Creditor(id, applicationId, details);
    }

    public void update(CreditorDetails newDetails) {
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

    public CreditorDetails details() {
        return details;
    }
}
