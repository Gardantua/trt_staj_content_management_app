package com.trt.contentengagement.quiz.api;

import com.trt.contentengagement.quiz.application.PublishedQuizDetails;

public record PublishedQuizSummaryResponse(
        String quizId,
        String contentId,
        String scopeType,
        String seasonId,
        String episodeId,
        String title,
        String description,
        int questionCount
) {

    public static PublishedQuizSummaryResponse from(PublishedQuizDetails details) {
        return new PublishedQuizSummaryResponse(
                details.quizId().toString(),
                details.contentId().toString(),
                details.scopeType(),
                details.seasonId() == null ? null : details.seasonId().toString(),
                details.episodeId() == null ? null : details.episodeId().toString(),
                details.title(),
                details.description(),
                details.questions().size()
        );
    }
}
