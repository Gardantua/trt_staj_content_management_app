package com.trt.contentengagement.gameplay.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.gameplay.application.QuizAttemptRepository;
import com.trt.contentengagement.gameplay.domain.AttemptStatus;
import com.trt.contentengagement.gameplay.domain.QuizAttempt;
import com.trt.contentengagement.gameplay.domain.SubmittedAnswer;
import org.springframework.stereotype.Repository;

@Repository
public class JpaQuizAttemptRepositoryAdapter implements QuizAttemptRepository {
    private final SpringDataQuizAttemptRepository repository;
    public JpaQuizAttemptRepositoryAdapter(SpringDataQuizAttemptRepository repository) {
        this.repository = repository;
    }
    @Override public QuizAttempt save(QuizAttempt attempt) {
        return toDomain(repository.saveAndFlush(toEntity(attempt)));
    }
    @Override public Optional<QuizAttempt> findByIdAndUserId(UUID attemptId, UUID userId) {
        return repository.findByIdAndUserId(attemptId, userId).map(this::toDomain);
    }
    @Override public Optional<QuizAttempt> findActiveByUserIdAndQuizId(UUID userId, UUID quizId) {
        return repository.findByUserIdAndQuizIdAndStatus(userId, quizId, AttemptStatus.ACTIVE)
                .map(this::toDomain);
    }
    private JpaQuizAttemptEntity toEntity(QuizAttempt attempt) {
        JpaQuizAttemptEntity entity = new JpaQuizAttemptEntity(
                attempt.id(), attempt.userId(), attempt.quizId(), attempt.quizVersionId(),
                attempt.scoringPolicyVersion(), attempt.startedAt(), attempt.deadline(),
                attempt.timingPolicyVersion(),
                attempt.status(), attempt.score(), attempt.completedAt()
        );
        attempt.answers().forEach(answer -> entity.addAnswer(new JpaSubmittedAnswerEntity(
                answer.id(), answer.questionId(), answer.selectedOptionId(), answer.idempotencyKey(),
                answer.correct(), answer.awardedPoints(), answer.answeredAt()
        )));
        return entity;
    }
    private QuizAttempt toDomain(JpaQuizAttemptEntity entity) {
        return QuizAttempt.rehydrate(
                entity.id(), entity.userId(), entity.quizId(), entity.quizVersionId(),
                entity.scoringPolicyVersion(), entity.timingPolicyVersion(),
                entity.startedAt(), entity.deadline(),
                entity.status(), entity.score(), entity.completedAt(),
                entity.answers().stream().map(answer -> new SubmittedAnswer(
                        answer.id(), answer.questionId(), answer.selectedOptionId(),
                        answer.idempotencyKey(), answer.correct(), answer.awardedPoints(),
                        answer.answeredAt()
                )).toList()
        );
    }
}
