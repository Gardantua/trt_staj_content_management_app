package com.trt.contentengagement.quiz.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.quiz.domain.QuestionDifficulty;
import com.trt.contentengagement.quiz.domain.VisualRole;
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
@Table(name = "quiz_questions")
class JpaQuizQuestionEntity {

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_version_id", nullable = false)
    private JpaQuizVersionEntity quizVersion;
    @Column(name = "question_order", nullable = false)
    private int questionOrder;
    @Column(nullable = false, length = 1000)
    private String prompt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionDifficulty difficulty;
    @Column(name = "visual_media_id")
    private UUID visualMediaId;
    @Enumerated(EnumType.STRING)
    @Column(name = "visual_role", length = 30)
    private VisualRole visualRole;
    @Column(name = "visual_alternative_text", length = 500)
    private String visualAlternativeText;
    @Column(name = "accessible_prompt", length = 1000)
    private String accessiblePrompt;
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionOrder ASC")
    private List<JpaAnswerOptionEntity> answerOptions = new ArrayList<>();

    protected JpaQuizQuestionEntity() {
    }

    JpaQuizQuestionEntity(
            UUID id,
            int questionOrder,
            String prompt,
            QuestionDifficulty difficulty,
            UUID visualMediaId,
            VisualRole visualRole,
            String visualAlternativeText,
            String accessiblePrompt
    ) {
        this.id = id;
        this.questionOrder = questionOrder;
        this.prompt = prompt;
        this.difficulty = difficulty;
        this.visualMediaId = visualMediaId;
        this.visualRole = visualRole;
        this.visualAlternativeText = visualAlternativeText;
        this.accessiblePrompt = accessiblePrompt;
    }

    void attachTo(JpaQuizVersionEntity quizVersion) { this.quizVersion = quizVersion; }
    void addAnswerOption(JpaAnswerOptionEntity answerOption) {
        answerOptions.add(answerOption);
        answerOption.attachTo(this);
    }
    UUID id() { return id; }
    int questionOrder() { return questionOrder; }
    String prompt() { return prompt; }
    QuestionDifficulty difficulty() { return difficulty; }
    UUID visualMediaId() { return visualMediaId; }
    VisualRole visualRole() { return visualRole; }
    String visualAlternativeText() { return visualAlternativeText; }
    String accessiblePrompt() { return accessiblePrompt; }
    List<JpaAnswerOptionEntity> answerOptions() { return answerOptions; }
}
