package com.trt.contentengagement.identity.application;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.trt.contentengagement.identity.domain.UserRole;

public record CurrentActor(UUID actorId, Set<UserRole> roles) {

    public CurrentActor {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(roles, "roles must not be null");

        if (roles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }

        roles = Set.copyOf(roles);
    }
}
