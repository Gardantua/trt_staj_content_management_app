CREATE TABLE leaderboard_xp_entries (
    event_id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    content_id UUID NOT NULL,
    amount INTEGER NOT NULL,
    reason VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_leaderboard_xp_reason
        CHECK (reason IN ('QUIZ_COMPLETED', 'ADMIN_ADJUSTMENT'))
);

CREATE INDEX idx_leaderboard_xp_global
    ON leaderboard_xp_entries (user_id, occurred_at, transaction_id)
    INCLUDE (amount);

CREATE INDEX idx_leaderboard_xp_content
    ON leaderboard_xp_entries (content_id, user_id, occurred_at, transaction_id)
    INCLUDE (amount);
