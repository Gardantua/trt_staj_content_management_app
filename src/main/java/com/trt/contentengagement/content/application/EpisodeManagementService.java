package com.trt.contentengagement.content.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.content.domain.Content;
import com.trt.contentengagement.content.domain.Episode;
import com.trt.contentengagement.identity.application.CurrentActorProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EpisodeManagementService {

    private final ContentCatalogRepository contentCatalogRepository;
    private final AdminAuditLog adminAuditLog;
    private final CurrentActorProvider currentActorProvider;
    private final Clock clock;

    public EpisodeManagementService(
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
    public ContentDetails add(
            UUID contentId,
            UUID seasonId,
            int episodeNumber,
            String title,
            String description
    ) {
        Instant occurredAt = clock.instant();
        Content content = requireContent(contentId);
        Episode episode = content.addEpisode(
                seasonId,
                episodeNumber,
                title,
                description,
                occurredAt
        );
        Content savedContent = contentCatalogRepository.save(content);
        audit("EPISODE_CREATED", episode.id(), occurredAt);
        return ContentDetails.from(savedContent);
    }

    @Transactional
    public ContentDetails update(
            UUID contentId,
            UUID seasonId,
            UUID episodeId,
            int episodeNumber,
            String title,
            String description
    ) {
        Instant occurredAt = clock.instant();
        Content content = requireContent(contentId);
        content.updateEpisode(
                seasonId,
                episodeId,
                episodeNumber,
                title,
                description,
                occurredAt
        );
        Content savedContent = contentCatalogRepository.save(content);
        audit("EPISODE_UPDATED", episodeId, occurredAt);
        return ContentDetails.from(savedContent);
    }

    @Transactional
    public void delete(UUID contentId, UUID seasonId, UUID episodeId) {
        Instant occurredAt = clock.instant();
        Content content = requireContent(contentId);
        content.removeEpisode(seasonId, episodeId, occurredAt);
        contentCatalogRepository.save(content);
        audit("EPISODE_DELETED", episodeId, occurredAt);
    }

    private Content requireContent(UUID contentId) {
        return contentCatalogRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }

    private void audit(String action, UUID resourceId, Instant occurredAt) {
        adminAuditLog.record(
                currentActorProvider.getCurrentActor().actorId(),
                action,
                "EPISODE",
                resourceId,
                occurredAt
        );
    }
}
