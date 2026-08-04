package com.trt.contentengagement.quiz.application;

import java.util.UUID;

public class QuizNotFoundException extends RuntimeException {

    public QuizNotFoundException(UUID quizId) {
        super("Quiz not found: " + quizId);
    }
}
