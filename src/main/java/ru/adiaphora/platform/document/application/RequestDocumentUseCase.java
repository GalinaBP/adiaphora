package ru.adiaphora.platform.document.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.application.api.ApplicationCommandService;
import ru.adiaphora.platform.application.api.ApplicationView;
import ru.adiaphora.platform.application.api.BankruptcyApplicationStatus;
import ru.adiaphora.platform.common.error.DocumentGenerationException;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.document.api.DocumentRequestedEvent;
import ru.adiaphora.platform.document.domain.DocumentData;
import ru.adiaphora.platform.document.domain.DocumentGenerator;
import ru.adiaphora.platform.document.domain.DocumentRepository;
import ru.adiaphora.platform.document.domain.DocumentStorage;
import ru.adiaphora.platform.document.domain.DocumentTemplate;
import ru.adiaphora.platform.document.domain.DocumentTemplateRepository;
import ru.adiaphora.platform.document.domain.GeneratedDocument;
import ru.adiaphora.platform.document.domain.GeneratedDocumentContent;
import ru.adiaphora.platform.document.domain.StoredDocument;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

/**
 * Requests generation of a document: resolves the template, generates content (placeholder), stores
 * it, persists metadata, and marks it ready for download. When the case is approved for document
 * generation, it advances to {@code DOCUMENTS_READY}.
 */
@Service
public class RequestDocumentUseCase {

    private final DocumentAccess access;
    private final DocumentTemplateRepository templates;
    private final DocumentGenerator generator;
    private final DocumentStorage storage;
    private final DocumentRepository documents;
    private final ApplicationCommandService applicationCommands;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RequestDocumentUseCase(DocumentAccess access, DocumentTemplateRepository templates,
                                  DocumentGenerator generator, DocumentStorage storage,
                                  DocumentRepository documents, ApplicationCommandService applicationCommands,
                                  ApplicationEventPublisher events, Clock clock) {
        this.access = access;
        this.templates = templates;
        this.generator = generator;
        this.storage = storage;
        this.documents = documents;
        this.applicationCommands = applicationCommands;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public DocumentView request(UUID applicationId, String templateCode) {
        ApplicationView application = access.requireAccess(applicationId);
        UUID actorId = access.currentUserId();

        DocumentTemplate template = resolveTemplate(templateCode);
        GeneratedDocument document = GeneratedDocument.request(UUID.randomUUID(), applicationId,
                template.code());
        document.markGenerating();

        try {
            GeneratedDocumentContent content = generator.generate(template,
                    new DocumentData(applicationId, Map.of("applicationId", applicationId.toString())));
            StoredDocument stored = storage.store(content.filename(), content.contentType(),
                    content.content());
            document.markStored(content.filename(), content.contentType(), stored.storageKey(),
                    stored.sizeBytes());
        } catch (RuntimeException ex) {
            throw new DocumentGenerationException("Failed to generate document");
        }

        documents.save(document);
        events.publishEvent(new DocumentRequestedEvent(document.id(), applicationId, actorId,
                clock.instant()));

        if (application.status() == BankruptcyApplicationStatus.APPROVED_FOR_DOCUMENT_GENERATION) {
            applicationCommands.transitionStatus(applicationId,
                    BankruptcyApplicationStatus.DOCUMENTS_READY, "documents generated", actorId);
        }
        return DocumentView.from(document);
    }

    private DocumentTemplate resolveTemplate(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            return templates.defaultTemplate();
        }
        return templates.findByCode(templateCode)
                .orElseThrow(() -> ResourceNotFoundException.of("DocumentTemplate", templateCode));
    }
}
