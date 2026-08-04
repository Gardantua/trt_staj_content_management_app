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
    private final Instant deadline;
    private AttemptStatus status;
    private int score;
    private Instant completedAt;
    private final List<SubmittedAnswer> answers;

    private QuizAttempt(
            UUID id, UUID userId, UUID quizId, UUID quizVersionId,
            String scoringPolicyVersion, Instant startedAt, Instant deadline,
            String timingPolicyVersion,
            AttemptStatus status, int score, Instant completedAt,
            List<SubmittedAnswer> answers
    ) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.quizId = Objects.requireNonNull(quizId);
        this.quizVersionId = Objects.requireNonNull(quizVersionId);
        this.scoringPolicyVersion = Objects.requireNonNull(scoringPolicyVersion);
        this.timingPolicyVersion = Objects.requireNonNull(timingPolicyVersion);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.deadline = Objects.requireNonNull(deadline);
        this.status = Objects.requireNonNull(status);
        this.score = score;
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
                AttemptStatus.ACTIVE, 0, null, List.of()
        );
    }

    public static QuizAttempt rehydrate(
            UUID id, UUID userId, UUID quizId, UUID quizVersionId,
            String scoringPolicyVersion, String timingPolicyVersion,
            Instant startedAt, Instant deadline,
            AttemptStatus status, int score, Instant completedAt,
            List<SubmittedAnswer> answers
    ) {
        return new QuizAttempt(
                id, userId, quizId, quizVersionId, scoringPolicyVersion,
                startedAt, deadline, timingPolicyVersion,
                status, score, completedAt, answers
        );
    }

    public AnswerResult submitAnswer(
            QuestionAnswerKey answerKey,
            UUID selectedOptionId,
            String idempotencyKey,
            Instant answeredAt,
            int totalQuestionCount
    ) {
        SubmittedAnswer repeatedRequest = answers.stream()
                .filter(answer -> answer.idempotencyKey().equals(idempotencyKey))
                .findFirst()
                .orElse(null);
        if (repeatedRequest != null) {
            if (!repeatedRequest.questionId().equals(answerKey.questionId())
                    || !repeatedRequest.selectedOptionId().equals(selectedOptionId)) {
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
        int awardedPoints = correct ? 100 : 0;
        SubmittedAnswer answer = new SubmittedAnswer(
                UUID.randomUUID(), answerKey.questionId(), selectedOptionId,
                idempotencyKey, correct, awardedPoints, answeredAt
        );
        answers.add(answer);
        score += awardedPoints;
        if (answers.size() == totalQuestionCount) {
            status = AttemptStatus.COMPLETED;
            completedAt = answeredAt;
        }
        return AnswerResult.from(answer, answerKey.correctOptionId(), status);
    }

    public boolean expireIfDeadlineReached(Instant now) {
        if (status == AttemptStatus.ACTIVE && !now.isBefore(deadline)) {
            status = AttemptStatus.EXPIRED;
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
        return new QuizAttemptCompleted(
                id, userId, quizId, quizVersionId, score, completedAt
        );
    }

    private void requireActiveAndWithinDeadline(Instant now) {
        if (status != AttemptStatus.ACTIVE) {
            throw new GameplayRuleViolationException(
                    "ATTEMPT_NOT_ACTIVE", "The attempt is no longer active."
            );
        }
        if (!now.isBefore(deadline)) {
            throw new GameplayRuleViolationException(
                    "ATTEMPT_EXPIRED", "The attempt deadline has passed."
            );
        }
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
