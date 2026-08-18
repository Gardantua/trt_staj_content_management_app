package com.trt.contentengagement.content.application;

import java.util.Optional;
import java.util.UUID;

public interface ContentTranslationRepository {
    Optional<ContentTranslation> find(UUID contentId, String languageCode);
    void save(UUID contentId, String languageCode, ContentTranslation translation);
}
