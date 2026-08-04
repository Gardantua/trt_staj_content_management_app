package com.trt.contentengagement.messaging.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.trt.contentengagement.gameplay.domain.QuizAttemptCompleted;
import org.junit.jupiter.api.Test;

class QuizCompletedIntegrationEventV1Test {
    @Test
    void sameAttemptProducesStableVersionedEventIdentity() {
        UUID attemptId = UUID.randomUUID();
        QuizAttemptCompleted completion = new QuizAttemptCompleted(
                attemptId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                200,
                Instant.parse("2026-08-04T10:15:30.123456789Z")
        );

        QuizCompletedIntegrationEventV1 first = QuizCompletedIntegrationEventV1.from(completion);
        QuizCompletedIntegrationEventV1 repeated = QuizCompletedIntegrationEventV1.from(completion);

        assertThat(first).isEqualTo(repeated);
        assertThat(first.eventId()).isNotEqualTo(attemptId);
        assertThat(first.xpPolicyVersion()).isEqualTo("SCORE_MATCH_V1");
        assertThat(first.occurredAt()).isEqualTo(
                completion.completedAt().truncatedTo(ChronoUnit.MICROS)
        );
        assertThat(QuizCompletedIntegrationEventV1.EVENT_VERSION).isEqualTo(1);
    }

    @Test
    void eventCarriesOnlyServerFinalizedScore() {
        QuizCompletedIntegrationEventV1 event = QuizCompletedIntegrationEventV1.from(
                new QuizAttemptCompleted(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), 100, Instant.now()
                )
        );

        assertThat(event.finalScore()).isEqualTo(100);
    }
}
