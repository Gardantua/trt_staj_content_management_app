package com.trt.contentengagement.quiz.application;

import java.util.List;
import java.util.UUID;

public record GameplayQuizSnapshot(
        UUID quizId,
        UUID versionId,
        String scoringPolicyVersion,
        List<QuestionSnapshot> questions
) {
    public record QuestionSnapshot(
            UUID questionId,
            int questionOrder,
            String prompt,
            String difficulty,
            VisualSnapshot visual,
            String accessiblePrompt,
            List<OptionSnapshot> options,
            UUID correctOptionId
    ) {
    }
    public record VisualSnapshot(
            UUID mediaAssetId, String contentUrl, String role, String alternativeText
    ) { }
    public record OptionSnapshot(UUID optionId, int optionOrder, String text) {
    }
}
