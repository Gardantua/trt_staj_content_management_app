package com.trt.contentengagement.gameplay.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class QuizAttemptTest {
    private static final Instant START = Instant.parse("2026-08-04T10:00:00Z");
    private static final UUID QUESTION_ID = UUID.randomUUID();
    private static final UUID CORRECT_OPTION_ID = UUID.randomUUID();
    private static final UUID WRONG_OPTION_ID = UUID.randomUUID();
    private static final UUID THIRD_OPTION_ID = UUID.randomUUID();
    private static final UUID FOURTH_OPTION_ID = UUID.randomUUID();
    private final QuestionAnswerKey answerKey = new QuestionAnswerKey(
            QUESTION_ID, 1,
            List.of(CORRECT_OPTION_ID, WRONG_OPTION_ID, THIRD_OPTION_ID, FOURTH_OPTION_ID),
            CORRECT_OPTION_ID
    );

    @Test
    void acceptedAnswerImmediatelyRevealsCorrectOptionAndAwardsServerScore() {
        QuizAttempt attempt = attempt();
        QuizAttempt.AnswerResult result = attempt.submitAnswer(
                answerKey, CORRECT_OPTION_ID, "answer-1", START.plusSeconds(5), 2,
                START.plusSeconds(35)
        );
        assertThat(result.correct()).isTrue();
        assertThat(result.correctOptionId()).isEqualTo(CORRECT_OPTION_ID);
        assertThat(result.awardedPoints()).isEqualTo(10);
        assertThat(attempt.score()).isEqualTo(10);
    }

    @Test
    void sameQuestionCannotBeAnsweredTwice() {
        QuizAttempt attempt = attempt();
        attempt.submitAnswer(
                answerKey, WRONG_OPTION_ID, "answer-1", START.plusSeconds(5), 2,
                START.plusSeconds(35)
        );
        attempt.startNextQuestion(START.plusSeconds(10), START.plusSeconds(40));
        assertThatThrownBy(() -> attempt.submitAnswer(
                answerKey, CORRECT_OPTION_ID, "answer-2", START.plusSeconds(12), 2,
                START.plusSeconds(42)
        )).isInstanceOf(GameplayRuleViolationException.class)
                .hasMessage("The question already has a submitted answer.");
    }

    @Test
    void repeatedIdempotencyKeyReturnsSameAnswerButRejectsDifferentPayload() {
        QuizAttempt attempt = attempt();
        QuizAttempt.AnswerResult first = attempt.submitAnswer(
                answerKey, WRONG_OPTION_ID, "same-key", START.plusSeconds(5), 2,
                START.plusSeconds(35)
        );
        QuizAttempt.AnswerResult repeated = attempt.submitAnswer(
                answerKey, WRONG_OPTION_ID, "same-key", START.plusSeconds(8), 2,
                START.plusSeconds(38)
        );
        assertThat(repeated).isEqualTo(first);
        assertThatThrownBy(() -> attempt.submitAnswer(
                answerKey, CORRECT_OPTION_ID, "same-key", START.plusSeconds(9), 2,
                START.plusSeconds(39)
        )).isInstanceOf(GameplayRuleViolationException.class)
                .hasMessageContaining("different answer");
    }

    @Test
    void deadlineTimesOutCurrentQuestionAndWaitsForThePlayerToOpenTheNextQuestion() {
        QuizAttempt attempt = attempt();
        QuizAttempt.AnswerResult result = attempt.timeoutQuestion(
                answerKey, "timeout-1", START.plusSeconds(30), 2, START.plusSeconds(60)
        );
        assertThat(result.selectedOptionId()).isNull();
        assertThat(result.correct()).isFalse();
        assertThat(attempt.status()).isEqualTo(AttemptStatus.AWAITING_NEXT_QUESTION);
        assertThat(attempt.deadline()).isNull();
    }

    @Test
    void nextQuestionTimerStartsOnlyWhenThePlayerContinues() {
        QuizAttempt attempt = attempt();
        attempt.submitAnswer(
                answerKey, CORRECT_OPTION_ID, "answer-1", START.plusSeconds(5), 2,
                START.plusSeconds(35)
        );

        assertThat(attempt.status()).isEqualTo(AttemptStatus.AWAITING_NEXT_QUESTION);
        assertThat(attempt.deadline()).isNull();

        attempt.startNextQuestion(START.plusSeconds(8), START.plusSeconds(38));

        assertThat(attempt.status()).isEqualTo(AttemptStatus.ACTIVE);
        assertThat(attempt.deadline()).isEqualTo(START.plusSeconds(38));
    }

    @Test
    void lastAnswerCompletesAttemptAndProducesDomainEvent() {
        QuizAttempt attempt = attempt();
        attempt.submitAnswer(
                answerKey, CORRECT_OPTION_ID, "answer-1", START.plusSeconds(5), 1,
                START.plusSeconds(35)
        );
        attempt.recordEarnedXp(10);
        QuizAttemptCompleted event = attempt.completionEvent();
        assertThat(attempt.status()).isEqualTo(AttemptStatus.COMPLETED);
        assertThat(event.attemptId()).isEqualTo(attempt.id());
        assertThat(event.score()).isEqualTo(10);
        assertThat(event.earnedXp()).isEqualTo(10);
    }

    @Test
    void completedPracticeAttemptCanRecordZeroXpButCannotChangeThatDecision() {
        QuizAttempt attempt = attempt();
        attempt.submitAnswer(
                answerKey, CORRECT_OPTION_ID, "answer-1", START.plusSeconds(5), 1,
                START.plusSeconds(35)
        );

        attempt.recordEarnedXp(0);

        assertThat(attempt.earnedXp()).isZero();
        assertThatThrownBy(() -> attempt.recordEarnedXp(10))
                .isInstanceOf(GameplayRuleViolationException.class)
                .hasMessageContaining("already recorded");
    }

    private QuizAttempt attempt() {
        return QuizAttempt.start(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "STANDARD_V1", "QUESTION_30_SECONDS_V1", START, START.plusSeconds(30)
        );
    }
}
