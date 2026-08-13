package com.trt.contentengagement.quiz.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Question(
        UUID id,
        int questionOrder,
        String prompt,
        QuestionDifficulty difficulty,
        UUID visualMediaId,
        VisualRole visualRole,
        String visualAlternativeText,
        String accessiblePrompt,
        List<AnswerOption> answerOptions
) {

    public Question {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(difficulty, "difficulty must not be null");
        if (questionOrder < 1) {
            throw new QuizRuleViolationException(
                    "QUIZ_INVALID_QUESTION_ORDER",
                    "Question order must be positive."
            );
        }
        prompt = QuizText.require(prompt, 1000, "question prompt");
        VisualContract visualContract = normalizeVisualContract(
                visualMediaId, visualRole, visualAlternativeText, accessiblePrompt
        );
        visualAlternativeText = visualContract.alternativeText();
        accessiblePrompt = visualContract.accessiblePrompt();
        answerOptions = List.copyOf(Objects.requireNonNull(
                answerOptions,
                "answerOptions must not be null"
        ));
        if (answerOptions.size() != 4) {
            throw new QuizRuleViolationException(
                    "QUIZ_REQUIRES_FOUR_OPTIONS",
                    "Every question must contain exactly four answer options."
            );
        }
        if (answerOptions.stream().filter(AnswerOption::correct).count() > 1) {
            throw new QuizRuleViolationException(
                    "QUIZ_MULTIPLE_CORRECT_OPTIONS",
                    "A question cannot contain more than one correct answer option."
            );
        }
        long distinctOrders = answerOptions.stream().map(AnswerOption::optionOrder).distinct().count();
        if (distinctOrders != answerOptions.size()) {
            throw new QuizRuleViolationException(
                    "QUIZ_DUPLICATE_OPTION_ORDER",
                    "Answer option order must be unique within a question."
            );
        }
    }

    public static Question create(
            int questionOrder,
            String prompt,
            QuestionDifficulty difficulty,
            UUID visualMediaId,
            VisualRole visualRole,
            String visualAlternativeText,
            String accessiblePrompt,
            List<OptionDraft> optionDrafts
    ) {
        List<AnswerOption> answerOptions = Objects.requireNonNull(
                optionDrafts,
                "optionDrafts must not be null"
        ).stream()
                .map(optionDraft -> AnswerOption.create(
                        optionDraft.optionOrder(),
                        optionDraft.text(),
                        optionDraft.correct()
                ))
                .toList();
        return new Question(
                UUID.randomUUID(),
                questionOrder,
                prompt,
                difficulty,
                visualMediaId,
                visualRole,
                visualAlternativeText,
                accessiblePrompt,
                answerOptions
        );
    }

    Question copyWithNewIds() {
        return new Question(
                UUID.randomUUID(),
                questionOrder,
                prompt,
                difficulty,
                visualMediaId,
                visualRole,
                visualAlternativeText,
                accessiblePrompt,
                answerOptions.stream().map(AnswerOption::copyWithNewId).toList()
        );
    }

    void validateForPublication() {
        if (answerOptions.stream().filter(AnswerOption::correct).count() != 1) {
            throw new QuizRuleViolationException(
                    "QUIZ_QUESTION_REQUIRES_CORRECT_OPTION",
                    "Every published question requires exactly one correct answer option."
            );
        }
        if (visualRole == VisualRole.INFORMATIVE) {
            String correctAnswer = answerOptions.stream()
                    .filter(AnswerOption::correct)
                    .findFirst()
                    .orElseThrow()
                    .text()
                    .toLowerCase(java.util.Locale.ROOT);
            if (visualAlternativeText.toLowerCase(java.util.Locale.ROOT).contains(correctAnswer)
                    || accessiblePrompt.toLowerCase(java.util.Locale.ROOT).contains(correctAnswer)) {
                throw new QuizRuleViolationException(
                        "QUIZ_ACCESSIBLE_TEXT_REVEALS_ANSWER",
                        "Accessible visual text cannot contain the correct answer."
                );
            }
        }
    }

    private static VisualContract normalizeVisualContract(
            UUID visualMediaId,
            VisualRole visualRole,
            String visualAlternativeText,
            String accessiblePrompt
    ) {
        if (visualMediaId == null && visualRole == null
                && visualAlternativeText == null && accessiblePrompt == null) {
            return new VisualContract(null, null);
        }
        if (visualMediaId == null || visualRole == null) {
            throw new QuizRuleViolationException(
                    "QUIZ_VISUAL_CONTRACT_INVALID", "Visual media and role must be provided together."
            );
        }
        if (visualRole == VisualRole.DECORATIVE) {
            if (visualAlternativeText != null || accessiblePrompt != null) {
                throw new QuizRuleViolationException(
                        "QUIZ_DECORATIVE_VISUAL_HAS_TEXT",
                        "Decorative question visuals cannot carry alternative question content."
                );
            }
            return new VisualContract(null, null);
        }
        return new VisualContract(
                QuizText.require(visualAlternativeText, 500, "visual alternative text"),
                QuizText.require(accessiblePrompt, 1000, "accessible prompt")
        );
    }

    private record VisualContract(String alternativeText, String accessiblePrompt) { }

    public record OptionDraft(int optionOrder, String text, boolean correct) {
    }
}
