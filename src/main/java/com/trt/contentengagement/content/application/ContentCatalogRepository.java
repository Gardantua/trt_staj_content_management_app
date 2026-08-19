package com.trt.contentengagement.content.application;

import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.content.domain.Content;

public interface ContentCatalogRepository {

    Content save(Content content);

    Optional<Content> findById(UUID contentId);

    Optional<Content> findPublishedById(UUID contentId);

    PageResult<AdminContentSummary> findAllForAdministration(
            String normalizedTitleQuery,
            WatchLinkFilter watchLinkFilter,
            int page,
            int size
    );

    PageResult<ContentSummary> findPublished(int page, int size);

    void delete(Content content);
}
