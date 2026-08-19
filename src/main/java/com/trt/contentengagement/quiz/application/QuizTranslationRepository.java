package com.trt.contentengagement.quiz.application;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface QuizTranslationRepository {
    Optional<QuizTranslation> find(UUID versionId, String languageCode);
    Map<UUID, QuizTranslation> findAll(Set<UUID> versionIds, String languageCode);
    void save(UUID versionId, String languageCode, QuizTranslation translation);
}
