package ru.adiaphora.platform.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.adiaphora.platform.common.event.ResourceAccessDeniedEvent;
import ru.adiaphora.platform.common.web.CorrelationId;

import java.time.Clock;
import java.util.List;

/**
 * Single translation point from exceptions to the {@link ApiError} contract. It deliberately never
 * exposes framework or persistence messages to clients: unexpected errors are logged with the
 * correlation id and returned as a generic {@code INTERNAL_ERROR}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ApplicationEventPublisher events;
    private final Clock clock;

    public GlobalExceptionHandler(ApplicationEventPublisher events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    @ExceptionHandler(QuestionnaireValidationException.class)
    public ResponseEntity<ApiError> handleQuestionnaireValidation(QuestionnaireValidationException ex,
                                                                  HttpServletRequest request) {
        ApiError body = ApiError.of(ex.status().value(), ex.errorCode(), ex.getMessage(),
                request.getRequestURI(), CorrelationId.current(), ex.fieldErrors());
        return ResponseEntity.status(ex.status()).body(body);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        if (ex.errorCode() == ErrorCode.ACCESS_DENIED) {
            events.publishEvent(new ResourceAccessDeniedEvent(request.getRequestURI(), clock.instant()));
        }
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
        events.publishEvent(new ResourceAccessDeniedEvent(request.getRequestURI(), clock.instant()));
        ApiError body = ApiError.of(HttpStatus.FORBIDDEN.value(), ErrorCode.ACCESS_DENIED,
                "Access denied", request.getRequestURI(), CorrelationId.current());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // --- client mistakes that must never surface as 500s ------------------------
    // Logged without stack traces: these are request errors, not server faults.

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        logClientError(request, "malformed request body");
        ApiError body = ApiError.of(HttpStatus.BAD_REQUEST.value(), ErrorCode.VALIDATION_ERROR,
                "Malformed request body", request.getRequestURI(), CorrelationId.current());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        logClientError(request, "type mismatch for '" + ex.getName() + "'");
        ApiError body = ApiError.of(HttpStatus.BAD_REQUEST.value(), ErrorCode.VALIDATION_ERROR,
                "Invalid value for '" + ex.getName() + "'", request.getRequestURI(),
                CorrelationId.current());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex,
                                                           HttpServletRequest request) {
        logClientError(request, "missing parameter '" + ex.getParameterName() + "'");
        ApiError body = ApiError.of(HttpStatus.BAD_REQUEST.value(), ErrorCode.VALIDATION_ERROR,
                "Missing required parameter '" + ex.getParameterName() + "'",
                request.getRequestURI(), CorrelationId.current());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex,
                                                     HttpServletRequest request) {
        logClientError(request, "no such resource");
        ApiError body = ApiError.of(HttpStatus.NOT_FOUND.value(), ErrorCode.RESOURCE_NOT_FOUND,
                "Resource not found", request.getRequestURI(), CorrelationId.current());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                             HttpServletRequest request) {
        logClientError(request, "method not allowed");
        ApiError body = ApiError.of(HttpStatus.METHOD_NOT_ALLOWED.value(), ErrorCode.METHOD_NOT_ALLOWED,
                "Method not allowed", request.getRequestURI(), CorrelationId.current());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                    HttpServletRequest request) {
        logClientError(request, "unsupported media type");
        ApiError body = ApiError.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type",
                request.getRequestURI(), CorrelationId.current());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    private void logClientError(HttpServletRequest request, String summary) {
        log.debug("Client error [correlationId={}] {} {}: {}", CorrelationId.current(),
                request.getMethod(), request.getRequestURI(), summary);
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
