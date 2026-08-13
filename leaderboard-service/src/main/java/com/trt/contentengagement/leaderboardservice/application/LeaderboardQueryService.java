package com.trt.contentengagement.leaderboardservice.application;

import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.leaderboardservice.domain.LeaderboardResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaderboardQueryService {
    private final LeaderboardProjectionRepository projectionRepository;
    private final ObjectProvider<LeaderboardCache> cacheProvider;

    public LeaderboardQueryService(
            LeaderboardProjectionRepository projectionRepository,
            ObjectProvider<LeaderboardCache> cacheProvider
    ) {
        this.projectionRepository = projectionRepository;
        this.cacheProvider = cacheProvider;
    }

    @Transactional(readOnly = true)
    public LeaderboardView global(UUID currentUserId, int limit) {
        Optional<CachedLeaderboard> cached = cachedGlobal(currentUserId, limit);
        return cached
                .map(value -> view("GLOBAL", null, "REDIS", value))
                .orElseGet(() -> fallback(
                        "GLOBAL", null,
                        projectionRepository.findGlobal(currentUserId, limit)
                ));
    }

    @Transactional(readOnly = true)
    public LeaderboardView content(UUID contentId, UUID currentUserId, int limit) {
        Optional<CachedLeaderboard> cached = cachedContent(contentId, currentUserId, limit);
        return cached
                .map(value -> view("CONTENT", contentId, "REDIS", value))
                .orElseGet(() -> fallback(
                        "CONTENT", contentId,
                        projectionRepository.findByContent(contentId, currentUserId, limit)
                ));
    }

    private Optional<CachedLeaderboard> cachedGlobal(UUID currentUserId, int limit) {
        LeaderboardCache cache = cacheProvider.getIfAvailable();
        return cache == null ? Optional.empty() : cache.findGlobal(currentUserId, limit);
    }

    private Optional<CachedLeaderboard> cachedContent(
            UUID contentId, UUID currentUserId, int limit
    ) {
        LeaderboardCache cache = cacheProvider.getIfAvailable();
        return cache == null
                ? Optional.empty()
                : cache.findByContent(contentId, currentUserId, limit);
    }

    private LeaderboardView view(
            String scope, UUID contentId, String dataSource, CachedLeaderboard cached
    ) {
        return new LeaderboardView(
                scope, contentId, "ALL_TIME", dataSource,
                cached.generatedAt(), cached.result()
        );
    }

    private LeaderboardView fallback(
            String scope, UUID contentId, LeaderboardResult result
    ) {
        return new LeaderboardView(
                scope, contentId, "ALL_TIME", "POSTGRESQL_FALLBACK", null, result
        );
    }
}
