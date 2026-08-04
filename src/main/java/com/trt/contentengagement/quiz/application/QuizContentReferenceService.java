package com.trt.contentengagement.quiz.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizContentReferenceService implements QuizContentReferenceProvider {
    private final QuizCatalogRepository quizCatalogRepository;

    public QuizContentReferenceService(QuizCatalogRepository quizCatalogRepository) {
        this.quizCatalogRepository = quizCatalogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UUID requireContentId(UUID quizId) {
        return quizCatalogRepository.findById(quizId)
                .map(quiz -> quiz.contentId())
                .orElseThrow(() -> new QuizNotFoundException(quizId));
    }
}
