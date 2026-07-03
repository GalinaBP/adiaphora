package ru.adiaphora.platform.document.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.common.persistence.BaseEntity;
import ru.adiaphora.platform.document.domain.DocumentStatus;
import ru.adiaphora.platform.document.domain.GeneratedDocument;

import java.util.UUID;

/** JPA view of {@link GeneratedDocument} metadata. Content bytes live in {@code DocumentStorage}. */
@Entity
@Table(name = "generated_documents")
class GeneratedDocumentEntity extends BaseEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "application_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID applicationId;

    @Column(name = "template_code", nullable = false, length = 100)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DocumentStatus status;

    @Column(name = "filename", length = 300)
    private String filename;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "storage_key", length = 200)
    private String storageKey;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    protected GeneratedDocumentEntity() {
    }

    static GeneratedDocumentEntity fromDomain(GeneratedDocument document) {
        GeneratedDocumentEntity entity = new GeneratedDocumentEntity();
        entity.id = document.id();
        entity.applicationId = document.applicationId();
        entity.templateCode = document.templateCode();
        entity.applyFrom(document);
        return entity;
    }

    void applyFrom(GeneratedDocument document) {
        this.status = document.status();
        this.filename = document.filename();
        this.contentType = document.contentType();
        this.storageKey = document.storageKey();
        this.sizeBytes = document.sizeBytes();
    }

    GeneratedDocument toDomain() {
        return GeneratedDocument.rehydrate(id, applicationId, templateCode, status, filename, contentType,
                storageKey, sizeBytes);
    }
}
