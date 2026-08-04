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
    private final QuestionAnswerKey answerKey = new QuestionAnswerKey(
            QUESTION_ID, 1, List.of(CORRECT_OPTION_ID, WRONG_OPTION_ID), CORRECT_OPTION_ID
    );

    @Test
    void acceptedAnswerImmediatelyRevealsCorrectOptionAndAwardsServerScore() {
        QuizAttempt attempt = attempt();
        QuizAttempt.AnswerResult result = attempt.submitAnswer(
                answerKey, CORRECT_OPTION_ID, "answer-1", START.plusSeconds(5), 2
        );
        assertThat(result.correct()).isTrue();
        assertThat(result.correctOptionId()).isEqualTo(CORRECT_OPTION_ID);
        assertThat(result.awardedPoints()).isEqualTo(100);
        assertThat(attempt.score()).isEqualTo(100);
    }

    @Test
    void sameQuestionCannotBeAnsweredTwice() {
        QuizAttempt attempt = attempt();
        attempt.submitAnswer(answerKey, WRONG_OPTION_ID, "answer-1", START.plusSeconds(5), 2);
        assertThatThrownBy(() -> attempt.submitAnswer(
                answerKey, CORRECT_OPTION_ID, "answer-2", START.plusSeconds(6), 2
        )).isInstanceOf(GameplayRuleViolationException.class)
                .hasMessage("The question already has a submitted answer.");
    }

    @Test
    void repeatedIdempotencyKeyReturnsSameAnswerButRejectsDifferentPayload() {
        QuizAttempt attempt = attempt();
        QuizAttempt.AnswerResult first = attempt.submitAnswer(
                answerKey, WRONG_OPTION_ID, "same-key", START.plusSeconds(5), 2
        );
        QuizAttempt.AnswerResult repeated = attempt.submitAnswer(
                answerKey, WRONG_OPTION_ID, "same-key", START.plusSeconds(8), 2
        );
        assertThat(repeated).isEqualTo(first);
        assertThatThrownBy(() -> attempt.submitAnswer(
                answerKey, CORRECT_OPTION_ID, "same-key", START.plusSeconds(9), 2
        )).isInstanceOf(GameplayRuleViolationException.class)
                .hasMessageContaining("different answer");
    }

    @Test
    void deadlineExpiresAttemptAndRejectsAnswer() {
        QuizAttempt attempt = attempt();
        assertThat(attempt.expireIfDeadlineReached(START.plusSeconds(300))).isTrue();
        assertThat(attempt.status()).isEqualTo(AttemptStatus.EXPIRED);
        assertThatThrownBy(() -> attempt.submitAnswer(
                answerKey, CORRECT_OPTION_ID, "late", START.plusSeconds(301), 2
        )).isInstanceOf(GameplayRuleViolationException.class);
    }

    @Test
    void lastAnswerCompletesAttemptAndProducesDomainEvent() {
        QuizAttempt attempt = attempt();
        attempt.submitAnswer(answerKey, CORRECT_OPTION_ID, "answer-1", START.plusSeconds(5), 1);
        QuizAttemptCompleted event = attempt.completionEvent();
        assertThat(attempt.status()).isEqualTo(AttemptStatus.COMPLETED);
        assertThat(event.attemptId()).isEqualTo(attempt.id());
        assertThat(event.score()).isEqualTo(100);
    }

    private QuizAttempt attempt() {
        return QuizAttempt.start(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "STANDARD_V1", "STANDARD_V1", START, START.plusSeconds(300)
        );
    }
}
