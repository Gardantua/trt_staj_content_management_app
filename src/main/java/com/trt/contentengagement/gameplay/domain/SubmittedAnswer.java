package com.trt.contentengagement.gameplay.domain;

import java.time.Instant;
import java.util.UUID;

public record SubmittedAnswer(
        UUID id,
        UUID questionId,
        UUID selectedOptionId,
        String idempotencyKey,
        boolean correct,
        int awardedPoints,
        Instant answeredAt
) {
}
