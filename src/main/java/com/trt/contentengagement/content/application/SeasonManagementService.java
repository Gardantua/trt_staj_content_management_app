package com.trt.contentengagement.content.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.content.domain.Content;
import com.trt.contentengagement.content.domain.Season;
import com.trt.contentengagement.identity.application.CurrentActorProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeasonManagementService {

    private final ContentCatalogRepository contentCatalogRepository;
    private final AdminAuditLog adminAuditLog;
    private final CurrentActorProvider currentActorProvider;
    private final Clock clock;

    public SeasonManagementService(
            ContentCatalogRepository contentCatalogRepository,
            AdminAuditLog adminAuditLog,
            CurrentActorProvider currentActorProvider,
            Clock clock
    ) {
        this.contentCatalogRepository = contentCatalogRepository;
        this.adminAuditLog = adminAuditLog;
        this.currentActorProvider = currentActorProvider;
        this.clock = clock;
    }

    @Transactional
    public ContentDetails add(UUID contentId, int seasonNumber, String title) {
        Instant occurredAt = clock.instant();
        Content content = requireContent(contentId);
        Season season = content.addSeason(seasonNumber, title, occurredAt);
        Content savedContent = contentCatalogRepository.save(content);
        audit("SEASON_CREATED", season.id(), occurredAt);
        return ContentDetails.from(savedContent);
    }

    @Transactional
    public ContentDetails update(
            UUID contentId,
            UUID seasonId,
            int seasonNumber,
            String title
    ) {
        Instant occurredAt = clock.instant();
        Content content = requireContent(contentId);
        content.updateSeason(seasonId, seasonNumber, title, occurredAt);
        Content savedContent = contentCatalogRepository.save(content);
        audit("SEASON_UPDATED", seasonId, occurredAt);
        return ContentDetails.from(savedContent);
    }

    @Transactional
    public void delete(UUID contentId, UUID seasonId) {
        Instant occurredAt = clock.instant();
        Content content = requireContent(contentId);
        content.removeSeason(seasonId, occurredAt);
        contentCatalogRepository.save(content);
        audit("SEASON_DELETED", seasonId, occurredAt);
    }

    private Content requireContent(UUID contentId) {
        return contentCatalogRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }

    private void audit(String action, UUID resourceId, Instant occurredAt) {
        adminAuditLog.record(
                currentActorProvider.getCurrentActor().actorId(),
                action,
                "SEASON",
                resourceId,
                occurredAt
        );
    }
}
