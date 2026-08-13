package com.trt.contentengagement.quiz.api;

import java.time.Instant;
import java.util.List;

import com.trt.contentengagement.quiz.application.QuizDetails;

public record AdminQuizResponse(
        String id,
        String contentId,
        String scopeType,
        String seasonId,
        String episodeId,
        Instant createdAt,
        Instant updatedAt,
        List<VersionResponse> versions
) {

    public static AdminQuizResponse from(QuizDetails details) {
        return new AdminQuizResponse(
                details.id().toString(), details.contentId().toString(), details.scopeType(),
                details.seasonId() == null ? null : details.seasonId().toString(),
                details.episodeId() == null ? null : details.episodeId().toString(),
                details.createdAt(), details.updatedAt(),
                details.versions().stream().map(VersionResponse::from).toList()
        );
    }

    public record VersionResponse(
            String id,
            int versionNumber,
            String title,
            String description,
            String status,
            String scoringPolicyVersion,
            Instant createdAt,
            Instant publishedAt,
            Instant archivedAt,
            String fallbackMediaId,
            String fallbackAlternativeText,
            List<QuestionResponse> questions
    ) {
        static VersionResponse from(QuizDetails.VersionDetails details) {
            return new VersionResponse(
                    details.id().toString(), details.versionNumber(), details.title(),
                    details.description(), details.status(), details.scoringPolicyVersion(),
                    details.createdAt(), details.publishedAt(), details.archivedAt(),
                    details.fallbackMediaId() == null
                            ? null : details.fallbackMediaId().toString(),
                    details.fallbackAlternativeText(),
                    details.questions().stream().map(QuestionResponse::from).toList()
            );
        }
    }

    public record QuestionResponse(
            String id,
            int questionOrder,
            String prompt,
            String difficulty,
            String visualMediaId,
            String visualRole,
            String visualAlternativeText,
            String accessiblePrompt,
            List<OptionResponse> answerOptions
    ) {
        static QuestionResponse from(QuizDetails.QuestionDetails details) {
            return new QuestionResponse(
                    details.id().toString(), details.questionOrder(), details.prompt(),
                    details.difficulty(),
                    details.visualMediaId() == null
                            ? null : details.visualMediaId().toString(),
                    details.visualRole(), details.visualAlternativeText(),
                    details.accessiblePrompt(),
                    details.answerOptions().stream().map(OptionResponse::from).toList()
            );
        }
    }

    public record OptionResponse(
            String id,
            int optionOrder,
            String text,
            boolean correct
    ) {
        static OptionResponse from(QuizDetails.OptionDetails details) {
            return new OptionResponse(
                    details.id().toString(), details.optionOrder(), details.text(), details.correct()
            );
        }
    }
}
