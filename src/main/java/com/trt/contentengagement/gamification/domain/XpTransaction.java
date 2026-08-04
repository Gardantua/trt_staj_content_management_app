package com.trt.contentengagement.gamification.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record XpTransaction(
        UUID id,
        UUID userId,
        UUID contentId,
        int amount,
        XpReason reason,
        XpPolicyVersion policyVersion,
        String referenceKey,
        UUID sourceAttemptId,
        UUID relatedTransactionId,
        UUID createdBy,
        String note,
        Instant occurredAt
) {
    public XpTransaction {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(contentId, "contentId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(policyVersion, "policyVersion must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        referenceKey = requireText(referenceKey, 150, "reference key");
        if (reason == XpReason.QUIZ_COMPLETED) {
            validateQuizCompletion(amount, policyVersion, sourceAttemptId,
                    relatedTransactionId, createdBy, note);
        } else {
            note = validateAdjustment(amount, policyVersion, sourceAttemptId,
                    relatedTransactionId, createdBy, note);
        }
    }

    public static XpTransaction forQuizCompletion(
            UUID userId,
            UUID contentId,
            UUID attemptId,
            int finalScore,
            Instant completedAt
    ) {
        int amount = XpPolicyVersion.SCORE_MATCH_V1.xpForCompletedQuiz(finalScore);
        return new XpTransaction(
                UUID.randomUUID(), userId, contentId, amount, XpReason.QUIZ_COMPLETED,
                XpPolicyVersion.SCORE_MATCH_V1, "QUIZ_ATTEMPT:" + attemptId,
                attemptId, null, null, null, completedAt
        );
    }

    public static XpTransaction adjustment(
            UUID userId,
            UUID contentId,
            UUID relatedTransactionId,
            int amount,
            String referenceKey,
            String note,
            UUID createdBy,
            Instant occurredAt
    ) {
        return new XpTransaction(
                UUID.randomUUID(), userId, contentId, amount, XpReason.ADMIN_ADJUSTMENT,
                XpPolicyVersion.ADMIN_ADJUSTMENT_V1, referenceKey,
                null, relatedTransactionId, createdBy, note, occurredAt
        );
    }

    private static void validateQuizCompletion(
            int amount,
            XpPolicyVersion policyVersion,
            UUID sourceAttemptId,
            UUID relatedTransactionId,
            UUID createdBy,
            String note
    ) {
        if (amount < 0 || policyVersion != XpPolicyVersion.SCORE_MATCH_V1
                || sourceAttemptId == null || relatedTransactionId != null
                || createdBy != null || note != null) {
            throw new GamificationRuleViolationException(
                    "XP_COMPLETION_CONTRACT_INVALID",
                    "Quiz completion XP must identify one attempt and use SCORE_MATCH_V1."
            );
        }
    }

    private static String validateAdjustment(
            int amount,
            XpPolicyVersion policyVersion,
            UUID sourceAttemptId,
            UUID relatedTransactionId,
            UUID createdBy,
            String note
    ) {
        if (amount == 0 || policyVersion != XpPolicyVersion.ADMIN_ADJUSTMENT_V1
                || sourceAttemptId != null || relatedTransactionId == null
                || createdBy == null) {
            throw new GamificationRuleViolationException(
                    "XP_ADJUSTMENT_CONTRACT_INVALID",
                    "An XP adjustment must be non-zero and reference an existing transaction."
            );
        }
        return requireText(note, 500, "adjustment note");
    }

    private static String requireText(String text, int maximumLength, String fieldName) {
        if (text == null || text.isBlank() || text.trim().length() > maximumLength) {
            throw new GamificationRuleViolationException(
                    "XP_TEXT_INVALID", fieldName + " is required and exceeds its allowed length."
            );
        }
        return text.trim();
    }
}
