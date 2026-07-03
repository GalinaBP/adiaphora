package ru.adiaphora.platform.document.infrastructure;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.document.domain.DocumentTemplate;
import ru.adiaphora.platform.document.domain.DocumentTemplateRepository;

import java.util.Optional;

/** Placeholder template registry with a single default form. Replace with real templates later. */
@Component
class InMemoryDocumentTemplateRepository implements DocumentTemplateRepository {

    private static final DocumentTemplate DEFAULT = new DocumentTemplate(
            "bankruptcy-application-form",
            "Bankruptcy application form (placeholder)",
            "text/plain",
            "bankruptcy-application-{applicationId}.txt");

    @Override
    public DocumentTemplate defaultTemplate() {
        return DEFAULT;
    }

    @Override
    public Optional<DocumentTemplate> findByCode(String code) {
        return DEFAULT.code().equals(code) ? Optional.of(DEFAULT) : Optional.empty();
    }
}
