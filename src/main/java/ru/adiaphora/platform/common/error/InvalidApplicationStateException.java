package ru.adiaphora.platform.common.error;

import org.springframework.http.HttpStatus;

/** Thrown when an operation is not allowed for the current lifecycle state of an aggregate. */
public class InvalidApplicationStateException extends BusinessException {

    public InvalidApplicationStateException(String message) {
        super(ErrorCode.INVALID_STATE, HttpStatus.CONFLICT, message);
    }
}
