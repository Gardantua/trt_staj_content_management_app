package com.trt.contentengagement.identity.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {
    void replaceActiveToken(UUID accountId, String tokenHash, Instant expiresAt, Instant createdAt);

    Optional<UUID> consume(String tokenHash, Instant consumedAt);
}
