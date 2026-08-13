package com.trt.contentengagement.leaderboardservice.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record XpChangedEventV1(
        UUID eventId,
        UUID transactionId,
        UUID userId,
        UUID contentId,
        int amount,
        String reason,
        Instant occurredAt
) {
    private static final Set<String> SUPPORTED_REASONS = Set.of(
            "QUIZ_COMPLETED", "ADMIN_ADJUSTMENT"
    );

    public XpChangedEventV1 {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(contentId, "contentId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (!eventId.equals(transactionId)) {
            throw new IllegalArgumentException("XP event ID must equal transaction ID.");
        }
        if (!SUPPORTED_REASONS.contains(reason)) {
            throw new IllegalArgumentException("Unsupported XP reason.");
        }
    }
}
