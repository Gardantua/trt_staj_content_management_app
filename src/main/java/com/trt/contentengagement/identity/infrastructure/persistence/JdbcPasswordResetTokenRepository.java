package com.trt.contentengagement.identity.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.trt.contentengagement.identity.application.PasswordResetTokenRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcPasswordResetTokenRepository implements PasswordResetTokenRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcPasswordResetTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void replaceActiveToken(
            UUID accountId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt
    ) {
        jdbcTemplate.update(
                """
                UPDATE identity_password_reset_tokens
                SET used_at = ?
                WHERE user_account_id = ? AND used_at IS NULL
                """,
                Timestamp.from(createdAt), accountId
        );
        jdbcTemplate.update(
                """
                INSERT INTO identity_password_reset_tokens
                    (id, user_account_id, token_hash, expires_at, created_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(), accountId, tokenHash,
                Timestamp.from(expiresAt), Timestamp.from(createdAt)
        );
    }

    @Override
    public Optional<UUID> consume(String tokenHash, Instant consumedAt) {
        return jdbcTemplate.query(
                """
                UPDATE identity_password_reset_tokens
                SET used_at = ?
                WHERE token_hash = ? AND used_at IS NULL AND expires_at > ?
                RETURNING user_account_id
                """,
                (resultSet, rowNumber) -> resultSet.getObject("user_account_id", UUID.class),
                Timestamp.from(consumedAt), tokenHash, Timestamp.from(consumedAt)
        ).stream().findFirst();
    }
}
