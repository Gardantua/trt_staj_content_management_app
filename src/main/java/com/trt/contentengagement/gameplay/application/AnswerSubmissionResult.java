package com.trt.contentengagement.gameplay.application;

import java.util.UUID;

public record AnswerSubmissionResult(
        UUID attemptId,
        AttemptDetails.AnswerFeedback feedback,
        String attemptStatus,
        int score,
        Integer earnedXp,
        AttemptDetails.QuestionView nextQuestion
) { }
