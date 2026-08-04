package com.trt.contentengagement.gamification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class XpTransactionTest {
    private static final Instant OCCURRED_AT = Instant.parse("2026-08-04T12:00:00Z");

    @Test
    void scoreMatchPolicyAwardsXpEqualToServerScore() {
        UUID attemptId = UUID.randomUUID();

        XpTransaction transaction = XpTransaction.forQuizCompletion(
                UUID.randomUUID(), UUID.randomUUID(), attemptId, 300, OCCURRED_AT
        );

        assertThat(transaction.amount()).isEqualTo(300);
        assertThat(transaction.reason()).isEqualTo(XpReason.QUIZ_COMPLETED);
        assertThat(transaction.policyVersion()).isEqualTo(XpPolicyVersion.SCORE_MATCH_V1);
        assertThat(transaction.referenceKey()).isEqualTo("QUIZ_ATTEMPT:" + attemptId);
    }

    @Test
    void zeroScoreCompletionStillProducesAnIdempotencyLedgerFact() {
        XpTransaction transaction = XpTransaction.forQuizCompletion(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0, OCCURRED_AT
        );

        assertThat(transaction.amount()).isZero();
        assertThat(transaction.sourceAttemptId()).isNotNull();
    }

    @Test
    void adjustmentMustBeNonZeroAndReferenceOriginalTransaction() {
        assertThatThrownBy(() -> XpTransaction.adjustment(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0,
                "support-case-1", "Correction", UUID.randomUUID(), OCCURRED_AT
        )).isInstanceOf(GamificationRuleViolationException.class)
                .hasMessageContaining("non-zero");
    }

    @Test
    void adjustmentIsANewSignedLedgerEntry() {
        UUID originalTransactionId = UUID.randomUUID();

        XpTransaction adjustment = XpTransaction.adjustment(
                UUID.randomUUID(), UUID.randomUUID(), originalTransactionId, -50,
                "support-case-2", "Score correction", UUID.randomUUID(), OCCURRED_AT
        );

        assertThat(adjustment.amount()).isEqualTo(-50);
        assertThat(adjustment.reason()).isEqualTo(XpReason.ADMIN_ADJUSTMENT);
        assertThat(adjustment.relatedTransactionId()).isEqualTo(originalTransactionId);
        assertThat(adjustment.sourceAttemptId()).isNull();
    }
}
