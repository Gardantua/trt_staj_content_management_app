ALTER TABLE xp_transactions
    ADD COLUMN content_id UUID;

UPDATE xp_transactions AS completion
SET content_id = quiz.content_id
FROM gameplay_attempts AS attempt
JOIN quiz_definitions AS quiz ON quiz.id = attempt.quiz_id
WHERE completion.source_attempt_id = attempt.id;

UPDATE xp_transactions AS adjustment
SET content_id = original.content_id
FROM xp_transactions AS original
WHERE adjustment.related_transaction_id = original.id;

ALTER TABLE xp_transactions
    ALTER COLUMN content_id SET NOT NULL,
    ADD CONSTRAINT fk_xp_transactions_content
        FOREIGN KEY (content_id) REFERENCES catalog_contents(id) ON DELETE RESTRICT;

CREATE FUNCTION validate_xp_transaction_content_attribution()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.reason = 'QUIZ_COMPLETED' AND NOT EXISTS (
        SELECT 1
        FROM gameplay_attempts AS attempt
        JOIN quiz_definitions AS quiz ON quiz.id = attempt.quiz_id
        WHERE attempt.id = NEW.source_attempt_id
          AND quiz.content_id = NEW.content_id
          AND attempt.user_id = NEW.user_id
    ) THEN
        RAISE EXCEPTION 'Quiz completion XP content/user does not match its source attempt.'
            USING ERRCODE = '23514';
    END IF;

    IF NEW.reason = 'ADMIN_ADJUSTMENT' AND NOT EXISTS (
        SELECT 1
        FROM xp_transactions AS original
        WHERE original.id = NEW.related_transaction_id
          AND original.reason = 'QUIZ_COMPLETED'
          AND original.content_id = NEW.content_id
          AND original.user_id = NEW.user_id
    ) THEN
        RAISE EXCEPTION 'XP adjustment content/user does not match its original transaction.'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_xp_transactions_content_attribution
BEFORE INSERT OR UPDATE OF user_id, content_id, reason, source_attempt_id,
    related_transaction_id
ON xp_transactions
FOR EACH ROW
EXECUTE FUNCTION validate_xp_transaction_content_attribution();

CREATE INDEX idx_xp_transactions_global_leaderboard
    ON xp_transactions (user_id, occurred_at, id) INCLUDE (amount);

CREATE INDEX idx_xp_transactions_content_leaderboard
    ON xp_transactions (content_id, user_id, occurred_at, id) INCLUDE (amount);
