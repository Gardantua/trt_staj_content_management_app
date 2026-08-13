package com.trt.contentengagement.gameplay.infrastructure.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.gameplay.application.QuizResultSummary;
import com.trt.contentengagement.gameplay.application.QuizAttemptRepository;
import com.trt.contentengagement.gameplay.domain.AttemptStatus;
import com.trt.contentengagement.gameplay.domain.QuizAttempt;
import com.trt.contentengagement.gameplay.domain.SubmittedAnswer;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

@Repository
public class JpaQuizAttemptRepositoryAdapter implements QuizAttemptRepository {
    private final SpringDataQuizAttemptRepository repository;
    private final JdbcTemplate jdbcTemplate;
    public JpaQuizAttemptRepositoryAdapter(
            SpringDataQuizAttemptRepository repository, JdbcTemplate jdbcTemplate
    ) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }
    @Override public CompletionReward recordFirstCompletionReward(QuizAttempt completedAttempt) {
        if (completedAttempt.earnedXp() == null) {
            jdbcTemplate.update(
                    """
                    INSERT INTO gameplay_quiz_reward_claims (user_id, quiz_id, attempt_id, claimed_at)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (user_id, quiz_id) DO NOTHING
                    """,
                    completedAttempt.userId(), completedAttempt.quizId(), completedAttempt.id(),
                    java.sql.Timestamp.from(completedAttempt.completedAt())
            );
        }
        UUID rewardedAttemptId = jdbcTemplate.queryForObject(
                """
                SELECT attempt_id FROM gameplay_quiz_reward_claims
                WHERE user_id = ? AND quiz_id = ?
                """,
                UUID.class, completedAttempt.userId(), completedAttempt.quizId()
        );
        boolean firstCompletionReward = completedAttempt.id().equals(rewardedAttemptId);
        if (completedAttempt.earnedXp() == null) {
            completedAttempt.recordEarnedXp(firstCompletionReward ? completedAttempt.score() : 0);
            completedAttempt = save(completedAttempt);
        }
        return new CompletionReward(completedAttempt, firstCompletionReward);
    }
    @Override public QuizAttempt save(QuizAttempt attempt) {
        return toDomain(repository.saveAndFlush(toEntity(attempt)));
    }
    @Override public Optional<QuizAttempt> findByIdAndUserId(UUID attemptId, UUID userId) {
        return repository.findByIdAndUserId(attemptId, userId).map(this::toDomain);
    }
    @Override public Optional<QuizAttempt> findActiveByUserIdAndQuizId(UUID userId, UUID quizId) {
        return repository.findByUserIdAndQuizIdAndStatusIn(
                        userId, quizId, List.of(AttemptStatus.ACTIVE, AttemptStatus.AWAITING_NEXT_QUESTION)
                )
                .map(this::toDomain);
    }
    @Override public List<QuizResultSummary> findLatestCompletedResultsByUserId(UUID userId) {
        Map<UUID, QuizResultSummary> latestByQuiz = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                SELECT quiz_id, earned_xp
                FROM gameplay_attempts
                WHERE user_id = ? AND status = 'COMPLETED'
                ORDER BY completed_at DESC NULLS LAST, id DESC
                """,
                (RowCallbackHandler) resultSet -> latestByQuiz.putIfAbsent(
                        resultSet.getObject("quiz_id", UUID.class),
                        new QuizResultSummary(
                                resultSet.getObject("quiz_id", UUID.class),
                                (Integer) resultSet.getObject("earned_xp")
                        )
                ),
                userId
        );
        return new ArrayList<>(latestByQuiz.values());
    }
    private JpaQuizAttemptEntity toEntity(QuizAttempt attempt) {
        JpaQuizAttemptEntity entity = new JpaQuizAttemptEntity(
                attempt.id(), attempt.userId(), attempt.quizId(), attempt.quizVersionId(),
                attempt.scoringPolicyVersion(), attempt.startedAt(), attempt.deadline(),
                attempt.timingPolicyVersion(),
                attempt.status(), attempt.score(), attempt.earnedXp(), attempt.completedAt()
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
                entity.status(), entity.score(), entity.earnedXp(), entity.completedAt(),
                entity.answers().stream().map(answer -> new SubmittedAnswer(
                        answer.id(), answer.questionId(), answer.selectedOptionId(),
                        answer.idempotencyKey(), answer.correct(), answer.awardedPoints(),
                        answer.answeredAt()
                )).toList()
        );
    }
}
