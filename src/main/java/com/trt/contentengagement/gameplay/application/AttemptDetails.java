package com.trt.contentengagement.gameplay.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.gameplay.domain.QuizAttempt;
import com.trt.contentengagement.quiz.application.GameplayQuizSnapshot;

public record AttemptDetails(
        UUID attemptId, UUID quizId, UUID quizVersionId, String status,
        String timingPolicyVersion,
        int score, Integer earnedXp,
        Instant startedAt, Instant deadline, Instant completedAt,
        int answeredQuestionCount, int totalQuestionCount,
        QuestionView currentQuestion, List<AnswerFeedback> submittedAnswers
) {
    public static AttemptDetails from(QuizAttempt attempt, GameplayQuizSnapshot snapshot) {
        return from(attempt, snapshot, null);
    }

    public static AttemptDetails from(
            QuizAttempt attempt, GameplayQuizSnapshot snapshot, Integer earnedXp
    ) {
        QuestionView current = attempt.status().name().equals("ACTIVE")
                && attempt.answers().size() < snapshot.questions().size()
                ? QuestionView.from(snapshot.questions().get(attempt.answers().size())) : null;
        List<AnswerFeedback> feedback = attempt.answers().stream().map(answer -> {
            GameplayQuizSnapshot.QuestionSnapshot question = snapshot.questions().stream()
                    .filter(candidate -> candidate.questionId().equals(answer.questionId()))
                    .findFirst().orElseThrow();
            return new AnswerFeedback(
                    answer.questionId(), answer.selectedOptionId(), answer.correct(),
                    question.correctOptionId(), answer.awardedPoints()
            );
        }).toList();
        return new AttemptDetails(
                attempt.id(), attempt.quizId(), attempt.quizVersionId(), attempt.status().name(),
                attempt.timingPolicyVersion(),
                attempt.score(), earnedXp,
                attempt.startedAt(), attempt.deadline(), attempt.completedAt(),
                attempt.answers().size(), snapshot.questions().size(), current, feedback
        );
    }
    public record QuestionView(
            UUID questionId, int questionOrder, String prompt, String difficulty,
            VisualView visual, String accessiblePrompt,
            List<OptionView> options
    ) {
        static QuestionView from(GameplayQuizSnapshot.QuestionSnapshot question) {
            return new QuestionView(
                    question.questionId(), question.questionOrder(), question.prompt(),
                    question.difficulty(), VisualView.from(question.visual()),
                    question.accessiblePrompt(), question.options().stream()
                    .map(option -> new OptionView(option.optionId(), option.optionOrder(), option.text()))
                    .toList()
            );
        }
    }
    public record VisualView(
            UUID mediaAssetId, String contentUrl, String role, String alternativeText
    ) {
        static VisualView from(GameplayQuizSnapshot.VisualSnapshot visual) {
            return new VisualView(
                    visual.mediaAssetId(), visual.contentUrl(), visual.role(),
                    visual.alternativeText()
            );
        }
    }
    public record OptionView(UUID optionId, int optionOrder, String text) { }
    public record AnswerFeedback(
            UUID questionId, UUID selectedOptionId, boolean correct,
            String resultStatus, UUID correctOptionId, int awardedPoints
    ) {
        public AnswerFeedback(
                UUID questionId, UUID selectedOptionId, boolean correct,
                UUID correctOptionId, int awardedPoints
        ) {
            this(
                    questionId, selectedOptionId, correct,
                    correct ? "CORRECT" : "INCORRECT", correctOptionId, awardedPoints
            );
        }
    }
}
