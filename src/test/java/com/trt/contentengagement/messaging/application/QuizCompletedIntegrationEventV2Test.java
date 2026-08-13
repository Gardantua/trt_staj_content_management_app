package com.trt.contentengagement.messaging.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.gameplay.domain.QuizAttemptCompleted;
import org.junit.jupiter.api.Test;

class QuizCompletedIntegrationEventV2Test {
    @Test
    void firstCompletionCanExplicitlyAwardZeroXp() {
        QuizCompletedIntegrationEventV2 event = QuizCompletedIntegrationEventV2.from(
                completion(0, 0), true
        );

        assertThat(event.firstCompletionReward()).isTrue();
        assertThat(event.earnedXp()).isZero();
        assertThat(QuizCompletedIntegrationEventV2.EVENT_VERSION).isEqualTo(2);
    }

    @Test
    void practiceCompletionCannotClaimPositiveXp() {
        assertThatThrownBy(() -> new QuizCompletedIntegrationEventV2(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), 10, 10, false,
                QuizCompletedIntegrationEventV2.XP_POLICY_VERSION, Instant.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private QuizAttemptCompleted completion(int score, int earnedXp) {
        return new QuizAttemptCompleted(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                score, earnedXp, Instant.now()
        );
    }
}
