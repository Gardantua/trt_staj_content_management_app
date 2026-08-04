package com.trt.contentengagement.quiz.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_answer_options")
class JpaAnswerOptionEntity {

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private JpaQuizQuestionEntity question;
    @Column(name = "option_order", nullable = false)
    private int optionOrder;
    @Column(name = "option_text", nullable = false, length = 500)
    private String optionText;
    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    protected JpaAnswerOptionEntity() {
    }

    JpaAnswerOptionEntity(UUID id, int optionOrder, String optionText, boolean correct) {
        this.id = id;
        this.optionOrder = optionOrder;
        this.optionText = optionText;
        this.correct = correct;
    }

    void attachTo(JpaQuizQuestionEntity question) { this.question = question; }
    UUID id() { return id; }
    int optionOrder() { return optionOrder; }
    String optionText() { return optionText; }
    boolean correct() { return correct; }
}
