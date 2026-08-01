package com.trt.contentengagement.shared.api;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
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
