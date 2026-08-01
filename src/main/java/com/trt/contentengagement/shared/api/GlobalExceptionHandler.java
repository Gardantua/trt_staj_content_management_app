package com.trt.contentengagement.shared.api;

import java.time.Instant;

import com.trt.contentengagement.content.application.ContentNotFoundException;
import com.trt.contentengagement.content.domain.ContentRuleViolationException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String UNKNOWN_TRACE_ID = "unknown";

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            NoResourceFoundException resourceNotFoundException
    ) {
        ApiErrorResponse errorResponse = createErrorResponse(
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found."
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ContentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleContentNotFound(
            ContentNotFoundException contentNotFoundException
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(
                "CONTENT_NOT_FOUND",
                "The requested content was not found."
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationFailure(
            MethodArgumentNotValidException validationException
    ) {
        ApiErrorResponse errorResponse = createErrorResponse(
                "VALIDATION_FAILED",
                "The request contains invalid data."
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(Exception malformedRequest) {
        return ResponseEntity.badRequest().body(createErrorResponse(
                "VALIDATION_FAILED",
                "The request contains invalid data."
        ));
    }

    @ExceptionHandler(ContentRuleViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleContentRuleViolation(
            ContentRuleViolationException ruleViolation
    ) {
        HttpStatus status = ruleViolation.errorCode().endsWith("_NOT_FOUND")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(createErrorResponse(
                ruleViolation.errorCode(),
                ruleViolation.getMessage()
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityConflict(
            DataIntegrityViolationException dataIntegrityViolationException
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse(
                "DATA_INTEGRITY_CONFLICT",
                "The requested change conflicts with existing data."
        ));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthorizationDenied(
            AuthorizationDeniedException authorizationDeniedException
    ) {
        ApiErrorResponse errorResponse = createErrorResponse(
                "ACCESS_DENIED",
                "The authenticated actor does not have permission."
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedFailure(Exception unexpectedException) {
        LOGGER.error("Unexpected request failure", unexpectedException);

        ApiErrorResponse errorResponse = createErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private ApiErrorResponse createErrorResponse(String errorCode, String errorMessage) {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        String resolvedTraceId = traceId == null ? UNKNOWN_TRACE_ID : traceId;

        return new ApiErrorResponse(
                errorCode,
                errorMessage,
                resolvedTraceId,
                Instant.now()
        );
    }
}
