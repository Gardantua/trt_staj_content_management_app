package com.trt.contentengagement.gameplay.infrastructure.persistence;

import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

import com.trt.contentengagement.gameplay.domain.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

interface SpringDataQuizAttemptRepository extends JpaRepository<JpaQuizAttemptEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<JpaQuizAttemptEntity> findByIdAndUserId(UUID id, UUID userId);
    Optional<JpaQuizAttemptEntity> findByUserIdAndQuizIdAndStatusIn(
            UUID userId, UUID quizId, Collection<AttemptStatus> statuses
    );
}
