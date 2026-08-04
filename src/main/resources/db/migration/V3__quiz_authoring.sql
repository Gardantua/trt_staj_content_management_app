CREATE TABLE quiz_definitions (
    id UUID PRIMARY KEY,
    content_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_quiz_definitions_content
        FOREIGN KEY (content_id) REFERENCES catalog_contents(id) ON DELETE RESTRICT
);

CREATE TABLE quiz_versions (
    id UUID PRIMARY KEY,
    quiz_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL,
    scoring_policy_version VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    CONSTRAINT fk_quiz_versions_quiz
        FOREIGN KEY (quiz_id) REFERENCES quiz_definitions(id) ON DELETE CASCADE,
    CONSTRAINT uq_quiz_versions_number UNIQUE (quiz_id, version_number),
    CONSTRAINT ck_quiz_versions_number_positive CHECK (version_number > 0),
    CONSTRAINT ck_quiz_versions_title_not_blank CHECK (length(trim(title)) > 0),
    CONSTRAINT ck_quiz_versions_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_quiz_versions_scoring_policy
        CHECK (scoring_policy_version IN ('STANDARD_V1')),
    CONSTRAINT ck_quiz_versions_publication_times CHECK (
        (status = 'DRAFT' AND published_at IS NULL AND archived_at IS NULL)
        OR (status = 'PUBLISHED' AND published_at IS NOT NULL AND archived_at IS NULL)
        OR (status = 'ARCHIVED' AND published_at IS NOT NULL AND archived_at IS NOT NULL)
    )
);

CREATE TABLE quiz_questions (
    id UUID PRIMARY KEY,
    quiz_version_id UUID NOT NULL,
    question_order INTEGER NOT NULL,
    prompt VARCHAR(1000) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    CONSTRAINT fk_quiz_questions_version
        FOREIGN KEY (quiz_version_id) REFERENCES quiz_versions(id) ON DELETE CASCADE,
    CONSTRAINT uq_quiz_questions_order UNIQUE (quiz_version_id, question_order),
    CONSTRAINT ck_quiz_questions_order_positive CHECK (question_order > 0),
    CONSTRAINT ck_quiz_questions_prompt_not_blank CHECK (length(trim(prompt)) > 0),
    CONSTRAINT ck_quiz_questions_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'))
);

CREATE TABLE quiz_answer_options (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL,
    option_order INTEGER NOT NULL,
    option_text VARCHAR(500) NOT NULL,
    is_correct BOOLEAN NOT NULL,
    CONSTRAINT fk_quiz_answer_options_question
        FOREIGN KEY (question_id) REFERENCES quiz_questions(id) ON DELETE CASCADE,
    CONSTRAINT uq_quiz_answer_options_order UNIQUE (question_id, option_order),
    CONSTRAINT ck_quiz_answer_options_order_positive CHECK (option_order > 0),
    CONSTRAINT ck_quiz_answer_options_text_not_blank CHECK (length(trim(option_text)) > 0)
);

CREATE UNIQUE INDEX uq_quiz_versions_single_draft
    ON quiz_versions (quiz_id) WHERE status = 'DRAFT';

CREATE UNIQUE INDEX uq_quiz_answer_options_single_correct
    ON quiz_answer_options (question_id) WHERE is_correct;

CREATE INDEX idx_quiz_definitions_content ON quiz_definitions (content_id);
CREATE INDEX idx_quiz_versions_quiz ON quiz_versions (quiz_id, version_number);
CREATE INDEX idx_quiz_versions_public ON quiz_versions (quiz_id) WHERE status = 'PUBLISHED';
CREATE INDEX idx_quiz_questions_version ON quiz_questions (quiz_version_id, question_order);
CREATE INDEX idx_quiz_answer_options_question
    ON quiz_answer_options (question_id, option_order);
