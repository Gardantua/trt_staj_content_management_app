package com.trt.contentengagement.quiz.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.quiz.domain.AnswerOption;
import com.trt.contentengagement.quiz.domain.Question;
import com.trt.contentengagement.quiz.domain.Quiz;
import com.trt.contentengagement.quiz.domain.QuizVersion;

public record QuizDetails(
        UUID id,
        UUID contentId,
        String scopeType,
        UUID seasonId,
        UUID episodeId,
        Instant createdAt,
        Instant updatedAt,
        List<VersionDetails> versions
) {

    public static QuizDetails from(Quiz quiz) {
        return new QuizDetails(
                quiz.id(), quiz.contentId(), quiz.scopeType().name(),
                quiz.seasonId(), quiz.episodeId(), quiz.createdAt(), quiz.updatedAt(),
                quiz.versions().stream().map(VersionDetails::from).toList()
        );
    }

    public record VersionDetails(
            UUID id,
            int versionNumber,
            String title,
            String description,
            String status,
            String scoringPolicyVersion,
            Instant createdAt,
            Instant publishedAt,
            Instant archivedAt,
            UUID fallbackMediaId,
            String fallbackAlternativeText,
            List<QuestionDetails> questions
    ) {
        static VersionDetails from(QuizVersion version) {
            return new VersionDetails(
                    version.id(), version.versionNumber(), version.title(), version.description(),
                    version.status().name(), version.scoringPolicyVersion().name(),
                    version.createdAt(), version.publishedAt(), version.archivedAt(),
                    version.fallbackMediaId(), version.fallbackAlternativeText(),
                    version.questions().stream().map(QuestionDetails::from).toList()
            );
        }
    }

    public record QuestionDetails(
            UUID id,
            int questionOrder,
            String prompt,
            String difficulty,
            UUID visualMediaId,
            String visualRole,
            String visualAlternativeText,
            String accessiblePrompt,
            List<OptionDetails> answerOptions
    ) {
        static QuestionDetails from(Question question) {
            return new QuestionDetails(
                    question.id(), question.questionOrder(), question.prompt(),
                    question.difficulty().name(),
                    question.visualMediaId(),
                    question.visualRole() == null ? null : question.visualRole().name(),
                    question.visualAlternativeText(), question.accessiblePrompt(),
                    question.answerOptions().stream().map(OptionDetails::from).toList()
            );
        }
    }

    public record OptionDetails(
            UUID id,
            int optionOrder,
            String text,
            boolean correct
    ) {
        static OptionDetails from(AnswerOption option) {
            return new OptionDetails(
                    option.id(), option.optionOrder(), option.text(), option.correct()
            );
        }
    }
}
