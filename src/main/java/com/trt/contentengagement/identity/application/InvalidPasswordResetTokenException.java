package com.trt.contentengagement.identity.application;

public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException() {
        super("The password reset link is invalid or expired.");
    }
}
