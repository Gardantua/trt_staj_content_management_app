package com.trt.contentengagement.leaderboard.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.content.application.PublicContentQueryService;
import com.trt.contentengagement.gamification.application.RankedXpEntry;
import com.trt.contentengagement.gamification.application.XpLeaderboardQuery;
import com.trt.contentengagement.gamification.application.XpLeaderboardResult;
import com.trt.contentengagement.identity.application.CurrentActorProvider;
import com.trt.contentengagement.leaderboard.domain.LeaderboardPeriod;
import com.trt.contentengagement.leaderboard.domain.LeaderboardScope;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaderboardQueryService {
    private final XpLeaderboardQuery xpLeaderboardQuery;
    private final CurrentActorProvider currentActorProvider;
    private final PublicContentQueryService publicContentQueryService;
    private final ObjectProvider<LeaderboardProjectionStore> projectionStoreProvider;
    private final Counter redisHitCounter;
    private final Counter postgresqlFallbackCounter;

    public LeaderboardQueryService(
            XpLeaderboardQuery xpLeaderboardQuery,
            CurrentActorProvider currentActorProvider,
            PublicContentQueryService publicContentQueryService,
            ObjectProvider<LeaderboardProjectionStore> projectionStoreProvider,
            MeterRegistry meterRegistry
    ) {
        this.xpLeaderboardQuery = xpLeaderboardQuery;
        this.currentActorProvider = currentActorProvider;
        this.publicContentQueryService = publicContentQueryService;
        this.projectionStoreProvider = projectionStoreProvider;
        this.redisHitCounter = meterRegistry.counter("leaderboard.redis.hit");
        this.postgresqlFallbackCounter = meterRegistry.counter(
                "leaderboard.postgresql.fallback"
        );
    }

    @Transactional(readOnly = true)
    public LeaderboardSnapshot global(int limit) {
        UUID currentUserId = currentActorProvider.getCurrentActor().actorId();
        LeaderboardProjectionStore projectionStore = projectionStoreProvider.getIfAvailable();
        Optional<CachedLeaderboard> cached = projectionStore == null
                ? Optional.empty()
                : projectionStore.findGlobal(currentUserId, limit);
        return cached
                .map(value -> cachedSnapshot(
                        LeaderboardScope.GLOBAL, null, value
                ))
                .orElseGet(() -> fallbackSnapshot(
                        LeaderboardScope.GLOBAL, null,
                        xpLeaderboardQuery.findGlobal(currentUserId, limit)
                ));
    }

    @Transactional(readOnly = true)
    public LeaderboardSnapshot content(UUID contentId, int limit) {
        publicContentQueryService.getPublished(contentId);
        UUID currentUserId = currentActorProvider.getCurrentActor().actorId();
        LeaderboardProjectionStore projectionStore = projectionStoreProvider.getIfAvailable();
        Optional<CachedLeaderboard> cached = projectionStore == null
                ? Optional.empty()
                : projectionStore.findByContent(contentId, currentUserId, limit);
        return cached
                .map(value -> cachedSnapshot(
                        LeaderboardScope.CONTENT, contentId, value
                ))
                .orElseGet(() -> fallbackSnapshot(
                        LeaderboardScope.CONTENT, contentId,
                        xpLeaderboardQuery.findByContent(contentId, currentUserId, limit)
                ));
    }

    private LeaderboardSnapshot cachedSnapshot(
            LeaderboardScope scope, UUID contentId, CachedLeaderboard cached
    ) {
        redisHitCounter.increment();
        return snapshot(
                scope, contentId, cached.result(), LeaderboardDataSource.REDIS,
                cached.generatedAt()
        );
    }

    private LeaderboardSnapshot fallbackSnapshot(
            LeaderboardScope scope, UUID contentId, XpLeaderboardResult result
    ) {
        postgresqlFallbackCounter.increment();
        return snapshot(
                scope, contentId, result, LeaderboardDataSource.POSTGRESQL_FALLBACK,
                null
        );
    }

    private LeaderboardSnapshot snapshot(
            LeaderboardScope scope, UUID contentId, XpLeaderboardResult result,
            LeaderboardDataSource dataSource, Instant projectionGeneratedAt
    ) {
        return new LeaderboardSnapshot(
                scope,
                contentId,
                LeaderboardPeriod.ALL_TIME,
                dataSource,
                projectionGeneratedAt,
                result.participantCount(),
                result.leaders().stream().map(this::entry).toList(),
                result.currentUser() == null ? null : entry(result.currentUser())
        );
    }

    private LeaderboardEntry entry(RankedXpEntry rankedXpEntry) {
        return new LeaderboardEntry(
                rankedXpEntry.position(), rankedXpEntry.userId(),
                rankedXpEntry.totalXp(), rankedXpEntry.firstXpAt()
        );
    }
}
