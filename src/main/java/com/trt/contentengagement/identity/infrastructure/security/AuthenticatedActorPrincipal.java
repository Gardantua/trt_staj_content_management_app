package com.trt.contentengagement.identity.infrastructure.security;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.trt.contentengagement.identity.domain.UserRole;

public record AuthenticatedActorPrincipal(UUID actorId, Set<UserRole> roles) {

    public AuthenticatedActorPrincipal {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(roles, "roles must not be null");

        if (roles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }

        roles = Set.copyOf(roles);
    }
}
