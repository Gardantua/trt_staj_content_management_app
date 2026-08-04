package com.trt.contentengagement.quiz.api;

import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.quiz.application.PublishedQuizQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PublishedQuizController {

    private final PublishedQuizQueryService publishedQuizQueryService;

    public PublishedQuizController(PublishedQuizQueryService publishedQuizQueryService) {
        this.publishedQuizQueryService = publishedQuizQueryService;
    }

    @GetMapping("/quizzes/{quizId}")
    public PublishedQuizResponse getPublishedQuiz(@PathVariable UUID quizId) {
        return PublishedQuizResponse.from(publishedQuizQueryService.get(quizId));
    }

    @GetMapping("/contents/{contentId}/quizzes")
    public List<PublishedQuizResponse> listPublishedQuizzes(@PathVariable UUID contentId) {
        return publishedQuizQueryService.listForContent(contentId).stream()
                .map(PublishedQuizResponse::from)
                .toList();
    }
}
