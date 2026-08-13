package com.trt.contentengagement.messaging.infrastructure.persistence;

import java.util.HashMap;
import java.util.Map;

import com.trt.contentengagement.gamification.application.XpTransactionEventOutbox;
import com.trt.contentengagement.gamification.domain.XpTransaction;
import com.trt.contentengagement.messaging.application.OutboxEvent;
import com.trt.contentengagement.messaging.application.OutboxEventRepository;
import com.trt.contentengagement.messaging.application.XpChangedIntegrationEventV1;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class TransactionalXpTransactionOutbox implements XpTransactionEventOutbox {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final Propagator propagator;

    public TransactionalXpTransactionOutbox(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            Tracer tracer,
            Propagator propagator
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Override
    public void stage(XpTransaction xpTransaction) {
        XpChangedIntegrationEventV1 event = XpChangedIntegrationEventV1.from(xpTransaction);
        String traceId = traceId(event);
        Map<String, String> traceCarrier = currentTraceCarrier();
        outboxEventRepository.appendIfAbsent(new OutboxEvent(
                event.eventId(),
                "XP_TRANSACTION",
                event.transactionId(),
                XpChangedIntegrationEventV1.EVENT_TYPE,
                XpChangedIntegrationEventV1.EVENT_VERSION,
                objectMapper.writeValueAsString(event),
                traceId,
                traceCarrier.get("traceparent"),
                traceCarrier.get("tracestate"),
                event.occurredAt(),
                0
        ));
    }

    private String traceId(XpChangedIntegrationEventV1 event) {
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get("requestTraceId");
        }
        return traceId == null || traceId.isBlank() ? event.eventId().toString() : traceId;
    }

    private Map<String, String> currentTraceCarrier() {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return Map.of();
        }
        Map<String, String> traceCarrier = new HashMap<>();
        propagator.inject(currentSpan.context(), traceCarrier, Map::put);
        return traceCarrier;
    }
}
