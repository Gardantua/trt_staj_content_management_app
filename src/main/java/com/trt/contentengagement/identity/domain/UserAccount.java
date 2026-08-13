package com.trt.contentengagement.identity.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record UserAccount(
        UUID id,
        String email,
        String normalizedEmail,
        String displayName,
        String passwordHash,
        UserRole role,
        Instant createdAt
) {
    public UserAccount {
        Objects.requireNonNull(id, "id must not be null");
        email = requireTrimmed(email, "email");
        normalizedEmail = requireTrimmed(normalizedEmail, "normalizedEmail");
        displayName = requireTrimmed(displayName, "displayName");
        passwordHash = requireTrimmed(passwordHash, "passwordHash");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static String normalizeEmail(String email) {
        return requireTrimmed(email, "email").toLowerCase(Locale.ROOT);
    }

    public UserAccount withPasswordHash(String newPasswordHash) {
        return new UserAccount(
                id, email, normalizedEmail, displayName, newPasswordHash, role, createdAt
        );
    }

    private static String requireTrimmed(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmedValue;
    }
}
