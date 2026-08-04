package com.trt.contentengagement.identity.infrastructure.security;

import java.io.IOException;
import com.trt.contentengagement.shared.api.ApiErrorResponseWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorResponseWriter {

    private final ApiErrorResponseWriter apiErrorResponseWriter;

    public SecurityErrorResponseWriter(ApiErrorResponseWriter apiErrorResponseWriter) {
        this.apiErrorResponseWriter = apiErrorResponseWriter;
    }

    public void write(
            HttpServletResponse response,
            int httpStatus,
            String errorCode,
            String errorMessage
    ) throws IOException {
        apiErrorResponseWriter.write(response, httpStatus, errorCode, errorMessage);
    }
}
