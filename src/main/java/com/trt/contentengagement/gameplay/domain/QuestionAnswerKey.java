package com.trt.contentengagement.gameplay.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record QuestionAnswerKey(
        UUID questionId,
        int questionOrder,
        List<UUID> optionIds,
        UUID correctOptionId
) {
    public QuestionAnswerKey {
        Objects.requireNonNull(questionId);
        Objects.requireNonNull(correctOptionId);
        optionIds = List.copyOf(Objects.requireNonNull(optionIds));
        if (!optionIds.contains(correctOptionId)) {
            throw new IllegalArgumentException("Correct option must belong to the question.");
        }
    }
}
