package com.trt.contentengagement.leaderboard.api;

import com.trt.contentengagement.leaderboard.application.LeaderboardProjectionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/leaderboards")
@PreAuthorize("hasRole('ADMIN')")
@ConditionalOnProperty(name = "app.leaderboard.redis-enabled", havingValue = "true")
public class AdminLeaderboardController {
    private final LeaderboardProjectionService projectionService;

    public AdminLeaderboardController(LeaderboardProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @PostMapping("/rebuild")
    public LeaderboardProjectionService.ProjectionRebuildResult rebuild() {
        return projectionService.rebuild();
    }
}
