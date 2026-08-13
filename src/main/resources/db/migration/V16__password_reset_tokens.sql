CREATE TABLE identity_password_reset_tokens (
    id UUID PRIMARY KEY,
    user_account_id UUID NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_identity_password_reset_account
        FOREIGN KEY (user_account_id) REFERENCES identity_user_accounts(id) ON DELETE CASCADE,
    CONSTRAINT uq_identity_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_identity_password_reset_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_identity_password_reset_used_after_creation
        CHECK (used_at IS NULL OR used_at >= created_at)
);

CREATE INDEX ix_identity_password_reset_account
    ON identity_password_reset_tokens (user_account_id);
