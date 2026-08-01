package com.trt.contentengagement.content.application;

import java.time.Instant;
import java.util.UUID;

public interface AdminAuditLog {

    void record(
            UUID actorId,
            String action,
            String resourceType,
            UUID resourceId,
            Instant occurredAt
    );
}
