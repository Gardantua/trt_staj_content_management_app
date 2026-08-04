package com.trt.contentengagement.gamification.application;

import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.gamification.domain.XpTransaction;

public interface XpLedgerRepository {
    XpTransaction appendIfAbsent(XpTransaction xpTransaction);
    Optional<XpTransaction> findById(UUID transactionId);
    Optional<XpTransaction> findBySourceAttemptId(UUID attemptId);
    XpSummary summarize(UUID userId);
}
