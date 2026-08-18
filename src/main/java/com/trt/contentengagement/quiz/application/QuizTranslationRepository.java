package com.trt.contentengagement.quiz.application;

import java.util.Optional;
import java.util.UUID;

public interface QuizTranslationRepository {
    Optional<QuizTranslation> find(UUID versionId, String languageCode);
    void save(UUID versionId, String languageCode, QuizTranslation translation);
}
