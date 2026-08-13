ALTER TABLE quiz_definitions
    DROP CONSTRAINT fk_quiz_definitions_content;

ALTER TABLE quiz_definitions
    ADD CONSTRAINT fk_quiz_definitions_content
        FOREIGN KEY (content_id) REFERENCES catalog_contents(id) ON DELETE CASCADE;
