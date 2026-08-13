package com.trt.contentengagement.leaderboardservice.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.leaderboardservice.domain.LeaderboardEntry;
import com.trt.contentengagement.leaderboardservice.domain.LeaderboardResult;
import com.trt.contentengagement.leaderboardservice.domain.XpChangedEventV1;

public interface LeaderboardProjectionRepository {
    boolean appendIfAbsent(XpChangedEventV1 event, Instant consumedAt);
    LeaderboardResult findGlobal(UUID currentUserId, int limit);
    LeaderboardResult findByContent(UUID contentId, UUID currentUserId, int limit);
    List<LeaderboardEntry> findAllGlobal();
    List<UUID> findContentIds();
    List<LeaderboardEntry> findAllByContent(UUID contentId);
}
