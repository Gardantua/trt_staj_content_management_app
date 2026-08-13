package com.trt.contentengagement.gameplay.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class QuizAttempt {
    private final UUID id;
    private final UUID userId;
    private final UUID quizId;
    private final UUID quizVersionId;
    private final String scoringPolicyVersion;
    private final String timingPolicyVersion;
    private final Instant startedAt;
    private Instant deadline;
    private AttemptStatus status;
    private int score;
    private Integer earnedXp;
    private Instant completedAt;
    private final List<SubmittedAnswer> answers;

    private QuizAttempt(
            UUID id, UUID userId, UUID quizId, UUID quizVersionId,
            String scoringPolicyVersion, Instant startedAt, Instant deadline,
            String timingPolicyVersion,
            AttemptStatus status, int score, Integer earnedXp, Instant completedAt,
            List<SubmittedAnswer> answers
    ) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.quizId = Objects.requireNonNull(quizId);
        this.quizVersionId = Objects.requireNonNull(quizVersionId);
        this.scoringPolicyVersion = Objects.requireNonNull(scoringPolicyVersion);
        this.timingPolicyVersion = Objects.requireNonNull(timingPolicyVersion);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.deadline = deadline;
        this.status = Objects.requireNonNull(status);
        this.score = score;
        this.earnedXp = earnedXp;
        this.completedAt = completedAt;
        this.answers = new ArrayList<>(Objects.requireNonNull(answers));
    }

    public static QuizAttempt start(
            UUID userId, UUID quizId, UUID quizVersionId,
            String scoringPolicyVersion, String timingPolicyVersion,
            Instant startedAt, Instant deadline
    ) {
        if (!deadline.isAfter(startedAt)) {
            throw new IllegalArgumentException("Deadline must be after start time.");
        }
        return new QuizAttempt(
                UUID.randomUUID(), userId, quizId, quizVersionId,
                scoringPolicyVersion, startedAt, deadline, timingPolicyVersion,
                AttemptStatus.ACTIVE, 0, null, null, List.of()
        );
    }

    public static QuizAttempt rehydrate(
            UUID id, UUID userId, UUID quizId, UUID quizVersionId,
            String scoringPolicyVersion, String timingPolicyVersion,
            Instant startedAt, Instant deadline,
            AttemptStatus status, int score, Integer earnedXp, Instant completedAt,
            List<SubmittedAnswer> answers
    ) {
        return new QuizAttempt(
                id, userId, quizId, quizVersionId, scoringPolicyVersion,
                startedAt, deadline, timingPolicyVersion,
                status, score, earnedXp, completedAt, answers
        );
    }

    public AnswerResult submitAnswer(
            QuestionAnswerKey answerKey,
            UUID selectedOptionId,
            String idempotencyKey,
            Instant answeredAt,
            int totalQuestionCount,
            Instant nextQuestionDeadline
    ) {
        SubmittedAnswer repeatedRequest = answers.stream()
                .filter(answer -> answer.idempotencyKey().equals(idempotencyKey))
                .findFirst()
                .orElse(null);
        if (repeatedRequest != null) {
            if (!repeatedRequest.questionId().equals(answerKey.questionId())
                    || !Objects.equals(repeatedRequest.selectedOptionId(), selectedOptionId)) {
                throw new GameplayRuleViolationException(
                        "IDEMPOTENCY_KEY_CONFLICT",
                        "The idempotency key was already used with a different answer."
                );
            }
            return AnswerResult.from(repeatedRequest, answerKey.correctOptionId(), status);
        }
        requireActiveAndWithinDeadline(answeredAt);
        if (answers.stream().anyMatch(answer -> answer.questionId().equals(answerKey.questionId()))) {
            throw new GameplayRuleViolationException(
                    "QUESTION_ALREADY_ANSWERED",
                    "The question already has a submitted answer."
            );
        }
        if (!answerKey.optionIds().contains(selectedOptionId)) {
            throw new GameplayRuleViolationException(
                    "OPTION_NOT_IN_QUESTION",
                    "The selected option does not belong to the question."
            );
        }
        boolean correct = answerKey.correctOptionId().equals(selectedOptionId);
        int awardedPoints = correct ? 10 : 0;
        SubmittedAnswer answer = new SubmittedAnswer(
                UUID.randomUUID(), answerKey.questionId(), selectedOptionId,
                idempotencyKey, correct, awardedPoints, answeredAt
        );
        answers.add(answer);
        score += awardedPoints;
        if (answers.size() == totalQuestionCount) {
            status = AttemptStatus.COMPLETED;
            completedAt = answeredAt;
        } else {
            status = AttemptStatus.AWAITING_NEXT_QUESTION;
            deadline = null;
        }
        return AnswerResult.from(answer, answerKey.correctOptionId(), status);
    }

    public AnswerResult timeoutQuestion(
            QuestionAnswerKey answerKey,
            String idempotencyKey,
            Instant timedOutAt,
            int totalQuestionCount,
            Instant nextQuestionDeadline
    ) {
        SubmittedAnswer repeatedRequest = answers.stream()
                .filter(answer -> answer.idempotencyKey().equals(idempotencyKey))
                .findFirst()
                .orElse(null);
        if (repeatedRequest != null) {
            if (!repeatedRequest.questionId().equals(answerKey.questionId())
                    || repeatedRequest.selectedOptionId() != null) {
                throw new GameplayRuleViolationException(
                        "IDEMPOTENCY_KEY_CONFLICT",
                        "The idempotency key was already used for a different action."
                );
            }
            return AnswerResult.from(repeatedRequest, answerKey.correctOptionId(), status);
        }
        requireActive();
        if (timedOutAt.isBefore(deadline)) {
            throw new GameplayRuleViolationException(
                    "QUESTION_TIME_REMAINING",
                    "The current question still has answer time remaining."
            );
        }
        if (answers.stream().anyMatch(answer -> answer.questionId().equals(answerKey.questionId()))) {
            throw new GameplayRuleViolationException(
                    "QUESTION_ALREADY_ANSWERED",
                    "The question already has a submitted answer."
            );
        }
        SubmittedAnswer timedOutAnswer = new SubmittedAnswer(
                UUID.randomUUID(), answerKey.questionId(), null,
                idempotencyKey, false, 0, timedOutAt
        );
        answers.add(timedOutAnswer);
        if (answers.size() == totalQuestionCount) {
            status = AttemptStatus.COMPLETED;
            completedAt = timedOutAt;
        } else {
            status = AttemptStatus.AWAITING_NEXT_QUESTION;
            deadline = null;
        }
        return AnswerResult.from(timedOutAnswer, answerKey.correctOptionId(), status);
    }

    public boolean expireIfDeadlineReached(Instant now) {
        if (status == AttemptStatus.ACTIVE && deadline != null && !now.isBefore(deadline)) {
            status = AttemptStatus.EXPIRED;
            earnedXp = 0;
            completedAt = now;
            return true;
        }
        return false;
    }

    public QuizAttemptCompleted complete(Instant now, int totalQuestionCount) {
        if (status == AttemptStatus.COMPLETED) {
            return completionEvent();
        }
        requireActiveAndWithinDeadline(now);
        if (answers.size() != totalQuestionCount) {
            throw new GameplayRuleViolationException(
                    "ATTEMPT_INCOMPLETE",
                    "All questions must be answered before completion."
            );
        }
        status = AttemptStatus.COMPLETED;
        completedAt = now;
        return completionEvent();
    }

    public QuizAttemptCompleted completionEvent() {
        if (status != AttemptStatus.COMPLETED) {
            throw new GameplayRuleViolationException(
                    "ATTEMPT_NOT_COMPLETED", "The attempt is not completed."
            );
        }
        if (earnedXp == null) {
            throw new GameplayRuleViolationException(
                    "ATTEMPT_REWARD_NOT_RECORDED",
                    "The completion reward must be recorded before publishing the event."
            );
        }
        return new QuizAttemptCompleted(
                id, userId, quizId, quizVersionId, score, earnedXp, completedAt
        );
    }

    public void recordEarnedXp(int earnedXp) {
        if (status != AttemptStatus.COMPLETED) {
            throw new GameplayRuleViolationException(
                    "ATTEMPT_NOT_COMPLETED", "Only a completed attempt can record earned XP."
            );
        }
        if (earnedXp < 0 || earnedXp > score) {
            throw new IllegalArgumentException("Earned XP must be between zero and the final score.");
        }
        if (this.earnedXp != null && this.earnedXp != earnedXp) {
            throw new GameplayRuleViolationException(
                    "ATTEMPT_REWARD_CONFLICT", "The completion reward was already recorded."
            );
        }
        this.earnedXp = earnedXp;
    }

    private void requireActiveAndWithinDeadline(Instant now) {
        requireActive();
        if (deadline == null || !now.isBefore(deadline)) {
            throw new GameplayRuleViolationException(
                    "ATTEMPT_EXPIRED", "The attempt deadline has passed."
            );
        }
    }

    private void requireActive() {
        if (status != AttemptStatus.ACTIVE) {
            throw new GameplayRuleViolationException(
                    "ATTEMPT_NOT_ACTIVE", "The attempt is no longer active."
            );
        }
    }

    public void startNextQuestion(Instant startedAt, Instant nextQuestionDeadline) {
        if (status != AttemptStatus.AWAITING_NEXT_QUESTION) {
            throw new GameplayRuleViolationException(
                    "ATTEMPT_NOT_AWAITING_NEXT_QUESTION",
                    "The attempt is not waiting for the next question."
            );
        }
        if (nextQuestionDeadline == null || !nextQuestionDeadline.isAfter(startedAt)) {
            throw new IllegalArgumentException("Next question deadline must be after progress time.");
        }
        status = AttemptStatus.ACTIVE;
        deadline = nextQuestionDeadline;
    }

    public Optional<SubmittedAnswer> answerFor(UUID questionId) {
        return answers.stream().filter(answer -> answer.questionId().equals(questionId)).findFirst();
    }
    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID quizId() { return quizId; }
    public UUID quizVersionId() { return quizVersionId; }
    public String scoringPolicyVersion() { return scoringPolicyVersion; }
    public String timingPolicyVersion() { return timingPolicyVersion; }
    public Instant startedAt() { return startedAt; }
    public Instant deadline() { return deadline; }
    public AttemptStatus status() { return status; }
    public int score() { return score; }
    public Integer earnedXp() { return earnedXp; }
    public Instant completedAt() { return completedAt; }
    public List<SubmittedAnswer> answers() {
        return answers.stream().sorted(Comparator.comparing(SubmittedAnswer::answeredAt)).toList();
    }

    public record AnswerResult(
            UUID questionId, UUID selectedOptionId, boolean correct,
            UUID correctOptionId, int awardedPoints, AttemptStatus attemptStatus
    ) {
        static AnswerResult from(
                SubmittedAnswer answer, UUID correctOptionId, AttemptStatus status
        ) {
            return new AnswerResult(
                    answer.questionId(), answer.selectedOptionId(), answer.correct(),
                    correctOptionId, answer.awardedPoints(), status
            );
        }
    }
}
