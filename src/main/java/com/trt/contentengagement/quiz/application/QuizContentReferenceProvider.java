package com.trt.contentengagement.quiz.application;

import java.util.UUID;

public interface QuizContentReferenceProvider {
    UUID requireContentId(UUID quizId);
}
