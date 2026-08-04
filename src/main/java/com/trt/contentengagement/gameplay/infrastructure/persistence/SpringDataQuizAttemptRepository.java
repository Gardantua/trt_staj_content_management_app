package com.trt.contentengagement.gameplay.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.gameplay.domain.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataQuizAttemptRepository extends JpaRepository<JpaQuizAttemptEntity, UUID> {
    Optional<JpaQuizAttemptEntity> findByIdAndUserId(UUID id, UUID userId);
    Optional<JpaQuizAttemptEntity> findByUserIdAndQuizIdAndStatus(
            UUID userId, UUID quizId, AttemptStatus status
    );
}
