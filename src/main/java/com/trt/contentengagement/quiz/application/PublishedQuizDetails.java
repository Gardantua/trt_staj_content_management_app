package com.trt.contentengagement.quiz.application;

import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.quiz.domain.AnswerOption;
import com.trt.contentengagement.quiz.domain.Question;
import com.trt.contentengagement.quiz.domain.Quiz;
import com.trt.contentengagement.quiz.domain.QuizVersion;

public record PublishedQuizDetails(
        UUID quizId,
        UUID contentId,
        UUID versionId,
        int versionNumber,
        String title,
        String description,
        String scoringPolicyVersion,
        List<PublishedQuestionDetails> questions
) {

    public static PublishedQuizDetails from(Quiz quiz) {
        QuizVersion publishedVersion = quiz.publishedVersion();
        return new PublishedQuizDetails(
                quiz.id(), quiz.contentId(), publishedVersion.id(),
                publishedVersion.versionNumber(), publishedVersion.title(),
                publishedVersion.description(), publishedVersion.scoringPolicyVersion().name(),
                publishedVersion.questions().stream()
                        .map(question -> PublishedQuestionDetails.from(question, publishedVersion))
                        .toList()
        );
    }

    public record PublishedQuestionDetails(
            UUID id,
            int questionOrder,
            String prompt,
            String difficulty,
            PublishedVisualDetails visual,
            String accessiblePrompt,
            List<PublishedOptionDetails> answerOptions
    ) {
        static PublishedQuestionDetails from(Question question, QuizVersion version) {
            return new PublishedQuestionDetails(
                    question.id(), question.questionOrder(), question.prompt(),
                    question.difficulty().name(),
                    PublishedVisualDetails.from(question, version), question.accessiblePrompt(),
                    question.answerOptions().stream().map(PublishedOptionDetails::from).toList()
            );
        }
    }

    public record PublishedVisualDetails(
            UUID mediaAssetId, String contentUrl, String role, String alternativeText
    ) {
        static PublishedVisualDetails from(Question question, QuizVersion version) {
            if (question.visualMediaId() == null) {
                return new PublishedVisualDetails(
                        version.fallbackMediaId(),
                        "/api/v1/media/" + version.fallbackMediaId() + "/content",
                        "DECORATIVE", null
                );
            }
            return new PublishedVisualDetails(
                    question.visualMediaId(),
                    "/api/v1/media/" + question.visualMediaId() + "/content",
                    question.visualRole().name(), question.visualAlternativeText()
            );
        }
    }

    public record PublishedOptionDetails(UUID id, int optionOrder, String text) {
        static PublishedOptionDetails from(AnswerOption option) {
            return new PublishedOptionDetails(option.id(), option.optionOrder(), option.text());
        }
    }
}
