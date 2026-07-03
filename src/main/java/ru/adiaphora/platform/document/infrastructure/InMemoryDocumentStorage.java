package ru.adiaphora.platform.document.infrastructure;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.document.domain.DocumentContent;
import ru.adiaphora.platform.document.domain.DocumentStorage;
import ru.adiaphora.platform.document.domain.StoredDocument;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development-only {@link DocumentStorage} that keeps bytes in memory.
 *
 * <p><strong>NOT FOR PRODUCTION:</strong> content is lost on restart and is not shared across
 * instances. It exists so the flow works end-to-end locally; replace it with a real object-store
 * adapter (implementing the same interface) for production.
 */
@Component
class InMemoryDocumentStorage implements DocumentStorage {

    private record Blob(String filename, String contentType, byte[] content) {
    }

    private final Map<String, Blob> store = new ConcurrentHashMap<>();

    @Override
    public StoredDocument store(String filename, String contentType, byte[] content) {
        String storageKey = UUID.randomUUID().toString();
        store.put(storageKey, new Blob(filename, contentType, content.clone()));
        return new StoredDocument(storageKey, content.length);
    }

    @Override
    public DocumentContent load(String storageKey) {
        Blob blob = store.get(storageKey);
        if (blob == null) {
            throw new ResourceNotFoundException("Stored document content not found");
        }
        return new DocumentContent(blob.filename(), blob.contentType(), blob.content().clone());
    }

    @Override
    public void delete(String storageKey) {
        store.remove(storageKey);
    }
}
