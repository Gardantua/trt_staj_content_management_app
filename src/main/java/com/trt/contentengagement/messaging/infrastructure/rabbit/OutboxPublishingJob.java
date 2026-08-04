package com.trt.contentengagement.messaging.infrastructure.rabbit;

import com.trt.contentengagement.messaging.application.OutboxPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.rabbit-enabled", havingValue = "true")
public class OutboxPublishingJob {
    private final OutboxPublisher outboxPublisher;

    public OutboxPublishingJob(OutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    @Scheduled(
            fixedDelayString = "${app.messaging.publisher-delay:1000}",
            initialDelayString = "${app.messaging.publisher-delay:1000}"
    )
    public void publishPendingEvents() {
        outboxPublisher.publishPendingBatch();
    }
}
