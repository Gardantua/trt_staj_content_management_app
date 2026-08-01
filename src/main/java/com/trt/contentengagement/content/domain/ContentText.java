package com.trt.contentengagement.content.domain;

final class ContentText {

    private ContentText() {
    }

    static String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ContentRuleViolationException("TITLE_REQUIRED", "Title must not be blank.");
        }
        String normalizedTitle = title.trim();
        if (normalizedTitle.length() > 200) {
            throw new ContentRuleViolationException(
                    "TITLE_TOO_LONG",
                    "Title must not exceed 200 characters."
            );
        }
        return normalizedTitle;
    }

    static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String normalizedDescription = description.trim();
        if (normalizedDescription.length() > 2000) {
            throw new ContentRuleViolationException(
                    "DESCRIPTION_TOO_LONG",
                    "Description must not exceed 2000 characters."
            );
        }
        return normalizedDescription;
    }
}
