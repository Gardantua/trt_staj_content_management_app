package com.trt.contentengagement.gameplay.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.gameplay.domain.QuizAttempt;

public interface QuizAttemptRepository {
    QuizAttempt save(QuizAttempt attempt);
    CompletionReward recordFirstCompletionReward(QuizAttempt completedAttempt);
    Optional<QuizAttempt> findByIdAndUserId(UUID attemptId, UUID userId);
    Optional<QuizAttempt> findActiveByUserIdAndQuizId(UUID userId, UUID quizId);
    List<QuizResultSummary> findLatestCompletedResultsByUserId(UUID userId);

    record CompletionReward(QuizAttempt attempt, boolean firstCompletionReward) { }
}
