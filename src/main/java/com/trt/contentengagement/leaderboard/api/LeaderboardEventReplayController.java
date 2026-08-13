package com.trt.contentengagement.leaderboard.api;

import com.trt.contentengagement.gamification.application.XpService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/leaderboards")
@PreAuthorize("hasRole('ADMIN')")
public class LeaderboardEventReplayController {
    private final XpService xpService;

    public LeaderboardEventReplayController(XpService xpService) {
        this.xpService = xpService;
    }

    @PostMapping("/replay-xp-events")
    public ReplayResult replayXpEvents() {
        return new ReplayResult(xpService.replayLeaderboardEvents());
    }

    public record ReplayResult(int processedTransactions) {
    }
}
