package com.trt.contentengagement.content.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentReferenceService implements ContentReferenceVerifier {

    private final ContentCatalogRepository contentCatalogRepository;

    public ContentReferenceService(ContentCatalogRepository contentCatalogRepository) {
        this.contentCatalogRepository = contentCatalogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void requireExistingContent(UUID contentId) {
        if (contentCatalogRepository.findById(contentId).isEmpty()) {
            throw new ContentNotFoundException(contentId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ContentVisualReference requirePublishedVisual(UUID contentId) {
        return contentCatalogRepository.findPublishedById(contentId)
                .map(content -> new ContentVisualReference(
                        content.coverMediaId(), content.coverAlternativeText()
                ))
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }
}
