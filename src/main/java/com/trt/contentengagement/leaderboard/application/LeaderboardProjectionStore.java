package com.trt.contentengagement.leaderboard.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.gamification.application.RankedXpEntry;

public interface LeaderboardProjectionStore {
    Optional<CachedLeaderboard> findGlobal(UUID currentUserId, int limit);

    Optional<CachedLeaderboard> findByContent(
            UUID contentId, UUID currentUserId, int limit
    );

    boolean replaceAll(
            List<RankedXpEntry> globalEntries,
            Map<UUID, List<RankedXpEntry>> contentEntries,
            Instant generatedAt
    );
}
