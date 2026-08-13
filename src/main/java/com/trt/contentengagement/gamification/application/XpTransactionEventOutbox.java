package com.trt.contentengagement.gamification.application;

import com.trt.contentengagement.gamification.domain.XpTransaction;

/** Stages a durable integration event in the same transaction as a new XP row. */
public interface XpTransactionEventOutbox {
    void stage(XpTransaction xpTransaction);
}
