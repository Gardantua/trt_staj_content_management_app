ALTER TABLE quiz_versions
    ADD CONSTRAINT uq_quiz_versions_id_quiz UNIQUE (id, quiz_id);

ALTER TABLE quiz_answer_options
    ADD CONSTRAINT uq_quiz_answer_options_id_question UNIQUE (id, question_id);

CREATE TABLE gameplay_attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    quiz_id UUID NOT NULL,
    quiz_version_id UUID NOT NULL,
    scoring_policy_version VARCHAR(40) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    deadline TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    score INTEGER NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT fk_gameplay_attempts_quiz
        FOREIGN KEY (quiz_id) REFERENCES quiz_definitions(id) ON DELETE RESTRICT,
    CONSTRAINT fk_gameplay_attempts_quiz_version
        FOREIGN KEY (quiz_version_id, quiz_id)
        REFERENCES quiz_versions(id, quiz_id) ON DELETE RESTRICT,
    CONSTRAINT ck_gameplay_attempts_policy CHECK (scoring_policy_version IN ('STANDARD_V1')),
    CONSTRAINT ck_gameplay_attempts_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'EXPIRED')),
    CONSTRAINT ck_gameplay_attempts_score CHECK (score >= 0),
    CONSTRAINT ck_gameplay_attempts_deadline CHECK (deadline > started_at),
    CONSTRAINT ck_gameplay_attempts_completion CHECK (
        (status = 'ACTIVE' AND completed_at IS NULL)
        OR (status IN ('COMPLETED', 'EXPIRED') AND completed_at IS NOT NULL)
    )
);

CREATE TABLE gameplay_answers (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    question_id UUID NOT NULL,
    selected_option_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    is_correct BOOLEAN NOT NULL,
    awarded_points INTEGER NOT NULL,
    answered_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_gameplay_answers_attempt
        FOREIGN KEY (attempt_id) REFERENCES gameplay_attempts(id) ON DELETE CASCADE,
    CONSTRAINT fk_gameplay_answers_question
        FOREIGN KEY (question_id) REFERENCES quiz_questions(id) ON DELETE RESTRICT,
    CONSTRAINT fk_gameplay_answers_selected_option
        FOREIGN KEY (selected_option_id, question_id)
        REFERENCES quiz_answer_options(id, question_id) ON DELETE RESTRICT,
    CONSTRAINT uq_gameplay_answers_attempt_question UNIQUE (attempt_id, question_id),
    CONSTRAINT uq_gameplay_answers_attempt_idempotency UNIQUE (attempt_id, idempotency_key),
    CONSTRAINT ck_gameplay_answers_points CHECK (awarded_points >= 0),
    CONSTRAINT ck_gameplay_answers_idempotency_not_blank
        CHECK (length(trim(idempotency_key)) > 0)
);

CREATE UNIQUE INDEX uq_gameplay_attempts_single_active
    ON gameplay_attempts (user_id, quiz_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_gameplay_attempts_owner ON gameplay_attempts (user_id, started_at DESC);
CREATE INDEX idx_gameplay_answers_attempt ON gameplay_answers (attempt_id, answered_at);
