package com.trt.contentengagement.shared.api;

import java.io.IOException;
import java.time.Clock;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ApiErrorResponseWriter {
    private static final String REQUEST_TRACE_ID_MDC_KEY = "requestTraceId";
    private static final String UNKNOWN_TRACE_ID = "unknown";

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final ApiErrorMessageResolver errorMessageResolver;

    public ApiErrorResponseWriter(
            ObjectMapper objectMapper,
            Clock clock,
            ApiErrorMessageResolver errorMessageResolver
    ) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.errorMessageResolver = errorMessageResolver;
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            int httpStatus,
            String errorCode,
            String errorMessage
    ) throws IOException {
        String requestTraceId = MDC.get(REQUEST_TRACE_ID_MDC_KEY);
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                errorCode,
                errorMessageResolver.resolve(request, errorCode, errorMessage),
                requestTraceId == null ? UNKNOWN_TRACE_ID : requestTraceId,
                clock.instant()
        );

        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
