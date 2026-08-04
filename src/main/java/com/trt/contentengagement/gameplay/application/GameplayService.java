package com.trt.contentengagement.gameplay.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.gameplay.domain.QuestionAnswerKey;
import com.trt.contentengagement.gameplay.domain.QuizAttempt;
import com.trt.contentengagement.gameplay.domain.AttemptStatus;
import com.trt.contentengagement.gameplay.domain.TimingPolicyVersion;
import com.trt.contentengagement.identity.application.CurrentActorProvider;
import com.trt.contentengagement.quiz.application.GameplayQuizSnapshot;
import com.trt.contentengagement.quiz.application.GameplayQuizSnapshotProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameplayService {
    private final QuizAttemptRepository attemptRepository;
    private final GameplayQuizSnapshotProvider quizSnapshotProvider;
    private final CurrentActorProvider currentActorProvider;
    private final QuizCompletionEventOutbox completionEventOutbox;
    private final Clock clock;

    public GameplayService(
            QuizAttemptRepository attemptRepository,
            GameplayQuizSnapshotProvider quizSnapshotProvider,
            CurrentActorProvider currentActorProvider,
            QuizCompletionEventOutbox completionEventOutbox,
            Clock clock
    ) {
        this.attemptRepository=attemptRepository; this.quizSnapshotProvider=quizSnapshotProvider;
        this.currentActorProvider=currentActorProvider;
        this.completionEventOutbox=completionEventOutbox;
        this.clock=clock;
    }

    @Transactional
    public AttemptDetails start(UUID quizId, TimingPolicyVersion timingPolicyVersion) {
        UUID userId = currentActorProvider.getCurrentActor().actorId();
        QuizAttempt existing = attemptRepository.findActiveByUserIdAndQuizId(userId, quizId)
                .orElse(null);
        if (existing != null) {
            if (!existing.timingPolicyVersion().equals(timingPolicyVersion.name())) {
                throw new com.trt.contentengagement.gameplay.domain.GameplayRuleViolationException(
                        "ACTIVE_ATTEMPT_TIMING_POLICY_CONFLICT",
                        "The active attempt was started with a different timing policy."
                );
            }
            GameplayQuizSnapshot snapshot = quizSnapshotProvider.getByVersionId(existing.quizVersionId());
            return AttemptDetails.from(existing, snapshot);
        }
        GameplayQuizSnapshot snapshot = quizSnapshotProvider.getPublishedByQuizId(quizId);
        Instant now = clock.instant();
        QuizAttempt attempt = QuizAttempt.start(
                userId, quizId, snapshot.versionId(), snapshot.scoringPolicyVersion(),
                timingPolicyVersion.name(), now, now.plus(timingPolicyVersion.duration())
        );
        return AttemptDetails.from(attemptRepository.save(attempt), snapshot);
    }

    @Transactional
    public AttemptDetails get(UUID attemptId) {
        QuizAttempt attempt = requireOwned(attemptId);
        GameplayQuizSnapshot snapshot = quizSnapshotProvider.getByVersionId(attempt.quizVersionId());
        if (attempt.expireIfDeadlineReached(clock.instant())) {
            attempt = attemptRepository.save(attempt);
        }
        Integer earnedXp = attempt.status() == AttemptStatus.COMPLETED ? attempt.score() : null;
        return AttemptDetails.from(attempt, snapshot, earnedXp);
    }

    @Transactional(noRollbackFor = AttemptExpiredException.class)
    public AnswerSubmissionResult answer(
            UUID attemptId, UUID questionId, UUID selectedOptionId, String idempotencyKey
    ) {
        QuizAttempt attempt = requireOwned(attemptId);
        GameplayQuizSnapshot snapshot = quizSnapshotProvider.getByVersionId(attempt.quizVersionId());
        Instant now = clock.instant();
        if (attempt.expireIfDeadlineReached(now)) {
            attemptRepository.save(attempt);
            throw new AttemptExpiredException();
        }
        GameplayQuizSnapshot.QuestionSnapshot question = snapshot.questions().stream()
                .filter(candidate -> candidate.questionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new com.trt.contentengagement.gameplay.domain.GameplayRuleViolationException(
                        "QUESTION_NOT_IN_ATTEMPT", "The question does not belong to this attempt."
                ));
        boolean repeatedKey = attempt.answers().stream()
                .anyMatch(answer -> answer.idempotencyKey().equals(idempotencyKey));
        if (!repeatedKey) {
            GameplayQuizSnapshot.QuestionSnapshot expected = snapshot.questions()
                    .get(attempt.answers().size());
            if (!expected.questionId().equals(questionId)) {
                throw new com.trt.contentengagement.gameplay.domain.GameplayRuleViolationException(
                        "QUESTION_OUT_OF_ORDER", "Questions must be answered in server order."
                );
            }
        }
        QuizAttempt.AnswerResult result = attempt.submitAnswer(
                new QuestionAnswerKey(
                        question.questionId(), question.questionOrder(),
                        question.options().stream().map(GameplayQuizSnapshot.OptionSnapshot::optionId).toList(),
                        question.correctOptionId()
                ),
                selectedOptionId, idempotencyKey, now, snapshot.questions().size()
        );
        QuizAttempt saved = attemptRepository.save(attempt);
        if (saved.status() == AttemptStatus.COMPLETED) {
            stageCompletionEvent(saved);
        }
        Integer earnedXp = saved.status() == AttemptStatus.COMPLETED ? saved.score() : null;
        AttemptDetails details = AttemptDetails.from(saved, snapshot, earnedXp);
        return new AnswerSubmissionResult(
                saved.id(), new AttemptDetails.AnswerFeedback(
                        result.questionId(), result.selectedOptionId(), result.correct(),
                        result.correctOptionId(), result.awardedPoints()
                ), saved.status().name(), saved.score(), earnedXp, details.currentQuestion()
        );
    }

    @Transactional(noRollbackFor = AttemptExpiredException.class)
    public AttemptDetails complete(UUID attemptId) {
        QuizAttempt attempt = requireOwned(attemptId);
        GameplayQuizSnapshot snapshot = quizSnapshotProvider.getByVersionId(attempt.quizVersionId());
        Instant now = clock.instant();
        if (attempt.expireIfDeadlineReached(now)) {
            attemptRepository.save(attempt);
            throw new AttemptExpiredException();
        }
        attempt.complete(now, snapshot.questions().size());
        QuizAttempt saved = attemptRepository.save(attempt);
        stageCompletionEvent(saved);
        return AttemptDetails.from(saved, snapshot, saved.score());
    }

    private void stageCompletionEvent(QuizAttempt completedAttempt) {
        completionEventOutbox.stage(completedAttempt.completionEvent());
    }

    private QuizAttempt requireOwned(UUID attemptId) {
        UUID userId = currentActorProvider.getCurrentActor().actorId();
        return attemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));
    }
}
