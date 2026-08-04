package com.trt.contentengagement.leaderboard.infrastructure.redis;

import com.trt.contentengagement.leaderboard.application.LeaderboardProjectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = {"app.leaderboard.redis-enabled", "app.leaderboard.refresh-enabled"},
        havingValue = "true"
)
public class LeaderboardProjectionRefreshJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            LeaderboardProjectionRefreshJob.class
    );

    private final LeaderboardProjectionService projectionService;

    public LeaderboardProjectionRefreshJob(
            LeaderboardProjectionService projectionService
    ) {
        this.projectionService = projectionService;
    }

    @Scheduled(fixedDelayString = "${app.leaderboard.refresh-delay:5s}")
    public void refresh() {
        var result = projectionService.rebuild();
        if (!result.replaced()) {
            LOGGER.warn("Leaderboard Redis projection refresh failed; PostgreSQL fallback remains active.");
        }
    }
}
