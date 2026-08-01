package com.trt.contentengagement.identity.infrastructure.security;

import java.io.IOException;
import java.time.Instant;

import com.trt.contentengagement.shared.api.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorResponseWriter {

    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String UNKNOWN_TRACE_ID = "unknown";

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(
            HttpServletResponse response,
            int httpStatus,
            String errorCode,
            String errorMessage
    ) throws IOException {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        String resolvedTraceId = traceId == null ? UNKNOWN_TRACE_ID : traceId;
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                errorCode,
                errorMessage,
                resolvedTraceId,
                Instant.now()
        );

        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
