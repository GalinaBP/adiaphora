package ru.adiaphora.platform.document.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Domain port for generated-document metadata. */
public interface DocumentRepository {

    GeneratedDocument save(GeneratedDocument document);

    Optional<GeneratedDocument> findById(UUID documentId);

    List<GeneratedDocument> findByApplicationId(UUID applicationId);
}
