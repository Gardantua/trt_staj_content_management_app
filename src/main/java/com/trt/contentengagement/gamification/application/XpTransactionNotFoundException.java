package com.trt.contentengagement.gamification.application;

import java.util.UUID;

public class XpTransactionNotFoundException extends RuntimeException {
    public XpTransactionNotFoundException(UUID transactionId) {
        super("XP transaction was not found: " + transactionId);
    }
}
