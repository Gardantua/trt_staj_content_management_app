CREATE TABLE xp_transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    amount INTEGER NOT NULL,
    reason VARCHAR(40) NOT NULL,
    policy_version VARCHAR(40) NOT NULL,
    reference_key VARCHAR(150) NOT NULL,
    source_attempt_id UUID,
    related_transaction_id UUID,
    created_by UUID,
    note VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_xp_transactions_reference UNIQUE (reference_key),
    CONSTRAINT uq_xp_transactions_source_attempt UNIQUE (source_attempt_id),
    CONSTRAINT fk_xp_transactions_source_attempt
        FOREIGN KEY (source_attempt_id) REFERENCES gameplay_attempts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_xp_transactions_related
        FOREIGN KEY (related_transaction_id) REFERENCES xp_transactions(id) ON DELETE RESTRICT,
    CONSTRAINT ck_xp_transactions_reason CHECK (
        reason IN ('QUIZ_COMPLETED', 'ADMIN_ADJUSTMENT')
    ),
    CONSTRAINT ck_xp_transactions_policy CHECK (
        policy_version IN ('SCORE_MATCH_V1', 'ADMIN_ADJUSTMENT_V1')
    ),
    CONSTRAINT ck_xp_transactions_contract CHECK (
        (reason = 'QUIZ_COMPLETED'
            AND policy_version = 'SCORE_MATCH_V1'
            AND amount >= 0
            AND source_attempt_id IS NOT NULL
            AND related_transaction_id IS NULL
            AND created_by IS NULL
            AND note IS NULL)
        OR
        (reason = 'ADMIN_ADJUSTMENT'
            AND policy_version = 'ADMIN_ADJUSTMENT_V1'
            AND amount <> 0
            AND source_attempt_id IS NULL
            AND related_transaction_id IS NOT NULL
            AND created_by IS NOT NULL
            AND length(trim(note)) > 0)
    ),
    CONSTRAINT ck_xp_transactions_reference_not_blank
        CHECK (length(trim(reference_key)) > 0)
);

CREATE INDEX idx_xp_transactions_user_history
    ON xp_transactions (user_id, occurred_at DESC, id);
CREATE INDEX idx_xp_transactions_related
    ON xp_transactions (related_transaction_id);
