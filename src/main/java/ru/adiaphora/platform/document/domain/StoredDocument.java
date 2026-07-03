package ru.adiaphora.platform.document.domain;

/** A reference to stored content: the opaque storage key and its size in bytes. */
public record StoredDocument(String storageKey, long sizeBytes) {
}
