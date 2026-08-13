INSERT INTO xp_transactions (
    id,
    user_id,
    content_id,
    amount,
    reason,
    policy_version,
    reference_key,
    source_attempt_id,
    related_transaction_id,
    created_by,
    note,
    occurred_at
)
SELECT
    md5('ZERO_FIRST_COMPLETION:' || attempt.id::text)::uuid,
    reward_claim.user_id,
    quiz.content_id,
    0,
    'QUIZ_COMPLETED',
    'FIRST_COMPLETION_SCORE_V2',
    'QUIZ_ATTEMPT:' || attempt.id,
    attempt.id,
    NULL,
    NULL,
    NULL,
    attempt.completed_at
FROM gameplay_quiz_reward_claims AS reward_claim
JOIN gameplay_attempts AS attempt ON attempt.id = reward_claim.attempt_id
JOIN quiz_definitions AS quiz ON quiz.id = reward_claim.quiz_id
WHERE attempt.score = 0
  AND NOT EXISTS (
      SELECT 1
      FROM xp_transactions AS existing_transaction
      WHERE existing_transaction.source_attempt_id = attempt.id
  );
