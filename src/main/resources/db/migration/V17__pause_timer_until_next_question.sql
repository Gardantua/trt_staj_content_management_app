ALTER TABLE gameplay_attempts
    ALTER COLUMN deadline DROP NOT NULL;

ALTER TABLE gameplay_attempts
    DROP CONSTRAINT ck_gameplay_attempts_status,
    DROP CONSTRAINT ck_gameplay_attempts_completion,
    DROP CONSTRAINT ck_gameplay_attempts_earned_xp;

ALTER TABLE gameplay_attempts
    ADD CONSTRAINT ck_gameplay_attempts_status CHECK (
        status IN ('ACTIVE', 'AWAITING_NEXT_QUESTION', 'COMPLETED', 'EXPIRED')
    ),
    ADD CONSTRAINT ck_gameplay_attempts_completion CHECK (
        (status IN ('ACTIVE', 'AWAITING_NEXT_QUESTION') AND completed_at IS NULL)
        OR (status IN ('COMPLETED', 'EXPIRED') AND completed_at IS NOT NULL)
    ),
    ADD CONSTRAINT ck_gameplay_attempts_earned_xp CHECK (
        (status IN ('ACTIVE', 'AWAITING_NEXT_QUESTION') AND earned_xp IS NULL)
        OR (status = 'COMPLETED' AND (earned_xp IS NULL OR earned_xp BETWEEN 0 AND score))
        OR (status = 'EXPIRED' AND earned_xp = 0)
    );

DROP INDEX uq_gameplay_attempts_single_active;

CREATE UNIQUE INDEX uq_gameplay_attempts_single_open
    ON gameplay_attempts (user_id, quiz_id)
    WHERE status IN ('ACTIVE', 'AWAITING_NEXT_QUESTION');
