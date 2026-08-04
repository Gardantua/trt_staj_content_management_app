package com.trt.contentengagement.quiz.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublishedQuizQueryService {

    private final QuizCatalogRepository quizCatalogRepository;

    public PublishedQuizQueryService(QuizCatalogRepository quizCatalogRepository) {
        this.quizCatalogRepository = quizCatalogRepository;
    }

    @Transactional(readOnly = true)
    public PublishedQuizDetails get(UUID quizId) {
        return quizCatalogRepository.findWithPublishedVersionById(quizId)
                .map(PublishedQuizDetails::from)
                .orElseThrow(() -> new QuizNotFoundException(quizId));
    }

    @Transactional(readOnly = true)
    public List<PublishedQuizDetails> listForContent(UUID contentId) {
        return quizCatalogRepository.findWithPublishedVersionByContentId(contentId).stream()
                .map(PublishedQuizDetails::from)
                .toList();
    }
}
