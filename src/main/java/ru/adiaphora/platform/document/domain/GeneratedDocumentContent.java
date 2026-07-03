package ru.adiaphora.platform.document.domain;

/** The bytes produced by a {@link DocumentGenerator}, with their filename and content type. */
public record GeneratedDocumentContent(String filename, String contentType, byte[] content) {
}
