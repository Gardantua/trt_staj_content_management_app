package com.trt.contentengagement.identity.application;

public interface PasswordResetTokenGenerator {
    String generate();

    String hash(String rawToken);
}
