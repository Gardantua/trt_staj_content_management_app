package com.trt.contentengagement.gameplay.application;

import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.gameplay.domain.QuizAttempt;

public interface QuizAttemptRepository {
    QuizAttempt save(QuizAttempt attempt);
    Optional<QuizAttempt> findByIdAndUserId(UUID attemptId, UUID userId);
    Optional<QuizAttempt> findActiveByUserIdAndQuizId(UUID userId, UUID quizId);
}
