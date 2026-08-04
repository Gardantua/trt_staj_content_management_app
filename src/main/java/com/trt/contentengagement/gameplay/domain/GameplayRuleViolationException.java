package com.trt.contentengagement.gameplay.domain;

public class GameplayRuleViolationException extends RuntimeException {
    private final String errorCode;

    public GameplayRuleViolationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() { return errorCode; }
}
