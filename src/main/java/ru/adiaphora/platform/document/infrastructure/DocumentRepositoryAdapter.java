package ru.adiaphora.platform.document.infrastructure;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.document.domain.DocumentRepository;
import ru.adiaphora.platform.document.domain.GeneratedDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapts Spring Data JPA to the {@link DocumentRepository} port. */
@Component
class DocumentRepositoryAdapter implements DocumentRepository {

    private final GeneratedDocumentJpaRepository jpa;

    DocumentRepositoryAdapter(GeneratedDocumentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public GeneratedDocument save(GeneratedDocument document) {
        GeneratedDocumentEntity entity = jpa.findById(document.id())
                .map(existing -> {
                    existing.applyFrom(document);
                    return existing;
                })
                .orElseGet(() -> GeneratedDocumentEntity.fromDomain(document));
        return jpa.save(entity).toDomain();
    }

    @Override
    public Optional<GeneratedDocument> findById(UUID documentId) {
        return jpa.findById(documentId).map(GeneratedDocumentEntity::toDomain);
    }

    @Override
    public List<GeneratedDocument> findByApplicationId(UUID applicationId) {
        return jpa.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
                .map(GeneratedDocumentEntity::toDomain)
                .toList();
    }
}
