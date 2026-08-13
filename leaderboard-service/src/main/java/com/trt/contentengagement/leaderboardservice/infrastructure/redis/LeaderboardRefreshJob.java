package com.trt.contentengagement.leaderboardservice.infrastructure.redis;

import com.trt.contentengagement.leaderboardservice.application.LeaderboardRefreshService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.leaderboard.refresh-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class LeaderboardRefreshJob {
    private final LeaderboardRefreshService refreshService;

    public LeaderboardRefreshJob(LeaderboardRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @Scheduled(
            fixedDelayString = "${app.leaderboard.refresh-delay:5s}",
            initialDelayString = "${app.leaderboard.refresh-delay:5s}"
    )
    public void refresh() {
        refreshService.refresh();
    }
}
