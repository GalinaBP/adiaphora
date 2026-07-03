package ru.adiaphora.platform.document.domain;

import java.util.UUID;

/**
 * Generated-document aggregate: tracks a generation request and its lifecycle/metadata. The bytes
 * themselves live in {@link DocumentStorage}; this aggregate only holds the storage key and metadata.
 */
public class GeneratedDocument {

    private final UUID id;
    private final UUID applicationId;
    private final String templateCode;
    private DocumentStatus status;
    private String filename;
    private String contentType;
    private String storageKey;
    private long sizeBytes;

    private GeneratedDocument(UUID id, UUID applicationId, String templateCode, DocumentStatus status) {
        this.id = id;
        this.applicationId = applicationId;
        this.templateCode = templateCode;
        this.status = status;
    }

    public static GeneratedDocument request(UUID id, UUID applicationId, String templateCode) {
        return new GeneratedDocument(id, applicationId, templateCode, DocumentStatus.REQUESTED);
    }

    public static GeneratedDocument rehydrate(UUID id, UUID applicationId, String templateCode,
                                              DocumentStatus status, String filename, String contentType,
                                              String storageKey, long sizeBytes) {
        GeneratedDocument document = new GeneratedDocument(id, applicationId, templateCode, status);
        document.filename = filename;
        document.contentType = contentType;
        document.storageKey = storageKey;
        document.sizeBytes = sizeBytes;
        return document;
    }

    public void markGenerating() {
        this.status = DocumentStatus.GENERATING;
    }

    /** Records successful generation + storage and marks the document ready for download. */
    public void markStored(String filename, String contentType, String storageKey, long sizeBytes) {
        this.filename = filename;
        this.contentType = contentType;
        this.storageKey = storageKey;
        this.sizeBytes = sizeBytes;
        this.status = DocumentStatus.READY_FOR_DOWNLOAD;
    }

    public void markFailed() {
        this.status = DocumentStatus.FAILED;
    }

    public boolean isDownloadable() {
        return (status == DocumentStatus.READY_FOR_DOWNLOAD || status == DocumentStatus.APPROVED)
                && storageKey != null;
    }

    public UUID id() {
        return id;
    }

    public UUID applicationId() {
        return applicationId;
    }

    public String templateCode() {
        return templateCode;
    }

    public DocumentStatus status() {
        return status;
    }

    public String filename() {
        return filename;
    }

    public String contentType() {
        return contentType;
    }

    public String storageKey() {
        return storageKey;
    }

    public long sizeBytes() {
        return sizeBytes;
    }
}
