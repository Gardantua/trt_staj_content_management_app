package com.trt.contentengagement.identity.application;

public interface PasswordResetNotificationPort {
    void sendPasswordResetLink(String recipientEmail, String rawToken);
}
