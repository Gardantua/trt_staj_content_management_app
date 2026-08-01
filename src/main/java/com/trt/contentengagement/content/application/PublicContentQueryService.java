package com.trt.contentengagement.content.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicContentQueryService {

    private final ContentCatalogRepository contentCatalogRepository;

    public PublicContentQueryService(ContentCatalogRepository contentCatalogRepository) {
        this.contentCatalogRepository = contentCatalogRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<ContentSummary> listPublished(int page, int size) {
        return contentCatalogRepository.findPublished(page, size);
    }

    @Transactional(readOnly = true)
    public ContentDetails getPublished(UUID contentId) {
        return contentCatalogRepository.findPublishedById(contentId)
                .map(ContentDetails::from)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }
}
