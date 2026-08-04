package com.trt.contentengagement.messaging.application;

import java.time.Clock;
import java.time.Instant;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
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
    private final ObservationRegistry observationRegistry;
    private final Tracer tracer;
    private final Propagator propagator;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            RabbitEventGateway rabbitEventGateway,
            Clock clock,
            MeterRegistry meterRegistry,
            ObservationRegistry observationRegistry,
            Tracer tracer,
            Propagator propagator,
            @Value("${app.messaging.publisher-batch-size:25}") int batchSize
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitEventGateway = rabbitEventGateway;
        this.clock = clock;
        this.batchSize = batchSize;
        this.publishedCounter = meterRegistry.counter("messaging.outbox.published");
        this.failedCounter = meterRegistry.counter("messaging.outbox.publish.failed");
        this.observationRegistry = observationRegistry;
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Transactional
    public int publishPendingBatch() {
        Instant now = clock.instant();
        var pendingEvents = outboxEventRepository.lockPendingBatch(now, batchSize);
        int publishedCount = 0;
        for (OutboxEvent event : pendingEvents) {
            try {
                publishObserved(event);
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

    private void publishObserved(OutboxEvent event) {
        Span publisherSpan = publisherSpan(event);
        try (Tracer.SpanInScope ignored = tracer.withSpan(publisherSpan)) {
            Observation.createNotStarted("messaging.outbox.publish", observationRegistry)
                    .lowCardinalityKeyValue("event.type", event.eventType())
                    .lowCardinalityKeyValue("event.version", Integer.toString(event.eventVersion()))
                    .observe(() -> {
                        rabbitEventGateway.publish(event);
                        outboxEventRepository.markPublished(event, clock.instant());
                    });
        } catch (RuntimeException exception) {
            publisherSpan.error(exception);
            throw exception;
        } finally {
            publisherSpan.end();
        }
    }

    private Span publisherSpan(OutboxEvent event) {
        Span.Builder spanBuilder;
        if (event.traceParent() == null || event.traceParent().isBlank()) {
            spanBuilder = tracer.spanBuilder();
        } else {
            var traceCarrier = new java.util.HashMap<String, String>();
            traceCarrier.put("traceparent", event.traceParent());
            if (event.traceState() != null && !event.traceState().isBlank()) {
                traceCarrier.put("tracestate", event.traceState());
            }
            spanBuilder = propagator.extract(traceCarrier, java.util.Map::get);
        }
        return spanBuilder
                .name("messaging.rabbitmq.publish")
                .kind(Span.Kind.PRODUCER)
                .tag("messaging.system", "rabbitmq")
                .tag("messaging.destination", "content.engagement.events")
                .start();
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
