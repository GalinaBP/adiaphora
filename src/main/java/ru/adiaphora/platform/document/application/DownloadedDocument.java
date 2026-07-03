package ru.adiaphora.platform.document.application;

/** A document's bytes plus the metadata needed to stream it back to the client. */
public record DownloadedDocument(String filename, String contentType, byte[] content) {
}
