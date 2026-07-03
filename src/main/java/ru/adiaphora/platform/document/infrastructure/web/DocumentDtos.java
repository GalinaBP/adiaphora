package ru.adiaphora.platform.document.infrastructure.web;

import jakarta.validation.constraints.Size;
import ru.adiaphora.platform.document.application.DocumentView;

import java.util.UUID;

/** Request/response payloads for the document endpoints. */
public final class DocumentDtos {

    private DocumentDtos() {
    }

    public record CreateDocumentRequest(@Size(max = 100) String templateCode) {
    }

    public record DocumentResponse(
            UUID documentId,
            UUID applicationId,
            String templateCode,
            String status,
            String filename,
            String contentType,
            long sizeBytes) {

        public static DocumentResponse from(DocumentView view) {
            return new DocumentResponse(view.documentId(), view.applicationId(), view.templateCode(),
                    view.status().name(), view.filename(), view.contentType(), view.sizeBytes());
        }
    }
}
