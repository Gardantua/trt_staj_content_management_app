package com.trt.contentengagement.gamification.application;

import java.util.List;

public record XpLeaderboardResult(
        List<RankedXpEntry> leaders,
        RankedXpEntry currentUser,
        long participantCount
) {
    public XpLeaderboardResult {
        leaders = List.copyOf(leaders);
    }
}
