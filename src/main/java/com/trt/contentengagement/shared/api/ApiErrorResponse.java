package com.trt.contentengagement.shared.api;

import java.time.Instant;

public record ApiErrorResponse(
        String code,
        String message,
        String traceId,
        Instant timestamp
) {
}

