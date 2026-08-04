package com.trt.contentengagement.leaderboard.application;

import java.util.UUID;

import com.trt.contentengagement.content.application.PublicContentQueryService;
import com.trt.contentengagement.gamification.application.RankedXpEntry;
import com.trt.contentengagement.gamification.application.XpLeaderboardQuery;
import com.trt.contentengagement.gamification.application.XpLeaderboardResult;
import com.trt.contentengagement.identity.application.CurrentActorProvider;
import com.trt.contentengagement.leaderboard.domain.LeaderboardPeriod;
import com.trt.contentengagement.leaderboard.domain.LeaderboardScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaderboardQueryService {
    private final XpLeaderboardQuery xpLeaderboardQuery;
    private final CurrentActorProvider currentActorProvider;
    private final PublicContentQueryService publicContentQueryService;

    public LeaderboardQueryService(
            XpLeaderboardQuery xpLeaderboardQuery,
            CurrentActorProvider currentActorProvider,
            PublicContentQueryService publicContentQueryService
    ) {
        this.xpLeaderboardQuery = xpLeaderboardQuery;
        this.currentActorProvider = currentActorProvider;
        this.publicContentQueryService = publicContentQueryService;
    }

    @Transactional(readOnly = true)
    public LeaderboardSnapshot global(int limit) {
        UUID currentUserId = currentActorProvider.getCurrentActor().actorId();
        return snapshot(
                LeaderboardScope.GLOBAL, null,
                xpLeaderboardQuery.findGlobal(currentUserId, limit)
        );
    }

    @Transactional(readOnly = true)
    public LeaderboardSnapshot content(UUID contentId, int limit) {
        publicContentQueryService.getPublished(contentId);
        UUID currentUserId = currentActorProvider.getCurrentActor().actorId();
        return snapshot(
                LeaderboardScope.CONTENT, contentId,
                xpLeaderboardQuery.findByContent(contentId, currentUserId, limit)
        );
    }

    private LeaderboardSnapshot snapshot(
            LeaderboardScope scope, UUID contentId, XpLeaderboardResult result
    ) {
        return new LeaderboardSnapshot(
                scope,
                contentId,
                LeaderboardPeriod.ALL_TIME,
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
