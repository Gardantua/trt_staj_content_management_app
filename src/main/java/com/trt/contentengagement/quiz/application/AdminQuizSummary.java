package com.trt.contentengagement.quiz.application;

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

import com.trt.contentengagement.quiz.domain.Quiz;
import com.trt.contentengagement.quiz.domain.QuizVersion;
import com.trt.contentengagement.quiz.domain.QuizVersionStatus;

public record AdminQuizSummary(
        UUID id,
        UUID contentId,
        String scopeType,
        UUID seasonId,
        UUID episodeId,
        String title,
        String status,
        int versionNumber,
        int questionCount,
        Instant updatedAt
) {
    public static AdminQuizSummary from(Quiz quiz) {
        QuizVersion workingVersion = quiz.versions().stream()
                .filter(version -> version.status() == QuizVersionStatus.DRAFT)
                .findFirst()
                .orElseGet(() -> quiz.versions().stream()
                        .max(Comparator.comparingInt(QuizVersion::versionNumber))
                        .orElseThrow());
        return new AdminQuizSummary(
                quiz.id(), quiz.contentId(), quiz.scopeType().name(),
                quiz.seasonId(), quiz.episodeId(), workingVersion.title(),
                workingVersion.status().name(), workingVersion.versionNumber(),
                workingVersion.questions().size(), quiz.updatedAt()
        );
    }
}
