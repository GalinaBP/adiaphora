package ru.adiaphora.platform.review.domain;

import org.springframework.http.HttpStatus;
import ru.adiaphora.platform.common.error.BusinessException;
import ru.adiaphora.platform.common.error.ErrorCode;

/** Thrown when a review decision or route override is attempted without the required reason. */
public class UndocumentedDecisionException extends BusinessException {

    public UndocumentedDecisionException(String message) {
        super(ErrorCode.VALIDATION_ERROR, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
