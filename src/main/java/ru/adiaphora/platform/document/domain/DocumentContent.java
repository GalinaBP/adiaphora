package ru.adiaphora.platform.document.domain;

/** The bytes loaded back from storage for download, with their filename and content type. */
public record DocumentContent(String filename, String contentType, byte[] content) {
}
