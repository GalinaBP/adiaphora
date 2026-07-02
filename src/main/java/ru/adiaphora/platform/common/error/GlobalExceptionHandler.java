package ru.adiaphora.platform.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.adiaphora.platform.common.web.CorrelationId;

import java.util.List;

/**
 * Single translation point from exceptions to the {@link ApiError} contract. It deliberately never
 * exposes framework or persistence messages to clients: unexpected errors are logged with the
 * correlation id and returned as a generic {@code INTERNAL_ERROR}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(QuestionnaireValidationException.class)
    public ResponseEntity<ApiError> handleQuestionnaireValidation(QuestionnaireValidationException ex,
                                                                  HttpServletRequest request) {
        ApiError body = ApiError.of(ex.status().value(), ex.errorCode(), ex.getMessage(),
                request.getRequestURI(), CorrelationId.current(), ex.fieldErrors());
        return ResponseEntity.status(ex.status()).body(body);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        ApiError body = ApiError.of(ex.status().value(), ex.errorCode(), ex.getMessage(),
                request.getRequestURI(), CorrelationId.current());
        return ResponseEntity.status(ex.status()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        List<ApiFieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        ApiError body = ApiError.of(HttpStatus.BAD_REQUEST.value(), ErrorCode.VALIDATION_ERROR,
                "Request validation failed", request.getRequestURI(), CorrelationId.current(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex,
                                                         HttpServletRequest request) {
        ApiError body = ApiError.of(HttpStatus.UNAUTHORIZED.value(), ErrorCode.AUTHENTICATION_FAILED,
                "Authentication failed", request.getRequestURI(), CorrelationId.current());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                       HttpServletRequest request) {
        ApiError body = ApiError.of(HttpStatus.FORBIDDEN.value(), ErrorCode.ACCESS_DENIED,
                "Access denied", request.getRequestURI(), CorrelationId.current());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        String correlationId = CorrelationId.current();
        // Full detail goes to the log (correlated), never to the client.
        log.error("Unhandled exception [correlationId={}]", correlationId, ex);
        ApiError body = ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred", request.getRequestURI(), correlationId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ApiFieldError toFieldError(FieldError fieldError) {
        return new ApiFieldError(fieldError.getField(),
                fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "invalid value");
    }
}
