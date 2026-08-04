package com.trt.contentengagement.leaderboard.application;

import java.time.Instant;
import java.util.UUID;

public record LeaderboardEntry(
        long position,
        UUID userId,
        long totalXp,
        Instant firstXpAt
) {
}
