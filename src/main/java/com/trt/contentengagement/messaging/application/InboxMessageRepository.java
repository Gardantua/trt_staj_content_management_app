package com.trt.contentengagement.messaging.application;

import java.time.Instant;
import java.util.UUID;

public interface InboxMessageRepository {
    boolean claim(
            UUID eventId,
            String consumerName,
            String eventType,
            int eventVersion,
            Instant processedAt
    );
}
