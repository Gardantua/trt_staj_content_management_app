package com.trt.contentengagement.gameplay.application;

import java.time.Instant;
import java.util.UUID;

public record AnswerSubmissionResult(
        UUID attemptId,
        AttemptDetails.AnswerFeedback feedback,
        String attemptStatus,
        int score,
        Integer earnedXp,
        Instant questionDeadline,
        AttemptDetails.QuestionView nextQuestion
) { }
