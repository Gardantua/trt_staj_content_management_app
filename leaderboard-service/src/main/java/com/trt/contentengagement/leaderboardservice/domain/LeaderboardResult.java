package com.trt.contentengagement.leaderboardservice.domain;

import java.util.List;

public record LeaderboardResult(
        List<LeaderboardEntry> leaders,
        LeaderboardEntry currentUser,
        long participantCount
) {
    public LeaderboardResult {
        leaders = List.copyOf(leaders);
    }
}
