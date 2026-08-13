package com.trt.contentengagement.leaderboardservice.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.leaderboardservice.application.LeaderboardQueryService;
import com.trt.contentengagement.leaderboardservice.application.LeaderboardView;
import com.trt.contentengagement.leaderboardservice.domain.LeaderboardEntry;
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
@RequestMapping("/internal/v1/leaderboards")
public class InternalLeaderboardController {
    private final LeaderboardQueryService queryService;

    public InternalLeaderboardController(LeaderboardQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/global")
    public LeaderboardResponse global(
            @RequestParam UUID currentUserId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return LeaderboardResponse.from(queryService.global(currentUserId, limit));
    }

    @GetMapping("/contents/{contentId}")
    public LeaderboardResponse content(
            @PathVariable UUID contentId,
            @RequestParam UUID currentUserId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return LeaderboardResponse.from(queryService.content(contentId, currentUserId, limit));
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
        static LeaderboardResponse from(LeaderboardView view) {
            UUID currentUserId = view.result().currentUser() == null
                    ? null : view.result().currentUser().userId();
            return new LeaderboardResponse(
                    view.scope(), view.contentId(), view.period(), view.dataSource(),
                    view.projectionGeneratedAt(), view.result().participantCount(),
                    view.result().leaders().stream()
                            .map(entry -> LeaderboardEntryResponse.from(entry, currentUserId))
                            .toList(),
                    view.result().currentUser() == null
                            ? null
                            : LeaderboardEntryResponse.from(
                                    view.result().currentUser(), currentUserId
                            )
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
        static LeaderboardEntryResponse from(LeaderboardEntry entry, UUID currentUserId) {
            return new LeaderboardEntryResponse(
                    entry.position(), entry.userId(), entry.totalXp(), entry.firstXpAt(),
                    entry.userId().equals(currentUserId)
            );
        }
    }
}
