package com.trt.contentengagement.quiz.application;

import java.util.List;
import java.util.UUID;

public record QuizTranslation(
        String title, String description, String fallbackAlternativeText,
        List<QuestionTranslation> questions
) {
    public record QuestionTranslation(UUID questionId, String prompt, String visualAlternativeText,
                                      String accessiblePrompt, List<OptionTranslation> answerOptions) { }
    public record OptionTranslation(UUID optionId, String text) { }
}
