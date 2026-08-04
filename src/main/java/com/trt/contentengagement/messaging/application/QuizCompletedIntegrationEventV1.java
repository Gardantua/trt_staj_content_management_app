package com.trt.contentengagement.messaging.application;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import com.trt.contentengagement.gameplay.domain.QuizAttemptCompleted;

public record QuizCompletedIntegrationEventV1(
        UUID eventId,
        UUID attemptId,
        UUID userId,
        UUID quizId,
        UUID quizVersionId,
        int finalScore,
        String xpPolicyVersion,
        Instant occurredAt
) {
    public static final String EVENT_TYPE = "quiz.completed";
    public static final int EVENT_VERSION = 1;
    public static final String XP_POLICY_VERSION = "SCORE_MATCH_V1";

    public QuizCompletedIntegrationEventV1 {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(attemptId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(quizId);
        Objects.requireNonNull(quizVersionId);
        Objects.requireNonNull(xpPolicyVersion);
        Objects.requireNonNull(occurredAt);
        if (finalScore < 0) {
            throw new IllegalArgumentException("Final score cannot be negative.");
        }
        if (!XP_POLICY_VERSION.equals(xpPolicyVersion)) {
            throw new IllegalArgumentException("Unsupported XP policy version.");
        }
    }

    public static QuizCompletedIntegrationEventV1 from(QuizAttemptCompleted completedAttempt) {
        UUID eventId = UUID.nameUUIDFromBytes(
                (EVENT_TYPE + ":v1:" + completedAttempt.attemptId())
                        .getBytes(StandardCharsets.UTF_8)
        );
        return new QuizCompletedIntegrationEventV1(
                eventId,
                completedAttempt.attemptId(),
                completedAttempt.userId(),
                completedAttempt.quizId(),
                completedAttempt.quizVersionId(),
                completedAttempt.score(),
                XP_POLICY_VERSION,
                completedAttempt.completedAt().truncatedTo(ChronoUnit.MICROS)
        );
    }
}
