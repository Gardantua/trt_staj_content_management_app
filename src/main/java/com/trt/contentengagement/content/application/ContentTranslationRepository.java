package com.trt.contentengagement.content.application;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ContentTranslationRepository {
    Optional<ContentTranslation> find(UUID contentId, String languageCode);
    Map<UUID, ContentTranslation> findAll(Set<UUID> contentIds, String languageCode);
    void save(UUID contentId, String languageCode, ContentTranslation translation);
}
