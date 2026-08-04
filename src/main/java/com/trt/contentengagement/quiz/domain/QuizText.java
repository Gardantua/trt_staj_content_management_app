package com.trt.contentengagement.quiz.domain;

final class QuizText {

    private QuizText() {
    }

    static String require(String text, int maximumLength, String fieldName) {
        if (text == null || text.isBlank()) {
            throw new QuizRuleViolationException(
                    "QUIZ_INVALID_" + fieldName.toUpperCase(),
                    fieldName + " must not be blank."
            );
        }
        String normalizedText = text.trim();
        if (normalizedText.length() > maximumLength) {
            throw new QuizRuleViolationException(
                    "QUIZ_INVALID_" + fieldName.toUpperCase(),
                    fieldName + " exceeds the maximum length."
            );
        }
        return normalizedText;
    }

    static String normalizeOptional(String text, int maximumLength, String fieldName) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalizedText = text.trim();
        if (normalizedText.length() > maximumLength) {
            throw new QuizRuleViolationException(
                    "QUIZ_INVALID_" + fieldName.toUpperCase(),
                    fieldName + " exceeds the maximum length."
            );
        }
        return normalizedText;
    }
}
