package com.trt.contentengagement.leaderboard.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.trt.contentengagement.identity.application.AccountDisplayNameDirectory;
import com.trt.contentengagement.leaderboard.application.LeaderboardEntry;
import com.trt.contentengagement.leaderboard.application.LeaderboardReadFacade;
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
    private final LeaderboardReadFacade leaderboardReadFacade;
    private final AccountDisplayNameDirectory accountDisplayNameDirectory;

    public LeaderboardController(
            LeaderboardReadFacade leaderboardReadFacade,
            AccountDisplayNameDirectory accountDisplayNameDirectory
    ) {
        this.leaderboardReadFacade = leaderboardReadFacade;
        this.accountDisplayNameDirectory = accountDisplayNameDirectory;
    }

    @GetMapping("/global")
    public LeaderboardResponse global(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return responseFrom(leaderboardReadFacade.global(limit));
    }

    @GetMapping("/contents/{contentId}")
    public LeaderboardResponse content(
            @PathVariable UUID contentId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return responseFrom(leaderboardReadFacade.content(contentId, limit));
    }

    private LeaderboardResponse responseFrom(LeaderboardSnapshot snapshot) {
        Set<UUID> accountIds = snapshot.leaders().stream()
                .map(LeaderboardEntry::userId)
                .collect(Collectors.toSet());
        if (snapshot.currentUser() != null) {
            accountIds.add(snapshot.currentUser().userId());
        }
        Map<UUID, String> displayNames = accountDisplayNameDirectory.findDisplayNames(accountIds);
        return LeaderboardResponse.from(snapshot, displayNames);
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
        static LeaderboardResponse from(
                LeaderboardSnapshot snapshot, Map<UUID, String> displayNames
        ) {
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
                            .map(entry -> LeaderboardEntryResponse.from(
                                    entry, currentUserId, displayNames.get(entry.userId())
                            ))
                            .toList(),
                    snapshot.currentUser() == null
                            ? null
                            : LeaderboardEntryResponse.from(
                                    snapshot.currentUser(), currentUserId,
                                    displayNames.get(snapshot.currentUser().userId())
                            )
            );
        }
    }

    public record LeaderboardEntryResponse(
            long position,
            UUID userId,
            String displayName,
            long totalXp,
            Instant firstXpAt,
            boolean currentUser
    ) {
        static LeaderboardEntryResponse from(
                LeaderboardEntry entry, UUID currentUserId, String displayName
        ) {
            return new LeaderboardEntryResponse(
                    entry.position(), entry.userId(), displayName,
                    entry.totalXp(), entry.firstXpAt(),
                    entry.userId().equals(currentUserId)
            );
        }
    }
}
