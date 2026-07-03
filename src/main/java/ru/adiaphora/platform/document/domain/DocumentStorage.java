package ru.adiaphora.platform.document.domain;

/**
 * Storage abstraction for generated documents. The implementation is fully replaceable through this
 * interface (dev filesystem/in-memory now; object storage later) without touching callers.
 */
public interface DocumentStorage {

    StoredDocument store(String filename, String contentType, byte[] content);

    DocumentContent load(String storageKey);

    void delete(String storageKey);
}
