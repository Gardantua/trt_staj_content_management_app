package com.trt.contentengagement.quiz.application;

import java.util.UUID;

public interface GameplayQuizSnapshotProvider {
    GameplayQuizSnapshot getPublishedByQuizId(UUID quizId);
    GameplayQuizSnapshot getByVersionId(UUID versionId);
}
