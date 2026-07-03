package ru.adiaphora.platform.document.domain;

/** Metadata for a document template. Content type and filename pattern drive generation/storage. */
public record DocumentTemplate(String code, String name, String contentType, String filenamePattern) {
}
