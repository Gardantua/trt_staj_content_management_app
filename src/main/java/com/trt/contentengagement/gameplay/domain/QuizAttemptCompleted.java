package com.trt.contentengagement.gameplay.domain;

import java.time.Instant;
import java.util.UUID;

public record QuizAttemptCompleted(
        UUID attemptId,
        UUID userId,
        UUID quizId,
        UUID quizVersionId,
        int score,
        int earnedXp,
        Instant completedAt
) {
}
