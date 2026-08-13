package com.trt.contentengagement.leaderboardservice.application;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.trt.contentengagement.leaderboardservice.domain.LeaderboardEntry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaderboardRefreshService {
    private final LeaderboardProjectionRepository projectionRepository;
    private final ObjectProvider<LeaderboardCache> cacheProvider;
    private final Clock clock;

    public LeaderboardRefreshService(
            LeaderboardProjectionRepository projectionRepository,
            ObjectProvider<LeaderboardCache> cacheProvider,
            Clock clock
    ) {
        this.projectionRepository = projectionRepository;
        this.cacheProvider = cacheProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public boolean refresh() {
        LeaderboardCache cache = cacheProvider.getIfAvailable();
        if (cache == null) {
            return false;
        }
        List<LeaderboardEntry> global = projectionRepository.findAllGlobal();
        Map<UUID, List<LeaderboardEntry>> contents = new LinkedHashMap<>();
        for (UUID contentId : projectionRepository.findContentIds()) {
            contents.put(contentId, projectionRepository.findAllByContent(contentId));
        }
        cache.replace(global, contents, clock.instant());
        return true;
    }
}
