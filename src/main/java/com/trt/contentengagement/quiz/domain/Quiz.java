package com.trt.contentengagement.quiz.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Quiz {

    private final UUID id;
    private final UUID contentId;
    private final QuizScopeType scopeType;
    private final UUID seasonId;
    private final UUID episodeId;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<QuizVersion> versions;

    private Quiz(
            UUID id,
            UUID contentId,
            QuizScopeType scopeType,
            UUID seasonId,
            UUID episodeId,
            Instant createdAt,
            Instant updatedAt,
            List<QuizVersion> versions
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.contentId = Objects.requireNonNull(contentId, "contentId must not be null");
        this.scopeType = Objects.requireNonNull(scopeType, "scopeType must not be null");
        this.seasonId = seasonId;
        this.episodeId = episodeId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.versions = new ArrayList<>(Objects.requireNonNull(versions, "versions must not be null"));
        ensureScopeInvariant();
        ensureVersionInvariants();
    }

    public static Quiz create(
            UUID contentId,
            String title,
            String description,
            Instant createdAt
    ) {
        return create(
                contentId, QuizScopeType.CONTENT, null, null,
                title, description, createdAt
        );
    }

    public static Quiz create(
            UUID contentId,
            QuizScopeType scopeType,
            UUID seasonId,
            UUID episodeId,
            String title,
            String description,
            Instant createdAt
    ) {
        return new Quiz(
                UUID.randomUUID(), contentId, scopeType, seasonId, episodeId,
                createdAt, createdAt,
                List.of(QuizVersion.createFirstDraft(title, description, createdAt))
        );
    }

    public static Quiz rehydrate(
            UUID id,
            UUID contentId,
            QuizScopeType scopeType,
            UUID seasonId,
            UUID episodeId,
            Instant createdAt,
            Instant updatedAt,
            List<QuizVersion> versions
    ) {
        return new Quiz(
                id, contentId, scopeType, seasonId, episodeId,
                createdAt, updatedAt, versions
        );
    }

    private void ensureScopeInvariant() {
        boolean valid = switch (scopeType) {
            case CONTENT -> seasonId == null && episodeId == null;
            case SEASON -> seasonId != null && episodeId == null;
            case EPISODE -> seasonId != null && episodeId != null;
        };
        if (!valid) {
            throw new QuizRuleViolationException(
                    "QUIZ_SCOPE_INVALID",
                    "Quiz scope does not match its season and episode references."
            );
        }
    }

    public QuizVersion createDraftFromPublished(Instant occurredAt) {
        if (versions.stream().anyMatch(version -> version.status() == QuizVersionStatus.DRAFT)) {
            throw new QuizRuleViolationException(
                    "QUIZ_DRAFT_ALREADY_EXISTS",
                    "A quiz can have only one draft version at a time."
            );
        }
        QuizVersion publishedVersion = publishedVersion();
        int nextVersionNumber = versions.stream()
                .mapToInt(QuizVersion::versionNumber)
                .max()
                .orElseThrow() + 1;
        QuizVersion draftVersion = publishedVersion.copyAsDraft(nextVersionNumber, occurredAt);
        versions.add(draftVersion);
        updatedAt = occurredAt;
        return draftVersion;
    }

    public void updateDraft(UUID versionId, String title, String description, Instant occurredAt) {
        requireVersion(versionId).updateDetails(title, description);
        updatedAt = occurredAt;
    }

    public Question addQuestion(
            UUID versionId,
            int questionOrder,
            String prompt,
            QuestionDifficulty difficulty,
            UUID visualMediaId,
            VisualRole visualRole,
            String visualAlternativeText,
            String accessiblePrompt,
            List<Question.OptionDraft> optionDrafts,
            Instant occurredAt
    ) {
        Question question = requireVersion(versionId).addQuestion(
                questionOrder, prompt, difficulty, visualMediaId, visualRole,
                visualAlternativeText, accessiblePrompt, optionDrafts
        );
        updatedAt = occurredAt;
        return question;
    }

    public void updateQuestion(
            UUID versionId,
            UUID questionId,
            int questionOrder,
            String prompt,
            QuestionDifficulty difficulty,
            UUID visualMediaId,
            VisualRole visualRole,
            String visualAlternativeText,
            String accessiblePrompt,
            List<Question.OptionDraft> optionDrafts,
            Instant occurredAt
    ) {
        requireVersion(versionId).updateQuestion(
                questionId, questionOrder, prompt, difficulty, visualMediaId, visualRole,
                visualAlternativeText, accessiblePrompt, optionDrafts
        );
        updatedAt = occurredAt;
    }

    public void removeQuestion(
            UUID versionId,
            UUID questionId,
            Instant occurredAt
    ) {
        requireVersion(versionId).removeQuestion(questionId);
        updatedAt = occurredAt;
    }

    public void publish(
            UUID versionId, UUID fallbackMediaId,
            String fallbackAlternativeText, Instant occurredAt
    ) {
        QuizVersion versionToPublish = requireVersion(versionId);
        versionToPublish.publish(fallbackMediaId, fallbackAlternativeText, occurredAt);
        versions.stream()
                .filter(version -> !version.id().equals(versionId))
                .filter(version -> version.status() == QuizVersionStatus.PUBLISHED)
                .forEach(version -> version.archive(occurredAt));
        updatedAt = occurredAt;
    }

    public void archive(UUID versionId, Instant occurredAt) {
        requireVersion(versionId).archive(occurredAt);
        updatedAt = occurredAt;
    }

    public void retire(Instant occurredAt) {
        List<QuizVersion> publishedVersions = versions.stream()
                .filter(version -> version.status() == QuizVersionStatus.PUBLISHED)
                .toList();
        if (publishedVersions.isEmpty()) {
            throw new QuizRuleViolationException(
                    "QUIZ_ACTIVE_VERSION_NOT_FOUND",
                    "The quiz does not have an active published version."
            );
        }
        publishedVersions.forEach(version -> version.archive(occurredAt));
        versions.removeIf(version -> version.status() == QuizVersionStatus.DRAFT);
        updatedAt = occurredAt;
    }

    public boolean hasPublicationHistory() {
        return versions.stream().anyMatch(version -> version.status() != QuizVersionStatus.DRAFT);
    }

    public QuizVersion requireVersion(UUID versionId) {
        return versions.stream()
                .filter(version -> version.id().equals(versionId))
                .findFirst()
                .orElseThrow(() -> new QuizRuleViolationException(
                        "QUIZ_VERSION_NOT_FOUND",
                        "The requested quiz version was not found."
                ));
    }

    public QuizVersion publishedVersion() {
        return versions.stream()
                .filter(version -> version.status() == QuizVersionStatus.PUBLISHED)
                .findFirst()
                .orElseThrow(() -> new QuizRuleViolationException(
                        "QUIZ_PUBLISHED_VERSION_NOT_FOUND",
                        "The quiz does not have a published version."
                ));
    }

    private void ensureVersionInvariants() {
        long distinctVersionNumbers = versions.stream()
                .map(QuizVersion::versionNumber)
                .distinct()
                .count();
        if (distinctVersionNumbers != versions.size()) {
            throw new QuizRuleViolationException(
                    "QUIZ_DUPLICATE_VERSION_NUMBER",
                    "Quiz version numbers must be unique."
            );
        }
        if (versions.stream().filter(version -> version.status() == QuizVersionStatus.DRAFT).count() > 1) {
            throw new QuizRuleViolationException(
                    "QUIZ_DRAFT_ALREADY_EXISTS",
                    "A quiz can have only one draft version at a time."
            );
        }
        if (versions.stream().filter(version -> version.status() == QuizVersionStatus.PUBLISHED).count() > 1) {
            throw new QuizRuleViolationException(
                    "QUIZ_MULTIPLE_PUBLISHED_VERSIONS",
                    "A quiz can have only one active published version."
            );
        }
    }

    public UUID id() { return id; }
    public UUID contentId() { return contentId; }
    public QuizScopeType scopeType() { return scopeType; }
    public UUID seasonId() { return seasonId; }
    public UUID episodeId() { return episodeId; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public List<QuizVersion> versions() {
        return versions.stream().sorted(Comparator.comparingInt(QuizVersion::versionNumber)).toList();
    }
}
