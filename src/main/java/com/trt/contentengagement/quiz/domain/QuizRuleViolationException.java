package com.trt.contentengagement.quiz.domain;

public class QuizRuleViolationException extends RuntimeException {

    private final String errorCode;

    public QuizRuleViolationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
