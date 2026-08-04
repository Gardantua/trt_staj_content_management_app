package com.trt.contentengagement.leaderboard.application;

import java.time.Instant;

import com.trt.contentengagement.gamification.application.XpLeaderboardResult;

public record CachedLeaderboard(
        XpLeaderboardResult result,
        Instant generatedAt
) {
}
