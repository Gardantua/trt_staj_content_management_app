package com.trt.contentengagement.content.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.i18n.LocaleContextHolder;

@Service
public class PublicContentQueryService {

    private final ContentCatalogRepository contentCatalogRepository;
    private final ContentTranslationService contentTranslationService;

    public PublicContentQueryService(ContentCatalogRepository contentCatalogRepository,
                                     ContentTranslationService contentTranslationService) {
        this.contentCatalogRepository = contentCatalogRepository;
        this.contentTranslationService = contentTranslationService;
    }

    @Transactional(readOnly = true)
    public PageResult<ContentSummary> listPublished(int page, int size) {
        return contentTranslationService.localize(contentCatalogRepository.findPublished(page, size), language());
    }

    @Transactional(readOnly = true)
    public ContentDetails getPublished(UUID contentId) {
        ContentDetails source = contentCatalogRepository.findPublishedById(contentId)
                .map(ContentDetails::from)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        return contentTranslationService.localize(source, language());
    }

    private String language() { return LocaleContextHolder.getLocale().getLanguage(); }
}
