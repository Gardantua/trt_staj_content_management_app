package com.trt.contentengagement.leaderboard.application;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.leaderboard.domain.LeaderboardPeriod;
import com.trt.contentengagement.leaderboard.domain.LeaderboardScope;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.leaderboard.remote-enabled", havingValue = "true")
public class RemoteLeaderboardClient {
    private final RestClient restClient;

    public RemoteLeaderboardClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.leaderboard.service-base-url:http://localhost:8082}") String baseUrl
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .build();
    }

    public LeaderboardSnapshot global(UUID currentUserId, int limit) {
        return requireResponse(restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/v1/leaderboards/global")
                        .queryParam("currentUserId", currentUserId)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(RemoteLeaderboardResponse.class));
    }

    public LeaderboardSnapshot content(UUID contentId, UUID currentUserId, int limit) {
        return requireResponse(restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/v1/leaderboards/contents/{contentId}")
                        .queryParam("currentUserId", currentUserId)
                        .queryParam("limit", limit)
                        .build(contentId))
                .retrieve()
                .body(RemoteLeaderboardResponse.class));
    }

    private LeaderboardSnapshot requireResponse(RemoteLeaderboardResponse response) {
        if (response == null) {
            throw new IllegalStateException("Leaderboard service returned an empty response.");
        }
        return new LeaderboardSnapshot(
                LeaderboardScope.valueOf(response.scope()),
                response.contentId(),
                LeaderboardPeriod.valueOf(response.period()),
                LeaderboardDataSource.valueOf(response.dataSource()),
                response.projectionGeneratedAt(),
                response.participantCount(),
                response.leaders().stream().map(RemoteLeaderboardEntry::toEntry).toList(),
                response.currentUser() == null ? null : response.currentUser().toEntry()
        );
    }

    private record RemoteLeaderboardResponse(
            String scope,
            UUID contentId,
            String period,
            String dataSource,
            Instant projectionGeneratedAt,
            long participantCount,
            List<RemoteLeaderboardEntry> leaders,
            RemoteLeaderboardEntry currentUser
    ) {
    }

    private record RemoteLeaderboardEntry(
            long position,
            UUID userId,
            long totalXp,
            Instant firstXpAt,
            boolean currentUser
    ) {
        LeaderboardEntry toEntry() {
            return new LeaderboardEntry(position, userId, totalXp, firstXpAt);
        }
    }
}
