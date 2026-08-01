package com.trt.contentengagement.content.domain;

public class ContentRuleViolationException extends RuntimeException {

    private final String errorCode;

    public ContentRuleViolationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
