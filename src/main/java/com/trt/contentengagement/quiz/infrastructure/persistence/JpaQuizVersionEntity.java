package com.trt.contentengagement.quiz.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.quiz.domain.QuizVersionStatus;
import com.trt.contentengagement.quiz.domain.ScoringPolicyVersion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_versions")
class JpaQuizVersionEntity {

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private JpaQuizEntity quiz;
    @Column(name = "version_number", nullable = false)
    private int versionNumber;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(length = 2000)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizVersionStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_policy_version", nullable = false, length = 40)
    private ScoringPolicyVersion scoringPolicyVersion;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "archived_at")
    private Instant archivedAt;
    @Column(name = "fallback_media_id")
    private UUID fallbackMediaId;
    @Column(name = "fallback_alternative_text", length = 500)
    private String fallbackAlternativeText;
    @OneToMany(mappedBy = "quizVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    private List<JpaQuizQuestionEntity> questions = new ArrayList<>();

    protected JpaQuizVersionEntity() {
    }

    JpaQuizVersionEntity(
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
            String fallbackAlternativeText
    ) {
        this.id = id;
        this.versionNumber = versionNumber;
        this.title = title;
        this.description = description;
        this.status = status;
        this.scoringPolicyVersion = scoringPolicyVersion;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.archivedAt = archivedAt;
        this.fallbackMediaId = fallbackMediaId;
        this.fallbackAlternativeText = fallbackAlternativeText;
    }

    void attachTo(JpaQuizEntity quiz) { this.quiz = quiz; }
    void addQuestion(JpaQuizQuestionEntity question) {
        questions.add(question);
        question.attachTo(this);
    }
    UUID id() { return id; }
    int versionNumber() { return versionNumber; }
    String title() { return title; }
    String description() { return description; }
    QuizVersionStatus status() { return status; }
    ScoringPolicyVersion scoringPolicyVersion() { return scoringPolicyVersion; }
    Instant createdAt() { return createdAt; }
    Instant publishedAt() { return publishedAt; }
    Instant archivedAt() { return archivedAt; }
    UUID fallbackMediaId() { return fallbackMediaId; }
    String fallbackAlternativeText() { return fallbackAlternativeText; }
    List<JpaQuizQuestionEntity> questions() { return questions; }
}
