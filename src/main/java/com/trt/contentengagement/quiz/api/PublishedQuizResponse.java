package com.trt.contentengagement.quiz.api;

import java.util.List;

import com.trt.contentengagement.quiz.application.PublishedQuizDetails;

public record PublishedQuizResponse(
        String quizId,
        String contentId,
        String versionId,
        int versionNumber,
        String title,
        String description,
        String scoringPolicyVersion,
        List<QuestionResponse> questions
) {

    public static PublishedQuizResponse from(PublishedQuizDetails details) {
        return new PublishedQuizResponse(
                details.quizId().toString(), details.contentId().toString(),
                details.versionId().toString(), details.versionNumber(), details.title(),
                details.description(), details.scoringPolicyVersion(),
                details.questions().stream().map(QuestionResponse::from).toList()
        );
    }

    public record QuestionResponse(
            String id,
            int questionOrder,
            String prompt,
            String difficulty,
            VisualResponse visual,
            String accessiblePrompt,
            List<OptionResponse> answerOptions
    ) {
        static QuestionResponse from(PublishedQuizDetails.PublishedQuestionDetails details) {
            return new QuestionResponse(
                    details.id().toString(), details.questionOrder(), details.prompt(),
                    details.difficulty(),
                    VisualResponse.from(details.visual()), details.accessiblePrompt(),
                    details.answerOptions().stream().map(OptionResponse::from).toList()
            );
        }
    }

    public record VisualResponse(
            String mediaAssetId, String contentUrl, String role, String alternativeText
    ) {
        static VisualResponse from(PublishedQuizDetails.PublishedVisualDetails details) {
            return new VisualResponse(
                    details.mediaAssetId().toString(), details.contentUrl(), details.role(),
                    details.alternativeText()
            );
        }
    }

    public record OptionResponse(String id, int optionOrder, String text) {
        static OptionResponse from(PublishedQuizDetails.PublishedOptionDetails details) {
            return new OptionResponse(
                    details.id().toString(), details.optionOrder(), details.text()
            );
        }
    }
}
