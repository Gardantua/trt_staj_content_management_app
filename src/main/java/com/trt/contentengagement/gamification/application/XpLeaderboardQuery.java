package com.trt.contentengagement.gamification.application;

import java.util.List;
import java.util.UUID;

public interface XpLeaderboardQuery {
    XpLeaderboardResult findGlobal(UUID currentUserId, int limit);
    XpLeaderboardResult findByContent(UUID contentId, UUID currentUserId, int limit);
    List<RankedXpEntry> findAllGlobal();
    List<UUID> findRankedContentIds();
    List<RankedXpEntry> findAllByContent(UUID contentId);
}
