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
        List<PublishedQuizDetails> sources = quizCatalogRepository.findWithPublishedVersionByContentId(contentId).stream()
                .map(PublishedQuizDetails::from)
                .toList();
        return quizTranslationService.localize(sources, language());
    }

    @Transactional(readOnly = true)
    public List<PublishedQuizDetails> listAll() {
        List<PublishedQuizDetails> sources = quizCatalogRepository.findAllWithPublishedVersion().stream()
                .map(PublishedQuizDetails::from)
                .toList();
        return quizTranslationService.localize(sources, language());
    }

    private String language() { return LocaleContextHolder.getLocale().getLanguage(); }
}
