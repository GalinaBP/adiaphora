package ru.adiaphora.platform.document.domain;

/**
 * SPI for producing document content from a template and data. Implementations are replaceable (e.g.
 * a future DOCX generator); the current one is a placeholder that emits simple content.
 */
public interface DocumentGenerator {

    GeneratedDocumentContent generate(DocumentTemplate template, DocumentData data);
}
