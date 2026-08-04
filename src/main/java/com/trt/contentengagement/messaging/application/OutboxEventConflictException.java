package com.trt.contentengagement.messaging.application;

public class OutboxEventConflictException extends RuntimeException {
    public OutboxEventConflictException() {
        super("The completion event conflicts with the existing outbox record.");
    }
}
