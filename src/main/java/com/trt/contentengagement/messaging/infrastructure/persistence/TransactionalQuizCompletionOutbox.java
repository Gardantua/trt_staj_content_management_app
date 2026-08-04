package com.trt.contentengagement.messaging.infrastructure.persistence;

import com.trt.contentengagement.gameplay.application.QuizCompletionEventOutbox;
import com.trt.contentengagement.gameplay.domain.QuizAttemptCompleted;
import com.trt.contentengagement.messaging.application.OutboxEvent;
import com.trt.contentengagement.messaging.application.OutboxEventRepository;
import com.trt.contentengagement.messaging.application.QuizCompletedIntegrationEventV1;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class TransactionalQuizCompletionOutbox implements QuizCompletionEventOutbox {
    private static final String TRACE_ID_MDC_KEY = "traceId";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public TransactionalQuizCompletionOutbox(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void stage(QuizAttemptCompleted completedAttempt) {
        QuizCompletedIntegrationEventV1 integrationEvent =
                QuizCompletedIntegrationEventV1.from(completedAttempt);
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        if (traceId == null || traceId.isBlank()) {
            traceId = integrationEvent.eventId().toString();
        }
        outboxEventRepository.appendIfAbsent(new OutboxEvent(
                integrationEvent.eventId(),
                "QUIZ_ATTEMPT",
                integrationEvent.attemptId(),
                QuizCompletedIntegrationEventV1.EVENT_TYPE,
                QuizCompletedIntegrationEventV1.EVENT_VERSION,
                objectMapper.writeValueAsString(integrationEvent),
                traceId,
                integrationEvent.occurredAt(),
                0
        ));
    }
}
