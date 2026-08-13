package com.trt.contentengagement.messaging.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.trt.contentengagement.gamification.domain.XpTransaction;

/** Public, versioned contract consumed by the leaderboard service. */
public record XpChangedIntegrationEventV1(
        UUID eventId,
        UUID transactionId,
        UUID userId,
        UUID contentId,
        int amount,
        String reason,
        Instant occurredAt
) {
    public static final String EVENT_TYPE = "xp.changed";
    public static final int EVENT_VERSION = 1;

    public XpChangedIntegrationEventV1 {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(contentId, "contentId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (!eventId.equals(transactionId)) {
            throw new IllegalArgumentException("XP event ID must equal its transaction ID.");
        }
    }

    public static XpChangedIntegrationEventV1 from(XpTransaction xpTransaction) {
        return new XpChangedIntegrationEventV1(
                xpTransaction.id(),
                xpTransaction.id(),
                xpTransaction.userId(),
                xpTransaction.contentId(),
                xpTransaction.amount(),
                xpTransaction.reason().name(),
                xpTransaction.occurredAt()
        );
    }
}
