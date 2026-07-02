package ru.adiaphora.platform.audit.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * An immutable audit record. Created once via {@link #builder()} and never modified. Sensitive values
 * (passwords, tokens, full questionnaire/document contents, free-text answers) must never be placed in
 * {@code metadata}; keep it to small, non-sensitive context such as codes and enum names.
 */
public record AuditEvent(
        UUID id,
        Instant occurredAt,
        UUID actorId,
        String actorRole,
        AuditAction action,
        String objectType,
        UUID objectId,
        UUID applicationId,
        AuditResult result,
        String correlationId,
        String metadata
) {

    public static Builder builder() {
        return new Builder();
    }

    /** Mutable builder for assembling an {@link AuditEvent}; the resulting record is immutable. */
    public static final class Builder {
        private UUID actorId;
        private String actorRole;
        private AuditAction action;
        private String objectType;
        private UUID objectId;
        private UUID applicationId;
        private AuditResult result = AuditResult.SUCCESS;
        private String metadata;

        public Builder actor(UUID actorId, String actorRole) {
            this.actorId = actorId;
            this.actorRole = actorRole;
            return this;
        }

        public Builder action(AuditAction action) {
            this.action = action;
            return this;
        }

        public Builder object(String objectType, UUID objectId) {
            this.objectType = objectType;
            this.objectId = objectId;
            return this;
        }

        public Builder applicationId(UUID applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public Builder result(AuditResult result) {
            this.result = result;
            return this;
        }

        public Builder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        /** Builds the record, supplying the identity, timestamp, and correlation id. */
        public AuditEvent build(UUID id, Instant occurredAt, String correlationId) {
            return new AuditEvent(id, occurredAt, actorId, actorRole, action, objectType, objectId,
                    applicationId, result, correlationId, metadata);
        }
    }
}
