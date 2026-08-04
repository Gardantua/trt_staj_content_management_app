package com.trt.contentengagement.messaging.application;

import java.time.Clock;
import java.time.Instant;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.messaging.rabbit-enabled", havingValue = "true")
public class OutboxPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final RabbitEventGateway rabbitEventGateway;
    private final Clock clock;
    private final int batchSize;
    private final Counter publishedCounter;
    private final Counter failedCounter;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            RabbitEventGateway rabbitEventGateway,
            Clock clock,
            MeterRegistry meterRegistry,
            @Value("${app.messaging.publisher-batch-size:25}") int batchSize
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitEventGateway = rabbitEventGateway;
        this.clock = clock;
        this.batchSize = batchSize;
        this.publishedCounter = meterRegistry.counter("messaging.outbox.published");
        this.failedCounter = meterRegistry.counter("messaging.outbox.publish.failed");
    }

    @Transactional
    public int publishPendingBatch() {
        Instant now = clock.instant();
        var pendingEvents = outboxEventRepository.lockPendingBatch(now, batchSize);
        int publishedCount = 0;
        for (OutboxEvent event : pendingEvents) {
            try {
                rabbitEventGateway.publish(event);
                outboxEventRepository.markPublished(event, clock.instant());
                publishedCounter.increment();
                publishedCount++;
            } catch (RuntimeException exception) {
                outboxEventRepository.markFailed(
                        event,
                        nextAttemptAt(event.publishAttempts(), clock.instant()),
                        safeErrorMessage(exception)
                );
                failedCounter.increment();
            }
        }
        return publishedCount;
    }

    private Instant nextAttemptAt(int previousAttempts, Instant now) {
        long delaySeconds = Math.min(60, 1L << Math.min(previousAttempts, 6));
        return now.plusSeconds(delaySeconds);
    }

    private String safeErrorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
