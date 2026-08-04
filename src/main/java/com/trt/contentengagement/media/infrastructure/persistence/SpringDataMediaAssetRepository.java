package com.trt.contentengagement.media.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMediaAssetRepository extends JpaRepository<JpaMediaAssetEntity, UUID> {
}
