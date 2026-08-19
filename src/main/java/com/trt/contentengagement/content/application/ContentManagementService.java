package com.trt.contentengagement.content.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.admin.application.AdminAuditLog;
import com.trt.contentengagement.content.domain.Content;
import com.trt.contentengagement.content.domain.ContentType;
import com.trt.contentengagement.identity.application.CurrentActorProvider;
import com.trt.contentengagement.media.application.MediaReferenceVerifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentManagementService {

    private final ContentCatalogRepository contentCatalogRepository;
    private final AdminAuditLog adminAuditLog;
    private final CurrentActorProvider currentActorProvider;
    private final MediaReferenceVerifier mediaReferenceVerifier;
    private final Clock clock;

    public ContentManagementService(
            ContentCatalogRepository contentCatalogRepository,
            AdminAuditLog adminAuditLog,
            CurrentActorProvider currentActorProvider,
            MediaReferenceVerifier mediaReferenceVerifier,
            Clock clock
    ) {
        this.contentCatalogRepository = contentCatalogRepository;
        this.adminAuditLog = adminAuditLog;
        this.currentActorProvider = currentActorProvider;
        this.mediaReferenceVerifier = mediaReferenceVerifier;
        this.clock = clock;
    }

    @Transactional
    public ContentDetails create(String title, String description, ContentType contentType) {
        Instant occurredAt = clock.instant();
        Content content = Content.create(title, description, contentType, occurredAt);
        Content savedContent = contentCatalogRepository.save(content);
        audit("CONTENT_CREATED", "CONTENT", savedContent.id(), occurredAt);
        return ContentDetails.from(savedContent);
    }

    @Transactional(readOnly = true)
    public ContentDetails get(UUID contentId) {
        return ContentDetails.from(requireContent(contentId));
    }

    @Transactional(readOnly = true)
    public PageResult<AdminContentSummary> listForAdministration(
            String titleQuery,
            int page,
            int size
    ) {
        String normalizedTitleQuery = titleQuery == null ? "" : titleQuery.strip();
        return contentCatalogRepository.findAllForAdministration(
                normalizedTitleQuery,
                page,
                size
        );
    }

    @Transactional
    public ContentDetails update(UUID contentId, String title, String description) {
        Instant occurredAt = clock.instant();
        Content content = requireContent(contentId);
        content.updateDetails(title, description, occurredAt);
        Content savedContent = contentCatalogRepository.save(content);
        audit("CONTENT_UPDATED", "CONTENT", contentId, occurredAt);
        return ContentDetails.from(savedContent);
    }

    @Transactional
    public ContentDetails setWatchUrl(UUID contentId, String watchUrl) {
        Instant occurredAt = clock.instant();
        Content content = requireContent(contentId);
        content.setWatchUrl(watchUrl, occurredAt);
        Content savedContent = contentCatalogRepository.save(content);
        audit("CONTENT_WATCH_URL_UPDATED", "CONTENT", contentId, occurredAt);
        return ContentDetails.from(savedContent);
    }

    @Transactional
    public ContentDetails setCover(UUID contentId, UUID mediaAssetId, String alternativeText) {
        mediaReferenceVerifier.requireImage(mediaAssetId);
        Instant occurredAt = clock.instant();
        Content content = requireContent(contentId);
        content.setCover(mediaAssetId, alternativeText, occurredAt);
        Content savedContent = contentCatalogRepository.save(content);
        audit("CONTENT_COVER_SET", "CONTENT", contentId, occurredAt);
        return ContentDetails.from(savedContent);
    }

    @Transactional
    public ContentDetails publish(UUID contentId) {
        Instant occurredAt = clock.instant();
        Content content = requireContent(contentId);
        content.publish(occurredAt);
        Content savedContent = contentCatalogRepository.save(content);
        audit("CONTENT_PUBLISHED", "CONTENT", contentId, occurredAt);
        return ContentDetails.from(savedContent);
    }

    @Transactional
    public void delete(UUID contentId) {
        Instant occurredAt = clock.instant();
        Content content = requireContent(contentId);
        contentCatalogRepository.delete(content);
        audit("CONTENT_DELETED", "CONTENT", contentId, occurredAt);
    }

    private Content requireContent(UUID contentId) {
        return contentCatalogRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }

    private void audit(String action, String resourceType, UUID resourceId, Instant occurredAt) {
        adminAuditLog.record(
                currentActorProvider.getCurrentActor().actorId(),
                action,
                resourceType,
                resourceId,
                occurredAt
        );
    }
}
