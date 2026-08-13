package com.trt.contentengagement.leaderboardservice.application;

import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.leaderboardservice.domain.LeaderboardResult;

public record LeaderboardView(
        String scope,
        UUID contentId,
        String period,
        String dataSource,
        Instant projectionGeneratedAt,
        LeaderboardResult result
) {
}
