package ru.adiaphora.platform.document.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.common.error.InvalidApplicationStateException;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.document.api.DocumentDownloadedEvent;
import ru.adiaphora.platform.document.domain.DocumentContent;
import ru.adiaphora.platform.document.domain.DocumentRepository;
import ru.adiaphora.platform.document.domain.DocumentStorage;
import ru.adiaphora.platform.document.domain.GeneratedDocument;

import java.time.Clock;
import java.util.UUID;

/** Downloads a document's bytes after checking ownership and that it is ready for download. */
@Service
public class DownloadDocumentUseCase {

    private final DocumentAccess access;
    private final DocumentRepository documents;
    private final DocumentStorage storage;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public DownloadDocumentUseCase(DocumentAccess access, DocumentRepository documents,
                                   DocumentStorage storage, ApplicationEventPublisher events, Clock clock) {
        this.access = access;
        this.documents = documents;
        this.storage = storage;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public DownloadedDocument download(UUID documentId) {
        GeneratedDocument document = documents.findById(documentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Document", documentId));
        access.requireAccess(document.applicationId());

        if (!document.isDownloadable()) {
            throw new InvalidApplicationStateException("Document is not ready for download");
        }

        DocumentContent content = storage.load(document.storageKey());
        events.publishEvent(new DocumentDownloadedEvent(document.id(), document.applicationId(),
                access.currentUserId(), clock.instant()));
        return new DownloadedDocument(content.filename(), content.contentType(), content.content());
    }
}
