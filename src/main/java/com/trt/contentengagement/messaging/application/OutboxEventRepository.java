package com.trt.contentengagement.messaging.application;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository {
    void appendIfAbsent(OutboxEvent event);
    List<OutboxEvent> lockPendingBatch(Instant now, int batchSize);
    void markPublished(OutboxEvent event, Instant publishedAt);
    void markFailed(OutboxEvent event, Instant nextAttemptAt, String errorMessage);
}
