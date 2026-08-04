package com.trt.contentengagement.quiz.domain;

import java.util.Objects;
import java.util.UUID;

public record AnswerOption(UUID id, int optionOrder, String text, boolean correct) {

    public AnswerOption {
        Objects.requireNonNull(id, "id must not be null");
        if (optionOrder < 1) {
            throw new QuizRuleViolationException(
                    "QUIZ_INVALID_OPTION_ORDER",
                    "Option order must be positive."
            );
        }
        text = QuizText.require(text, 500, "option text");
    }

    public static AnswerOption create(int optionOrder, String text, boolean correct) {
        return new AnswerOption(UUID.randomUUID(), optionOrder, text, correct);
    }

    AnswerOption copyWithNewId() {
        return create(optionOrder, text, correct);
    }
}
