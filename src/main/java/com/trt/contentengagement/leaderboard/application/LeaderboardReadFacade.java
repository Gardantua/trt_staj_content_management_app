package com.trt.contentengagement.leaderboard.application;

import java.util.UUID;

import com.trt.contentengagement.content.application.PublicContentQueryService;
import com.trt.contentengagement.identity.application.CurrentActorProvider;
import org.springframework.beans.factory.ObjectProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/** Keeps the public API stable while the remote service is introduced behind a flag. */
@Service
public class LeaderboardReadFacade {
    private final LeaderboardQueryService localQueryService;
    private final ObjectProvider<RemoteLeaderboardClient> remoteClientProvider;
    private final CurrentActorProvider currentActorProvider;
    private final PublicContentQueryService publicContentQueryService;
    private final Counter remoteFallbackCounter;

    public LeaderboardReadFacade(
            LeaderboardQueryService localQueryService,
            ObjectProvider<RemoteLeaderboardClient> remoteClientProvider,
            CurrentActorProvider currentActorProvider,
            PublicContentQueryService publicContentQueryService,
            MeterRegistry meterRegistry
    ) {
        this.localQueryService = localQueryService;
        this.remoteClientProvider = remoteClientProvider;
        this.currentActorProvider = currentActorProvider;
        this.publicContentQueryService = publicContentQueryService;
        this.remoteFallbackCounter = meterRegistry.counter("leaderboard.remote.fallback");
    }

    public LeaderboardSnapshot global(int limit) {
        RemoteLeaderboardClient remoteClient = remoteClientProvider.getIfAvailable();
        if (remoteClient == null) {
            return localQueryService.global(limit);
        }
        UUID currentUserId = currentActorProvider.getCurrentActor().actorId();
        try {
            return remoteClient.global(currentUserId, limit);
        } catch (RestClientException remoteFailure) {
            remoteFallbackCounter.increment();
            return localQueryService.global(limit);
        }
    }

    public LeaderboardSnapshot content(UUID contentId, int limit) {
        RemoteLeaderboardClient remoteClient = remoteClientProvider.getIfAvailable();
        if (remoteClient == null) {
            return localQueryService.content(contentId, limit);
        }
        publicContentQueryService.getPublished(contentId);
        UUID currentUserId = currentActorProvider.getCurrentActor().actorId();
        try {
            return remoteClient.content(contentId, currentUserId, limit);
        } catch (RestClientException remoteFailure) {
            remoteFallbackCounter.increment();
            return localQueryService.content(contentId, limit);
        }
    }
}
