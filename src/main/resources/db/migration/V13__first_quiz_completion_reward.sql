ALTER TABLE gameplay_attempts
    ADD COLUMN earned_xp INTEGER;

UPDATE gameplay_attempts
SET earned_xp = CASE WHEN status = 'COMPLETED' THEN score ELSE 0 END
WHERE status <> 'ACTIVE';

ALTER TABLE gameplay_attempts
    ADD CONSTRAINT ck_gameplay_attempts_earned_xp CHECK (
        (status = 'ACTIVE' AND earned_xp IS NULL)
        OR (status = 'COMPLETED' AND (earned_xp IS NULL OR earned_xp BETWEEN 0 AND score))
        OR (status = 'EXPIRED' AND earned_xp = 0)
    );

CREATE TABLE gameplay_quiz_reward_claims (
    user_id UUID NOT NULL,
    quiz_id UUID NOT NULL,
    attempt_id UUID NOT NULL UNIQUE,
    claimed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, quiz_id),
    CONSTRAINT fk_gameplay_reward_claim_attempt
        FOREIGN KEY (attempt_id) REFERENCES gameplay_attempts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_gameplay_reward_claim_quiz
        FOREIGN KEY (quiz_id) REFERENCES quiz_definitions(id) ON DELETE RESTRICT
);

INSERT INTO gameplay_quiz_reward_claims (user_id, quiz_id, attempt_id, claimed_at)
SELECT user_id, quiz_id, id, completed_at
FROM (
    SELECT attempt.*,
           ROW_NUMBER() OVER (
               PARTITION BY user_id, quiz_id
               ORDER BY completed_at, id
           ) AS completion_order
    FROM gameplay_attempts AS attempt
    WHERE status = 'COMPLETED'
) AS completed_attempt
WHERE completion_order = 1;

ALTER TABLE xp_transactions
    DROP CONSTRAINT ck_xp_transactions_policy,
    DROP CONSTRAINT ck_xp_transactions_contract;

ALTER TABLE xp_transactions
    ADD CONSTRAINT ck_xp_transactions_policy CHECK (
        policy_version IN (
            'SCORE_MATCH_V1', 'FIRST_COMPLETION_SCORE_V2', 'ADMIN_ADJUSTMENT_V1'
        )
    ),
    ADD CONSTRAINT ck_xp_transactions_contract CHECK (
        (reason = 'QUIZ_COMPLETED'
            AND policy_version IN ('SCORE_MATCH_V1', 'FIRST_COMPLETION_SCORE_V2')
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
            AND note IS NOT NULL)
    );
