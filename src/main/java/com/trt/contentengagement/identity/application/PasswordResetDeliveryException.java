package com.trt.contentengagement.identity.application;

public class PasswordResetDeliveryException extends RuntimeException {
    public PasswordResetDeliveryException(Throwable cause) {
        super("Password reset email delivery failed.", cause);
    }
}
