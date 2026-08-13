package com.trt.contentengagement.identity.application;

public class InvalidLoginCredentialsException extends RuntimeException {
    public InvalidLoginCredentialsException() {
        super("Email or password is incorrect.");
    }
}

