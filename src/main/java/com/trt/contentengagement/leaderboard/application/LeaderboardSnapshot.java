package com.trt.contentengagement.leaderboard.application;

import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.leaderboard.domain.LeaderboardPeriod;
import com.trt.contentengagement.leaderboard.domain.LeaderboardScope;

public record LeaderboardSnapshot(
        LeaderboardScope scope,
        UUID contentId,
        LeaderboardPeriod period,
        long participantCount,
        List<LeaderboardEntry> leaders,
        LeaderboardEntry currentUser
) {
    public LeaderboardSnapshot {
        leaders = List.copyOf(leaders);
    }
}
