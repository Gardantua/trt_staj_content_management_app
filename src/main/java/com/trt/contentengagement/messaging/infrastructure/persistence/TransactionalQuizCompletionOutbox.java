package com.trt.contentengagement.messaging.infrastructure.persistence;

import java.util.HashMap;
import java.util.Map;

import com.trt.contentengagement.gameplay.application.QuizCompletionEventOutbox;
import com.trt.contentengagement.gameplay.domain.QuizAttemptCompleted;
import com.trt.contentengagement.messaging.application.OutboxEvent;
import com.trt.contentengagement.messaging.application.OutboxEventRepository;
import com.trt.contentengagement.messaging.application.QuizCompletedIntegrationEventV1;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class TransactionalQuizCompletionOutbox implements QuizCompletionEventOutbox {
    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String REQUEST_TRACE_ID_MDC_KEY = "requestTraceId";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final Propagator propagator;

    public TransactionalQuizCompletionOutbox(
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
    public void stage(QuizAttemptCompleted completedAttempt) {
        QuizCompletedIntegrationEventV1 integrationEvent =
                QuizCompletedIntegrationEventV1.from(completedAttempt);
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get(REQUEST_TRACE_ID_MDC_KEY);
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = integrationEvent.eventId().toString();
        }
        Map<String, String> traceCarrier = currentTraceCarrier();
        outboxEventRepository.appendIfAbsent(new OutboxEvent(
                integrationEvent.eventId(),
                "QUIZ_ATTEMPT",
                integrationEvent.attemptId(),
                QuizCompletedIntegrationEventV1.EVENT_TYPE,
                QuizCompletedIntegrationEventV1.EVENT_VERSION,
                objectMapper.writeValueAsString(integrationEvent),
                traceId,
                traceCarrier.get("traceparent"),
                traceCarrier.get("tracestate"),
                integrationEvent.occurredAt(),
                0
        ));
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
