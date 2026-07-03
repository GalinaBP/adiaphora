package ru.adiaphora.platform.document.application;

import ru.adiaphora.platform.document.domain.DocumentStatus;
import ru.adiaphora.platform.document.domain.GeneratedDocument;

import java.util.UUID;

/** Read model of a generated document's metadata (never includes the content bytes). */
public record DocumentView(
        UUID documentId,
        UUID applicationId,
        String templateCode,
        DocumentStatus status,
        String filename,
        String contentType,
        long sizeBytes
) {

    static DocumentView from(GeneratedDocument document) {
        return new DocumentView(document.id(), document.applicationId(), document.templateCode(),
                document.status(), document.filename(), document.contentType(), document.sizeBytes());
    }
}
