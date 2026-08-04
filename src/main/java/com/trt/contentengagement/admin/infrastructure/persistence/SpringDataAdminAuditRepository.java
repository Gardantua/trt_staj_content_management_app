package com.trt.contentengagement.admin.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAdminAuditRepository extends JpaRepository<JpaAdminAuditEntry, UUID> {
}
