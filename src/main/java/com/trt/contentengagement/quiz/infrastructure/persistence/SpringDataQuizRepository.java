package com.trt.contentengagement.quiz.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.quiz.domain.QuizVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataQuizRepository extends JpaRepository<JpaQuizEntity, UUID> {

    Optional<JpaQuizEntity> findDistinctByIdAndVersionsStatus(
            UUID quizId,
            QuizVersionStatus status
    );

    List<JpaQuizEntity> findDistinctByContentIdAndVersionsStatusOrderByCreatedAtAsc(
            UUID contentId,
            QuizVersionStatus status
    );

    Optional<JpaQuizEntity> findDistinctByVersionsId(UUID versionId);
}
