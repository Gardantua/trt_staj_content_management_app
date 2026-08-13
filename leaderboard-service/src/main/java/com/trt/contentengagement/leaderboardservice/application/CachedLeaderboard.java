package com.trt.contentengagement.leaderboardservice.application;

import java.time.Instant;

import com.trt.contentengagement.leaderboardservice.domain.LeaderboardResult;

public record CachedLeaderboard(LeaderboardResult result, Instant generatedAt) {
}
