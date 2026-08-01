package com.trt.contentengagement.content.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.content.application.AdminAuditLog;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAdminAuditLogAdapter implements AdminAuditLog {

    private final SpringDataAdminAuditRepository auditRepository;

    public JpaAdminAuditLogAdapter(SpringDataAdminAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public void record(
            UUID actorId,
            String action,
            String resourceType,
            UUID resourceId,
            Instant occurredAt
    ) {
        auditRepository.save(new JpaAdminAuditEntry(
                UUID.randomUUID(),
                actorId,
                action,
                resourceType,
                resourceId,
                occurredAt
        ));
    }
}
