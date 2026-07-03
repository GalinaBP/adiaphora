package ru.adiaphora.platform.document.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.document.domain.DocumentRepository;
import ru.adiaphora.platform.document.domain.GeneratedDocument;

import java.util.List;
import java.util.UUID;

/** Read access to document metadata, authorization-checked against the owning application. */
@Service
public class DocumentQueries {

    private final DocumentAccess access;
    private final DocumentRepository documents;

    public DocumentQueries(DocumentAccess access, DocumentRepository documents) {
        this.access = access;
        this.documents = documents;
    }

    @Transactional(readOnly = true)
    public List<DocumentView> listForApplication(UUID applicationId) {
        access.requireAccess(applicationId);
        return documents.findByApplicationId(applicationId).stream().map(DocumentView::from).toList();
    }

    @Transactional(readOnly = true)
    public DocumentView get(UUID documentId) {
        GeneratedDocument document = load(documentId);
        access.requireAccess(document.applicationId());
        return DocumentView.from(document);
    }

    private GeneratedDocument load(UUID documentId) {
        return documents.findById(documentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Document", documentId));
    }
}
