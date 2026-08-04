package com.trt.contentengagement.leaderboard.application;

import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.leaderboard.redis-enabled", havingValue = "true")
public class LeaderboardProjectionService {
    private final LeaderboardProjectionSnapshotLoader snapshotLoader;
    private final LeaderboardProjectionStore projectionStore;
    private final Clock clock;

    public LeaderboardProjectionService(
            LeaderboardProjectionSnapshotLoader snapshotLoader,
            LeaderboardProjectionStore projectionStore,
            Clock clock
    ) {
        this.snapshotLoader = snapshotLoader;
        this.projectionStore = projectionStore;
        this.clock = clock;
    }

    public ProjectionRebuildResult rebuild() {
        var snapshot = snapshotLoader.load();
        Instant generatedAt = clock.instant();
        boolean replaced = projectionStore.replaceAll(
                snapshot.globalEntries(), snapshot.contentEntries(), generatedAt
        );
        return new ProjectionRebuildResult(
                replaced, generatedAt, snapshot.globalEntries().size(),
                snapshot.contentEntries().size()
        );
    }

    public record ProjectionRebuildResult(
            boolean replaced,
            Instant generatedAt,
            int globalParticipantCount,
            int contentScopeCount
    ) {
    }
}
