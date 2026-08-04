package com.trt.contentengagement.gamification.domain;

public class GamificationRuleViolationException extends RuntimeException {
    private final String errorCode;

    public GamificationRuleViolationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
