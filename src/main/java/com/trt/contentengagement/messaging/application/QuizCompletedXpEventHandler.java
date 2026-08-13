package com.trt.contentengagement.messaging.application;

import java.time.Clock;

import com.trt.contentengagement.gamification.application.XpService;
import com.trt.contentengagement.gamification.domain.XpPolicyVersion;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class QuizCompletedXpEventHandler {
    public static final String CONSUMER_NAME = "gamification.quiz-completed-xp.v1";

    private final ObjectMapper objectMapper;
    private final InboxMessageRepository inboxMessageRepository;
    private final XpService xpService;
    private final Clock clock;
    private final Counter processedCounter;
    private final Counter duplicateCounter;

    public QuizCompletedXpEventHandler(
            ObjectMapper objectMapper,
            InboxMessageRepository inboxMessageRepository,
            XpService xpService,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.objectMapper = objectMapper;
        this.inboxMessageRepository = inboxMessageRepository;
        this.xpService = xpService;
        this.clock = clock;
        this.processedCounter = meterRegistry.counter("messaging.consumer.processed");
        this.duplicateCounter = meterRegistry.counter("messaging.consumer.duplicate");
    }

    @Transactional
    public void handle(String payload) {
        if (objectMapper.readTree(payload).has("firstCompletionReward")) {
            handleV2(objectMapper.readValue(payload, QuizCompletedIntegrationEventV2.class));
            return;
        }
        handleV1(objectMapper.readValue(payload, QuizCompletedIntegrationEventV1.class));
    }

    private void handleV1(QuizCompletedIntegrationEventV1 event) {
        boolean claimed = inboxMessageRepository.claim(
                event.eventId(), CONSUMER_NAME,
                QuizCompletedIntegrationEventV1.EVENT_TYPE,
                QuizCompletedIntegrationEventV1.EVENT_VERSION,
                clock.instant()
        );
        if (!claimed) {
            duplicateCounter.increment();
            return;
        }
        if (event.awardedXp() > 0) {
            xpService.awardQuizCompletion(
                    event.userId(), event.quizId(), event.attemptId(),
                    event.awardedXp(), XpPolicyVersion.valueOf(event.xpPolicyVersion()),
                    event.occurredAt()
            );
        }
        processedCounter.increment();
    }

    private void handleV2(QuizCompletedIntegrationEventV2 event) {
        boolean claimed = inboxMessageRepository.claim(
                event.eventId(), CONSUMER_NAME,
                QuizCompletedIntegrationEventV2.EVENT_TYPE,
                QuizCompletedIntegrationEventV2.EVENT_VERSION,
                clock.instant()
        );
        if (!claimed) {
            duplicateCounter.increment();
            return;
        }
        if (event.firstCompletionReward()) {
            xpService.awardQuizCompletion(
                    event.userId(), event.quizId(), event.attemptId(),
                    event.earnedXp(), XpPolicyVersion.valueOf(event.xpPolicyVersion()),
                    event.occurredAt()
            );
        }
        processedCounter.increment();
    }
}
