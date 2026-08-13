package com.trt.contentengagement.messaging.application;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import com.trt.contentengagement.gameplay.domain.QuizAttemptCompleted;

public record QuizCompletedIntegrationEventV2(
        UUID eventId,
        UUID attemptId,
        UUID userId,
        UUID quizId,
        UUID quizVersionId,
        int finalScore,
        int earnedXp,
        boolean firstCompletionReward,
        String xpPolicyVersion,
        Instant occurredAt
) {
    public static final String EVENT_TYPE = "quiz.completed";
    public static final int EVENT_VERSION = 2;
    public static final String XP_POLICY_VERSION = "FIRST_COMPLETION_SCORE_V2";

    public QuizCompletedIntegrationEventV2 {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(attemptId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(quizId);
        Objects.requireNonNull(quizVersionId);
        Objects.requireNonNull(xpPolicyVersion);
        Objects.requireNonNull(occurredAt);
        if (finalScore < 0 || earnedXp < 0 || earnedXp > finalScore) {
            throw new IllegalArgumentException("Score and earned XP must be valid.");
        }
        if (firstCompletionReward && earnedXp != finalScore) {
            throw new IllegalArgumentException("First completion XP must equal the final score.");
        }
        if (!firstCompletionReward && earnedXp != 0) {
            throw new IllegalArgumentException("Practice completion cannot award XP.");
        }
        if (!XP_POLICY_VERSION.equals(xpPolicyVersion)) {
            throw new IllegalArgumentException("Unsupported XP policy version.");
        }
    }

    public static QuizCompletedIntegrationEventV2 from(
            QuizAttemptCompleted completedAttempt, boolean firstCompletionReward
    ) {
        UUID eventId = UUID.nameUUIDFromBytes(
                (EVENT_TYPE + ":v2:" + completedAttempt.attemptId())
                        .getBytes(StandardCharsets.UTF_8)
        );
        return new QuizCompletedIntegrationEventV2(
                eventId,
                completedAttempt.attemptId(),
                completedAttempt.userId(),
                completedAttempt.quizId(),
                completedAttempt.quizVersionId(),
                completedAttempt.score(),
                completedAttempt.earnedXp(),
                firstCompletionReward,
                XP_POLICY_VERSION,
                completedAttempt.completedAt().truncatedTo(ChronoUnit.MICROS)
        );
    }
}
