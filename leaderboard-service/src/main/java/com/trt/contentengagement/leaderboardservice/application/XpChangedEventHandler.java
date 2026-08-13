package com.trt.contentengagement.leaderboardservice.application;

import java.time.Clock;

import com.trt.contentengagement.leaderboardservice.domain.XpChangedEventV1;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class XpChangedEventHandler {
    private final LeaderboardProjectionRepository projectionRepository;
    private final Clock clock;

    public XpChangedEventHandler(
            LeaderboardProjectionRepository projectionRepository,
            Clock clock
    ) {
        this.projectionRepository = projectionRepository;
        this.clock = clock;
    }

    /** Returns false for an already consumed event; no second XP row is created. */
    @Transactional
    public boolean handle(XpChangedEventV1 event) {
        return projectionRepository.appendIfAbsent(event, clock.instant());
    }
}
