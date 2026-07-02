package ru.adiaphora.platform.auth.domain;

import org.springframework.http.HttpStatus;
import ru.adiaphora.platform.common.error.BusinessException;
import ru.adiaphora.platform.common.error.ErrorCode;

/** Thrown when registering with an email that already exists. */
public class EmailAlreadyUsedException extends BusinessException {

    public EmailAlreadyUsedException() {
        super(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT, "Email is already registered");
    }
}
