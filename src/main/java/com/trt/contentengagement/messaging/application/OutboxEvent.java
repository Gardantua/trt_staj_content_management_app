package com.trt.contentengagement.messaging.application;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        UUID eventId,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        int eventVersion,
        String payload,
        String traceId,
        String traceParent,
        String traceState,
        Instant occurredAt,
        int publishAttempts
) {
}
