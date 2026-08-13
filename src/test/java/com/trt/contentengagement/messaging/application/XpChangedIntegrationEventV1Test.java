package com.trt.contentengagement.messaging.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.gamification.domain.XpPolicyVersion;
import com.trt.contentengagement.gamification.domain.XpTransaction;
import org.junit.jupiter.api.Test;

class XpChangedIntegrationEventV1Test {
    @Test
    void exposesOnlyTheStableLeaderboardProjectionFields() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-12T08:00:00Z");
        XpTransaction transaction = new XpTransaction(
                transactionId, userId, contentId, 80,
                com.trt.contentengagement.gamification.domain.XpReason.QUIZ_COMPLETED,
                XpPolicyVersion.FIRST_COMPLETION_SCORE_V2,
                "QUIZ_ATTEMPT:" + UUID.randomUUID(), UUID.randomUUID(),
                null, null, null, occurredAt
        );

        XpChangedIntegrationEventV1 event = XpChangedIntegrationEventV1.from(transaction);

        assertThat(event.eventId()).isEqualTo(transactionId);
        assertThat(event.transactionId()).isEqualTo(transactionId);
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.contentId()).isEqualTo(contentId);
        assertThat(event.amount()).isEqualTo(80);
        assertThat(event.reason()).isEqualTo("QUIZ_COMPLETED");
        assertThat(event.occurredAt()).isEqualTo(occurredAt);
    }
}
