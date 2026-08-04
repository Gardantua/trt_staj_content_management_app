package com.trt.contentengagement.gameplay.application;

import java.util.UUID;

public class AttemptNotFoundException extends RuntimeException {
    public AttemptNotFoundException(UUID attemptId) { super("Attempt not found: " + attemptId); }
}
