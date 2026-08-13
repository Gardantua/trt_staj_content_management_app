package com.trt.contentengagement.gameplay.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    public AttemptDetails start(UUID quizId) {
        TimingPolicyVersion timingPolicyVersion = TimingPolicyVersion.QUESTION_30_SECONDS_V1;
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
        Instant now = now();
        QuizAttempt attempt = QuizAttempt.start(
                userId, quizId, snapshot.versionId(), snapshot.scoringPolicyVersion(),
                timingPolicyVersion.name(), now, now.plus(timingPolicyVersion.duration())
        );
        return AttemptDetails.from(attemptRepository.save(attempt), snapshot);
    }

    @Transactional(readOnly = true)
    public List<QuizResultSummary> latestCompletedResults() {
        return attemptRepository.findLatestCompletedResultsByUserId(
                currentActorProvider.getCurrentActor().actorId()
        );
    }

    @Transactional
    public AttemptDetails get(UUID attemptId) {
        QuizAttempt attempt = requireOwned(attemptId);
        GameplayQuizSnapshot snapshot = quizSnapshotProvider.getByVersionId(attempt.quizVersionId());
        Instant now = now();
        if (attempt.status() == AttemptStatus.ACTIVE && !now.isBefore(attempt.deadline())) {
            attempt = timeoutCurrentQuestion(
                    attempt, snapshot, "automatic-timeout-" + attempt.answers().size(), now
            );
        }
        return AttemptDetails.from(attempt, snapshot, attempt.earnedXp());
    }

    @Transactional(noRollbackFor = AttemptExpiredException.class)
    public AnswerSubmissionResult answer(
            UUID attemptId, UUID questionId, UUID selectedOptionId, String idempotencyKey
    ) {
        QuizAttempt attempt = requireOwned(attemptId);
        GameplayQuizSnapshot snapshot = quizSnapshotProvider.getByVersionId(attempt.quizVersionId());
        Instant now = now();
        if (attempt.status() == AttemptStatus.ACTIVE && !now.isBefore(attempt.deadline())) {
            throw new com.trt.contentengagement.gameplay.domain.GameplayRuleViolationException(
                    "QUESTION_TIME_EXPIRED", "The current question answer time has expired."
            );
        }
        GameplayQuizSnapshot.QuestionSnapshot question = snapshot.questions().stream()
                .filter(candidate -> candidate.questionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new com.trt.contentengagement.gameplay.domain.GameplayRuleViolationException(
                        "QUESTION_NOT_IN_ATTEMPT", "The question does not belong to this attempt."
                ));
        boolean repeatedKey = attempt.answers().stream()
                .anyMatch(answer -> answer.idempotencyKey().equals(idempotencyKey));
        if (attempt.status() == AttemptStatus.AWAITING_NEXT_QUESTION && !repeatedKey) {
            attempt.startNextQuestion(
                    now, now.plus(TimingPolicyVersion.QUESTION_30_SECONDS_V1.duration())
            );
        }
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
                selectedOptionId, idempotencyKey, now, snapshot.questions().size(),
                now.plus(TimingPolicyVersion.QUESTION_30_SECONDS_V1.duration())
        );
        QuizAttempt saved = attemptRepository.save(attempt);
        if (saved.status() == AttemptStatus.COMPLETED) {
            QuizAttemptRepository.CompletionReward reward = recordCompletionReward(saved);
            saved = reward.attempt();
            stageCompletionEvent(saved, reward.firstCompletionReward());
        }
        Integer earnedXp = saved.earnedXp();
        AttemptDetails details = AttemptDetails.from(saved, snapshot, earnedXp);
        AttemptDetails.AnswerFeedback feedback = details.submittedAnswers().isEmpty() ? new AttemptDetails.AnswerFeedback(
                result.questionId(), result.selectedOptionId(), result.correct(),
                result.correctOptionId(), result.awardedPoints()
        ) : details.submittedAnswers().get(details.submittedAnswers().size() - 1);
        return new AnswerSubmissionResult(
                saved.id(), feedback, saved.status().name(), saved.score(), earnedXp,
                saved.status() == AttemptStatus.ACTIVE ? saved.deadline() : null,
                details.currentQuestion()
        );
    }

    @Transactional
    public AttemptDetails startNextQuestion(UUID attemptId) {
        QuizAttempt attempt = requireOwned(attemptId);
        GameplayQuizSnapshot snapshot = quizSnapshotProvider.getByVersionId(attempt.quizVersionId());
        Instant now = now();
        attempt.startNextQuestion(
                now, now.plus(TimingPolicyVersion.QUESTION_30_SECONDS_V1.duration())
        );
        return AttemptDetails.from(attemptRepository.save(attempt), snapshot);
    }

    @Transactional
    public AttemptDetails timeout(UUID attemptId, UUID questionId, String idempotencyKey) {
        QuizAttempt attempt = requireOwned(attemptId);
        GameplayQuizSnapshot snapshot = quizSnapshotProvider.getByVersionId(attempt.quizVersionId());
        if (attempt.status() == AttemptStatus.COMPLETED) {
            return AttemptDetails.from(attempt, snapshot, attempt.earnedXp());
        }
        if (attempt.status() != AttemptStatus.ACTIVE) {
            throw new com.trt.contentengagement.gameplay.domain.GameplayRuleViolationException(
                    "ATTEMPT_NOT_ACTIVE", "The attempt is no longer accepting timed answers."
            );
        }
        GameplayQuizSnapshot.QuestionSnapshot current = snapshot.questions()
                .get(attempt.answers().size());
        if (!current.questionId().equals(questionId)) {
            throw new com.trt.contentengagement.gameplay.domain.GameplayRuleViolationException(
                    "QUESTION_OUT_OF_ORDER", "Only the current question can time out."
            );
        }
        QuizAttempt saved = timeoutCurrentQuestion(
                attempt, snapshot, idempotencyKey, now()
        );
        return AttemptDetails.from(saved, snapshot, saved.earnedXp());
    }

    @Transactional(noRollbackFor = AttemptExpiredException.class)
    public AttemptDetails complete(UUID attemptId) {
        QuizAttempt attempt = requireOwned(attemptId);
        GameplayQuizSnapshot snapshot = quizSnapshotProvider.getByVersionId(attempt.quizVersionId());
        Instant now = now();
        attempt.complete(now, snapshot.questions().size());
        QuizAttemptRepository.CompletionReward reward = recordCompletionReward(
                attemptRepository.save(attempt)
        );
        QuizAttempt saved = reward.attempt();
        stageCompletionEvent(saved, reward.firstCompletionReward());
        return AttemptDetails.from(saved, snapshot, saved.earnedXp());
    }

    private void stageCompletionEvent(
            QuizAttempt completedAttempt, boolean firstCompletionReward
    ) {
        completionEventOutbox.stage(completedAttempt.completionEvent(), firstCompletionReward);
    }

    private QuizAttempt timeoutCurrentQuestion(
            QuizAttempt attempt,
            GameplayQuizSnapshot snapshot,
            String idempotencyKey,
            Instant now
    ) {
        GameplayQuizSnapshot.QuestionSnapshot question = snapshot.questions()
                .get(attempt.answers().size());
        attempt.timeoutQuestion(
                new QuestionAnswerKey(
                        question.questionId(), question.questionOrder(),
                        question.options().stream()
                                .map(GameplayQuizSnapshot.OptionSnapshot::optionId).toList(),
                        question.correctOptionId()
                ),
                idempotencyKey, now, snapshot.questions().size(),
                now.plus(TimingPolicyVersion.QUESTION_30_SECONDS_V1.duration())
        );
        QuizAttempt saved = attemptRepository.save(attempt);
        if (saved.status() == AttemptStatus.COMPLETED) {
            QuizAttemptRepository.CompletionReward reward = recordCompletionReward(saved);
            saved = reward.attempt();
            stageCompletionEvent(saved, reward.firstCompletionReward());
        }
        return saved;
    }

    private QuizAttemptRepository.CompletionReward recordCompletionReward(
            QuizAttempt completedAttempt
    ) {
        return attemptRepository.recordFirstCompletionReward(completedAttempt);
    }

    private QuizAttempt requireOwned(UUID attemptId) {
        UUID userId = currentActorProvider.getCurrentActor().actorId();
        return attemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
