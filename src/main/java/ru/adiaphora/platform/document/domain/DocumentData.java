package ru.adiaphora.platform.document.domain;

import java.util.Map;
import java.util.UUID;

/** Input data for generating a document (non-sensitive fields only in this placeholder). */
public record DocumentData(UUID applicationId, Map<String, String> fields) {

    public DocumentData {
        fields = fields == null ? Map.of() : Map.copyOf(fields);
    }
}
