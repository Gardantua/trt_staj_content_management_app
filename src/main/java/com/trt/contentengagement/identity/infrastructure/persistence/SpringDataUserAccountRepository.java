package com.trt.contentengagement.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.identity.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserAccountRepository extends JpaRepository<JpaUserAccountEntity, UUID> {
    Optional<JpaUserAccountEntity> findByNormalizedEmail(String normalizedEmail);

    boolean existsByRole(UserRole role);
}
