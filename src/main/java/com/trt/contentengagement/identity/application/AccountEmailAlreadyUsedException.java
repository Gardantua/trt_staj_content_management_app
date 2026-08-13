package com.trt.contentengagement.identity.application;

public class AccountEmailAlreadyUsedException extends RuntimeException {
    public AccountEmailAlreadyUsedException() {
        super("An account already exists for this email address.");
    }
}

