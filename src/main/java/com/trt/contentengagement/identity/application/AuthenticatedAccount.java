package com.trt.contentengagement.identity.application;

import java.util.Set;
import java.util.UUID;

import com.trt.contentengagement.identity.domain.UserRole;

public record AuthenticatedAccount(
        UUID actorId,
        String email,
        String displayName,
        Set<UserRole> roles
) {
    public AuthenticatedAccount {
        roles = Set.copyOf(roles);
    }
}

