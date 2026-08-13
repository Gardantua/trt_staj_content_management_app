package com.trt.contentengagement.identity.application;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException() {
        super("The authenticated account no longer exists.");
    }
}
