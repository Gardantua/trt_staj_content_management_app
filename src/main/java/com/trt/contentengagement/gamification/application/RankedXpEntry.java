package com.trt.contentengagement.gamification.application;

import java.time.Instant;
import java.util.UUID;

public record RankedXpEntry(
        long position,
        UUID userId,
        long totalXp,
        Instant firstXpAt
) {
}
