package ru.adiaphora.platform.common.error;

import org.springframework.http.HttpStatus;

/** Thrown when document generation fails. */
public class DocumentGenerationException extends BusinessException {

    public DocumentGenerationException(String message) {
        super(ErrorCode.DOCUMENT_GENERATION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
