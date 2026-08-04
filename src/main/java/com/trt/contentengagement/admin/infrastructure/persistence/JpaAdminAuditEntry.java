package com.trt.contentengagement.admin.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_audit_entries")
class JpaAdminAuditEntry {

    @Id
    private UUID id;
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;
    @Column(nullable = false, length = 80)
    private String action;
    @Column(name = "resource_type", nullable = false, length = 40)
    private String resourceType;
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected JpaAdminAuditEntry() {
    }

    JpaAdminAuditEntry(
            UUID id,
            UUID actorId,
            String action,
            String resourceType,
            UUID resourceId,
            Instant occurredAt
    ) {
        this.id = id;
        this.actorId = actorId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.occurredAt = occurredAt;
    }
}
