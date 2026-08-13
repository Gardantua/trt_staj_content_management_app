package com.trt.contentengagement.gameplay.application;

import java.util.UUID;

public record QuizResultSummary(
        UUID quizId,
        Integer earnedXp
) {
}
