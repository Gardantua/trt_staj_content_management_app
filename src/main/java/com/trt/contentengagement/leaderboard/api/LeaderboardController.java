package com.trt.contentengagement.leaderboard.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.leaderboard.application.LeaderboardEntry;
import com.trt.contentengagement.leaderboard.application.LeaderboardQueryService;
import com.trt.contentengagement.leaderboard.application.LeaderboardSnapshot;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/leaderboards")
public class LeaderboardController {
    private final LeaderboardQueryService leaderboardQueryService;

    public LeaderboardController(LeaderboardQueryService leaderboardQueryService) {
        this.leaderboardQueryService = leaderboardQueryService;
    }

    @GetMapping("/global")
    public LeaderboardResponse global(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return LeaderboardResponse.from(leaderboardQueryService.global(limit));
    }

    @GetMapping("/contents/{contentId}")
    public LeaderboardResponse content(
            @PathVariable UUID contentId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return LeaderboardResponse.from(leaderboardQueryService.content(contentId, limit));
    }

    public record LeaderboardResponse(
            String scope,
            UUID contentId,
            String period,
            String dataSource,
            Instant projectionGeneratedAt,
            long participantCount,
            List<LeaderboardEntryResponse> leaders,
            LeaderboardEntryResponse currentUser
    ) {
        static LeaderboardResponse from(LeaderboardSnapshot snapshot) {
            UUID currentUserId = snapshot.currentUser() == null
                    ? null : snapshot.currentUser().userId();
            return new LeaderboardResponse(
                    snapshot.scope().name(),
                    snapshot.contentId(),
                    snapshot.period().name(),
                    snapshot.dataSource().name(),
                    snapshot.projectionGeneratedAt(),
                    snapshot.participantCount(),
                    snapshot.leaders().stream()
                            .map(entry -> LeaderboardEntryResponse.from(entry, currentUserId))
                            .toList(),
                    snapshot.currentUser() == null
                            ? null
                            : LeaderboardEntryResponse.from(snapshot.currentUser(), currentUserId)
            );
        }
    }

    public record LeaderboardEntryResponse(
            long position,
            UUID userId,
            long totalXp,
            Instant firstXpAt,
            boolean currentUser
    ) {
        static LeaderboardEntryResponse from(
                LeaderboardEntry entry, UUID currentUserId
        ) {
            return new LeaderboardEntryResponse(
                    entry.position(), entry.userId(), entry.totalXp(), entry.firstXpAt(),
                    entry.userId().equals(currentUserId)
            );
        }
    }
}
