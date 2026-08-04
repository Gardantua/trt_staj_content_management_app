package com.trt.contentengagement.media.domain;

public class MediaRuleViolationException extends RuntimeException {
    private final String errorCode;

    public MediaRuleViolationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
