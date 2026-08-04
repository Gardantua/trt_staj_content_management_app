package com.trt.contentengagement.gameplay.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.trt.contentengagement.gameplay.domain.AttemptStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "gameplay_attempts")
class JpaQuizAttemptEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "quiz_id", nullable = false) private UUID quizId;
    @Column(name = "quiz_version_id", nullable = false) private UUID quizVersionId;
    @Column(name = "scoring_policy_version", nullable = false, length = 40)
    private String scoringPolicyVersion;
    @Column(name = "timing_policy_version", nullable = false, length = 40)
    private String timingPolicyVersion;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(nullable = false) private Instant deadline;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AttemptStatus status;
    @Column(nullable = false) private int score;
    @Column(name = "completed_at") private Instant completedAt;
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("answeredAt ASC")
    private List<JpaSubmittedAnswerEntity> answers = new ArrayList<>();

    protected JpaQuizAttemptEntity() { }
    JpaQuizAttemptEntity(
            UUID id, UUID userId, UUID quizId, UUID quizVersionId,
            String scoringPolicyVersion, Instant startedAt, Instant deadline,
            String timingPolicyVersion,
            AttemptStatus status, int score, Instant completedAt
    ) {
        this.id=id; this.userId=userId; this.quizId=quizId; this.quizVersionId=quizVersionId;
        this.scoringPolicyVersion=scoringPolicyVersion; this.startedAt=startedAt;
        this.deadline=deadline; this.timingPolicyVersion=timingPolicyVersion;
        this.status=status; this.score=score; this.completedAt=completedAt;
    }
    void addAnswer(JpaSubmittedAnswerEntity answer) { answers.add(answer); answer.attachTo(this); }
    UUID id(){return id;} UUID userId(){return userId;} UUID quizId(){return quizId;}
    UUID quizVersionId(){return quizVersionId;} String scoringPolicyVersion(){return scoringPolicyVersion;}
    String timingPolicyVersion(){return timingPolicyVersion;}
    Instant startedAt(){return startedAt;} Instant deadline(){return deadline;}
    AttemptStatus status(){return status;} int score(){return score;}
    Instant completedAt(){return completedAt;} List<JpaSubmittedAnswerEntity> answers(){return answers;}
}
