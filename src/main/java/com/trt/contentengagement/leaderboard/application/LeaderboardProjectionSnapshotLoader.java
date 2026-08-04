package com.trt.contentengagement.leaderboard.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.trt.contentengagement.gamification.application.RankedXpEntry;
import com.trt.contentengagement.gamification.application.XpLeaderboardQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.leaderboard.redis-enabled", havingValue = "true")
public class LeaderboardProjectionSnapshotLoader {
    private final XpLeaderboardQuery xpLeaderboardQuery;

    public LeaderboardProjectionSnapshotLoader(XpLeaderboardQuery xpLeaderboardQuery) {
        this.xpLeaderboardQuery = xpLeaderboardQuery;
    }

    @Transactional(readOnly = true)
    public ProjectionSnapshot load() {
        List<RankedXpEntry> globalEntries = xpLeaderboardQuery.findAllGlobal();
        Map<UUID, List<RankedXpEntry>> contentEntries = new LinkedHashMap<>();
        for (UUID contentId : xpLeaderboardQuery.findRankedContentIds()) {
            contentEntries.put(contentId, xpLeaderboardQuery.findAllByContent(contentId));
        }
        return new ProjectionSnapshot(globalEntries, contentEntries);
    }

    public record ProjectionSnapshot(
            List<RankedXpEntry> globalEntries,
            Map<UUID, List<RankedXpEntry>> contentEntries
    ) {
        public ProjectionSnapshot {
            globalEntries = List.copyOf(globalEntries);
            contentEntries = Map.copyOf(contentEntries);
        }
    }
}
