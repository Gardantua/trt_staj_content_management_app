package com.trt.contentengagement.leaderboardservice.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.leaderboardservice.domain.LeaderboardEntry;

public interface LeaderboardCache {
    Optional<CachedLeaderboard> findGlobal(UUID currentUserId, int limit);
    Optional<CachedLeaderboard> findByContent(UUID contentId, UUID currentUserId, int limit);
    void replace(
            List<LeaderboardEntry> global,
            java.util.Map<UUID, List<LeaderboardEntry>> contents,
            Instant generatedAt
    );
}
