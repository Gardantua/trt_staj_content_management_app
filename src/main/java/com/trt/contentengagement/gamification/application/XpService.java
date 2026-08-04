package com.trt.contentengagement.gamification.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.admin.application.AdminAuditLog;
import com.trt.contentengagement.gamification.domain.GamificationRuleViolationException;
import com.trt.contentengagement.gamification.domain.XpReason;
import com.trt.contentengagement.gamification.domain.XpTransaction;
import com.trt.contentengagement.identity.application.CurrentActorProvider;
import com.trt.contentengagement.quiz.application.QuizContentReferenceProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class XpService {
    private final XpLedgerRepository xpLedgerRepository;
    private final CurrentActorProvider currentActorProvider;
    private final AdminAuditLog adminAuditLog;
    private final QuizContentReferenceProvider quizContentReferenceProvider;
    private final Clock clock;

    public XpService(
            XpLedgerRepository xpLedgerRepository,
            CurrentActorProvider currentActorProvider,
            AdminAuditLog adminAuditLog,
            QuizContentReferenceProvider quizContentReferenceProvider,
            Clock clock
    ) {
        this.xpLedgerRepository = xpLedgerRepository;
        this.currentActorProvider = currentActorProvider;
        this.adminAuditLog = adminAuditLog;
        this.quizContentReferenceProvider = quizContentReferenceProvider;
        this.clock = clock;
    }

    @Transactional
    public XpTransaction awardQuizCompletion(
            UUID userId,
            UUID quizId,
            UUID attemptId,
            int finalScore,
            Instant completedAt
    ) {
        UUID contentId = quizContentReferenceProvider.requireContentId(quizId);
        XpTransaction candidate = XpTransaction.forQuizCompletion(
                userId, contentId, attemptId, finalScore, completedAt
        );
        XpTransaction stored = xpLedgerRepository.appendIfAbsent(candidate);
        if (!sameCompletion(stored, candidate)) {
            throw new GamificationRuleViolationException(
                    "XP_SOURCE_CONFLICT",
                    "The attempt already produced a different XP transaction."
            );
        }
        return stored;
    }

    @Transactional(readOnly = true)
    public XpTransaction findAwardForAttempt(UUID attemptId) {
        return xpLedgerRepository.findBySourceAttemptId(attemptId)
                .orElseThrow(() -> new XpTransactionNotFoundException(attemptId));
    }

    @Transactional(readOnly = true)
    public XpSummary currentUserSummary() {
        UUID userId = currentActorProvider.getCurrentActor().actorId();
        return xpLedgerRepository.summarize(userId);
    }

    @Transactional
    public XpTransaction adjust(
            UUID relatedTransactionId, int amount, String referenceKey, String note
    ) {
        XpTransaction original = xpLedgerRepository.findById(relatedTransactionId)
                .orElseThrow(() -> new XpTransactionNotFoundException(relatedTransactionId));
        if (original.reason() != XpReason.QUIZ_COMPLETED) {
            throw new GamificationRuleViolationException(
                    "XP_ADJUSTMENT_TARGET_INVALID",
                    "An adjustment must reference an original quiz completion transaction."
            );
        }
        UUID actorId = currentActorProvider.getCurrentActor().actorId();
        XpTransaction candidate = XpTransaction.adjustment(
                original.userId(), original.contentId(), original.id(), amount, referenceKey,
                note, actorId, clock.instant()
        );
        XpTransaction stored = xpLedgerRepository.appendIfAbsent(candidate);
        if (!sameAdjustment(stored, candidate)) {
            throw new GamificationRuleViolationException(
                    "XP_REFERENCE_CONFLICT",
                    "The XP reference key was already used for a different transaction."
            );
        }
        if (stored.id().equals(candidate.id())) {
            adminAuditLog.record(
                    actorId, "XP_ADJUSTMENT_CREATED", "XP_TRANSACTION",
                    stored.id(), stored.occurredAt()
            );
        }
        return stored;
    }

    private boolean sameCompletion(XpTransaction stored, XpTransaction candidate) {
        return stored.reason() == XpReason.QUIZ_COMPLETED
                && stored.userId().equals(candidate.userId())
                && stored.contentId().equals(candidate.contentId())
                && stored.sourceAttemptId().equals(candidate.sourceAttemptId())
                && stored.amount() == candidate.amount()
                && stored.policyVersion() == candidate.policyVersion()
                && stored.referenceKey().equals(candidate.referenceKey());
    }

    private boolean sameAdjustment(XpTransaction stored, XpTransaction candidate) {
        return stored.reason() == XpReason.ADMIN_ADJUSTMENT
                && stored.userId().equals(candidate.userId())
                && stored.contentId().equals(candidate.contentId())
                && stored.relatedTransactionId().equals(candidate.relatedTransactionId())
                && stored.amount() == candidate.amount()
                && stored.referenceKey().equals(candidate.referenceKey())
                && stored.note().equals(candidate.note());
    }
}
