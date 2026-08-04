package com.trt.contentengagement.quiz.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class QuizVersion {

    private final UUID id;
    private final int versionNumber;
    private String title;
    private String description;
    private QuizVersionStatus status;
    private final ScoringPolicyVersion scoringPolicyVersion;
    private final Instant createdAt;
    private Instant publishedAt;
    private Instant archivedAt;
    private UUID fallbackMediaId;
    private String fallbackAlternativeText;
    private final List<Question> questions;

    private QuizVersion(
            UUID id,
            int versionNumber,
            String title,
            String description,
            QuizVersionStatus status,
            ScoringPolicyVersion scoringPolicyVersion,
            Instant createdAt,
            Instant publishedAt,
            Instant archivedAt,
            UUID fallbackMediaId,
            String fallbackAlternativeText,
            List<Question> questions
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        if (versionNumber < 1) {
            throw new QuizRuleViolationException(
                    "QUIZ_INVALID_VERSION_NUMBER",
                    "Quiz version number must be positive."
            );
        }
        this.versionNumber = versionNumber;
        this.title = QuizText.require(title, 200, "title");
        this.description = QuizText.normalizeOptional(description, 2000, "description");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.scoringPolicyVersion = Objects.requireNonNull(
                scoringPolicyVersion,
                "scoringPolicyVersion must not be null"
        );
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.publishedAt = publishedAt;
        this.archivedAt = archivedAt;
        this.fallbackMediaId = fallbackMediaId;
        this.fallbackAlternativeText = fallbackAlternativeText;
        this.questions = new ArrayList<>(Objects.requireNonNull(
                questions,
                "questions must not be null"
        ));
        ensureUniqueQuestionOrders();
    }

    static QuizVersion createFirstDraft(String title, String description, Instant createdAt) {
        return new QuizVersion(
                UUID.randomUUID(), 1, title, description, QuizVersionStatus.DRAFT,
                ScoringPolicyVersion.STANDARD_V1, createdAt, null, null,
                null, null, List.of()
        );
    }

    public static QuizVersion rehydrate(
            UUID id,
            int versionNumber,
            String title,
            String description,
            QuizVersionStatus status,
            ScoringPolicyVersion scoringPolicyVersion,
            Instant createdAt,
            Instant publishedAt,
            Instant archivedAt,
            UUID fallbackMediaId,
            String fallbackAlternativeText,
            List<Question> questions
    ) {
        return new QuizVersion(
                id, versionNumber, title, description, status, scoringPolicyVersion,
                createdAt, publishedAt, archivedAt, fallbackMediaId,
                fallbackAlternativeText, questions
        );
    }

    QuizVersion copyAsDraft(int newVersionNumber, Instant createdAt) {
        return new QuizVersion(
                UUID.randomUUID(), newVersionNumber, title, description,
                QuizVersionStatus.DRAFT, scoringPolicyVersion, createdAt, null, null,
                fallbackMediaId, fallbackAlternativeText,
                questions.stream().map(Question::copyWithNewIds).toList()
        );
    }

    void updateDetails(String title, String description) {
        requireDraft();
        this.title = QuizText.require(title, 200, "title");
        this.description = QuizText.normalizeOptional(description, 2000, "description");
    }

    Question addQuestion(
            int questionOrder,
            String prompt,
            QuestionDifficulty difficulty,
            UUID visualMediaId,
            VisualRole visualRole,
            String visualAlternativeText,
            String accessiblePrompt,
            List<Question.OptionDraft> optionDrafts
    ) {
        requireDraft();
        ensureQuestionOrderAvailable(questionOrder, null);
        Question question = Question.create(
                questionOrder, prompt, difficulty, visualMediaId, visualRole,
                visualAlternativeText, accessiblePrompt, optionDrafts
        );
        questions.add(question);
        return question;
    }

    void updateQuestion(
            UUID questionId,
            int questionOrder,
            String prompt,
            QuestionDifficulty difficulty,
            UUID visualMediaId,
            VisualRole visualRole,
            String visualAlternativeText,
            String accessiblePrompt,
            List<Question.OptionDraft> optionDrafts
    ) {
        requireDraft();
        Question currentQuestion = requireQuestion(questionId);
        ensureQuestionOrderAvailable(questionOrder, questionId);
        List<AnswerOption> revisedOptions = optionDrafts.stream()
                .map(optionDraft -> new AnswerOption(
                        currentQuestion.answerOptions().stream()
                                .filter(option -> option.optionOrder() == optionDraft.optionOrder())
                                .map(AnswerOption::id)
                                .findFirst()
                                .orElseGet(UUID::randomUUID),
                        optionDraft.optionOrder(),
                        optionDraft.text(),
                        optionDraft.correct()
                ))
                .toList();
        Question replacement = new Question(
                questionId,
                questionOrder,
                prompt,
                difficulty,
                visualMediaId,
                visualRole,
                visualAlternativeText,
                accessiblePrompt,
                revisedOptions
        );
        questions.replaceAll(question -> question.id().equals(questionId)
                ? replacement
                : question);
    }

    void removeQuestion(UUID questionId) {
        requireDraft();
        if (!questions.removeIf(question -> question.id().equals(questionId))) {
            throw new QuizRuleViolationException(
                    "QUIZ_QUESTION_NOT_FOUND",
                    "The requested quiz question was not found."
            );
        }
    }

    void publish(UUID fallbackMediaId, String fallbackAlternativeText, Instant publishedAt) {
        requireDraft();
        if (questions.isEmpty()) {
            throw new QuizRuleViolationException(
                    "QUIZ_REQUIRES_QUESTION",
                    "A quiz requires at least one question before publication."
            );
        }
        questions.forEach(Question::validateForPublication);
        this.fallbackMediaId = Objects.requireNonNull(
                fallbackMediaId, "fallbackMediaId must not be null"
        );
        this.fallbackAlternativeText = QuizText.require(
                fallbackAlternativeText, 500, "fallback alternative text"
        );
        this.status = QuizVersionStatus.PUBLISHED;
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");
    }

    void archive(Instant archivedAt) {
        if (status != QuizVersionStatus.PUBLISHED) {
            throw new QuizRuleViolationException(
                    "QUIZ_VERSION_NOT_PUBLISHED",
                    "Only a published quiz version can be archived."
            );
        }
        status = QuizVersionStatus.ARCHIVED;
        this.archivedAt = Objects.requireNonNull(archivedAt, "archivedAt must not be null");
    }

    private void requireDraft() {
        if (status != QuizVersionStatus.DRAFT) {
            throw new QuizRuleViolationException(
                    "QUIZ_VERSION_IMMUTABLE",
                    "Published or archived quiz versions cannot be modified in place."
            );
        }
    }

    private Question requireQuestion(UUID questionId) {
        return questions.stream()
                .filter(question -> question.id().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new QuizRuleViolationException(
                        "QUIZ_QUESTION_NOT_FOUND",
                        "The requested quiz question was not found."
                ));
    }

    private void ensureQuestionOrderAvailable(int questionOrder, UUID ignoredQuestionId) {
        boolean duplicateOrder = questions.stream().anyMatch(question ->
                question.questionOrder() == questionOrder
                        && !question.id().equals(ignoredQuestionId));
        if (duplicateOrder) {
            throw new QuizRuleViolationException(
                    "QUIZ_DUPLICATE_QUESTION_ORDER",
                    "Question order must be unique within a quiz version."
            );
        }
    }

    private void ensureUniqueQuestionOrders() {
        long distinctOrders = questions.stream().map(Question::questionOrder).distinct().count();
        if (distinctOrders != questions.size()) {
            throw new QuizRuleViolationException(
                    "QUIZ_DUPLICATE_QUESTION_ORDER",
                    "Question order must be unique within a quiz version."
            );
        }
    }

    public UUID id() { return id; }
    public int versionNumber() { return versionNumber; }
    public String title() { return title; }
    public String description() { return description; }
    public QuizVersionStatus status() { return status; }
    public ScoringPolicyVersion scoringPolicyVersion() { return scoringPolicyVersion; }
    public Instant createdAt() { return createdAt; }
    public Instant publishedAt() { return publishedAt; }
    public Instant archivedAt() { return archivedAt; }
    public UUID fallbackMediaId() { return fallbackMediaId; }
    public String fallbackAlternativeText() { return fallbackAlternativeText; }
    public List<Question> questions() {
        return questions.stream().sorted(Comparator.comparingInt(Question::questionOrder)).toList();
    }
}
