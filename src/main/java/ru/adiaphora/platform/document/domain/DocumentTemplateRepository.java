package ru.adiaphora.platform.document.domain;

import java.util.Optional;

/** Registry of available document templates. */
public interface DocumentTemplateRepository {

    /** The default template used when a request does not specify one. */
    DocumentTemplate defaultTemplate();

    Optional<DocumentTemplate> findByCode(String code);
}
