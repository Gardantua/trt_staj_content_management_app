package com.trt.contentengagement.content.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.content.domain.PublicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataContentRepository extends JpaRepository<JpaContentEntity, UUID> {

    @Override
    Optional<JpaContentEntity> findById(UUID contentId);

    Optional<JpaContentEntity> findByIdAndPublicationStatus(
            UUID contentId,
            PublicationStatus publicationStatus
    );

    Page<JpaContentEntity> findAllByPublicationStatus(
            PublicationStatus publicationStatus,
            Pageable pageable
    );

    Page<JpaContentEntity> findAllByTitleContainingIgnoreCase(
            String titleQuery,
            Pageable pageable
    );
}
