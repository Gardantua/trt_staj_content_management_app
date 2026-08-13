package com.trt.contentengagement.gameplay.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "gameplay_answers")
class JpaSubmittedAnswerEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false) private JpaQuizAttemptEntity attempt;
    @Column(name = "question_id", nullable = false) private UUID questionId;
    @Column(name = "selected_option_id") private UUID selectedOptionId;
    @Column(name = "idempotency_key", nullable = false, length = 100) private String idempotencyKey;
    @Column(name = "is_correct", nullable = false) private boolean correct;
    @Column(name = "awarded_points", nullable = false) private int awardedPoints;
    @Column(name = "answered_at", nullable = false) private Instant answeredAt;
    protected JpaSubmittedAnswerEntity() { }
    JpaSubmittedAnswerEntity(
            UUID id, UUID questionId, UUID selectedOptionId, String idempotencyKey,
            boolean correct, int awardedPoints, Instant answeredAt
    ) {
        this.id=id; this.questionId=questionId; this.selectedOptionId=selectedOptionId;
        this.idempotencyKey=idempotencyKey; this.correct=correct;
        this.awardedPoints=awardedPoints; this.answeredAt=answeredAt;
    }
    void attachTo(JpaQuizAttemptEntity attempt){this.attempt=attempt;}
    UUID id(){return id;} UUID questionId(){return questionId;}
    UUID selectedOptionId(){return selectedOptionId;} String idempotencyKey(){return idempotencyKey;}
    boolean correct(){return correct;} int awardedPoints(){return awardedPoints;}
    Instant answeredAt(){return answeredAt;}
}
