package com.trt.contentengagement.quiz.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.i18n.LocaleContextHolder;

@Service
public class PublishedQuizQueryService {

    private final QuizCatalogRepository quizCatalogRepository;
    private final QuizTranslationService quizTranslationService;

    public PublishedQuizQueryService(QuizCatalogRepository quizCatalogRepository,
                                     QuizTranslationService quizTranslationService) {
        this.quizCatalogRepository = quizCatalogRepository;
        this.quizTranslationService = quizTranslationService;
    }

    @Transactional(readOnly = true)
    public PublishedQuizDetails get(UUID quizId) {
        return quizCatalogRepository.findWithPublishedVersionById(quizId)
                .map(PublishedQuizDetails::from)
                .map(details -> quizTranslationService.localize(details, language()))
                .orElseThrow(() -> new QuizNotFoundException(quizId));
    }

    @Transactional(readOnly = true)
    public List<PublishedQuizDetails> listForContent(UUID contentId) {
        return quizCatalogRepository.findWithPublishedVersionByContentId(contentId).stream()
                .map(PublishedQuizDetails::from)
                .map(details -> quizTranslationService.localize(details, language()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublishedQuizDetails> listAll() {
        return quizCatalogRepository.findAllWithPublishedVersion().stream()
                .map(PublishedQuizDetails::from)
                .map(details -> quizTranslationService.localize(details, language()))
                .toList();
    }

    private String language() { return LocaleContextHolder.getLocale().getLanguage(); }
}
