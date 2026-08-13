UPDATE quiz_questions SET difficulty = 'MEDIUM' WHERE difficulty <> 'MEDIUM';

ALTER TABLE quiz_questions DROP CONSTRAINT ck_quiz_questions_difficulty;
ALTER TABLE quiz_questions
    ADD CONSTRAINT ck_quiz_questions_difficulty CHECK (difficulty = 'MEDIUM');

CREATE OR REPLACE FUNCTION enforce_four_options_from_question()
RETURNS TRIGGER AS $$
DECLARE
    affected_question_id UUID := COALESCE(NEW.id, OLD.id);
BEGIN
    IF EXISTS (SELECT 1 FROM quiz_questions WHERE id = affected_question_id)
       AND (SELECT COUNT(*) FROM quiz_answer_options
            WHERE question_id = affected_question_id) <> 4 THEN
        RAISE EXCEPTION 'Question % must contain exactly four answer options',
            affected_question_id USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION enforce_four_options_from_option()
RETURNS TRIGGER AS $$
DECLARE
    affected_question_id UUID := COALESCE(NEW.question_id, OLD.question_id);
BEGIN
    IF EXISTS (SELECT 1 FROM quiz_questions WHERE id = affected_question_id)
       AND (SELECT COUNT(*) FROM quiz_answer_options
            WHERE question_id = affected_question_id) <> 4 THEN
        RAISE EXCEPTION 'Question % must contain exactly four answer options',
            affected_question_id USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_quiz_question_four_options
AFTER INSERT OR UPDATE ON quiz_questions
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION enforce_four_options_from_question();

CREATE CONSTRAINT TRIGGER trg_quiz_option_four_options
AFTER INSERT OR UPDATE OR DELETE ON quiz_answer_options
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION enforce_four_options_from_option();

ALTER TABLE gameplay_attempts DROP CONSTRAINT ck_gameplay_attempts_timing_policy;
ALTER TABLE gameplay_attempts
    ADD CONSTRAINT ck_gameplay_attempts_timing_policy CHECK (
        timing_policy_version IN (
            'STANDARD_V1', 'EXTENDED_V1', 'QUESTION_30_SECONDS_V1'
        )
    );

ALTER TABLE gameplay_answers ALTER COLUMN selected_option_id DROP NOT NULL;
ALTER TABLE gameplay_answers
    ADD CONSTRAINT ck_gameplay_answers_timeout_selection CHECK (
        selected_option_id IS NOT NULL OR (is_correct = FALSE AND awarded_points = 0)
    );
