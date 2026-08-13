package com.trt.contentengagement.quiz.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.quiz.domain.Quiz;

public interface QuizCatalogRepository {

    Quiz save(Quiz quiz);

    Optional<Quiz> findById(UUID quizId);

    List<Quiz> findByContentId(UUID contentId);

    Optional<Quiz> findWithPublishedVersionById(UUID quizId);

    List<Quiz> findWithPublishedVersionByContentId(UUID contentId);

    List<Quiz> findAllWithPublishedVersion();

    Optional<Quiz> findByVersionId(UUID versionId);

    void delete(Quiz quiz);
}
