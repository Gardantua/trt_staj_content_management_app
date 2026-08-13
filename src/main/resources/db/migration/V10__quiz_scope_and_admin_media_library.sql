ALTER TABLE catalog_seasons
    ADD CONSTRAINT uq_catalog_seasons_id_content UNIQUE (id, content_id);

ALTER TABLE catalog_episodes
    ADD CONSTRAINT uq_catalog_episodes_id_season UNIQUE (id, season_id);

ALTER TABLE quiz_definitions
    ADD COLUMN scope_type VARCHAR(20) NOT NULL DEFAULT 'CONTENT',
    ADD COLUMN scope_season_id UUID,
    ADD COLUMN scope_episode_id UUID;

ALTER TABLE quiz_definitions
    ADD CONSTRAINT ck_quiz_definitions_scope CHECK (
        (scope_type = 'CONTENT' AND scope_season_id IS NULL AND scope_episode_id IS NULL)
        OR (scope_type = 'SEASON' AND scope_season_id IS NOT NULL AND scope_episode_id IS NULL)
        OR (scope_type = 'EPISODE' AND scope_season_id IS NOT NULL AND scope_episode_id IS NOT NULL)
    ),
    ADD CONSTRAINT fk_quiz_definitions_scope_season
        FOREIGN KEY (scope_season_id, content_id)
        REFERENCES catalog_seasons(id, content_id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_quiz_definitions_scope_episode
        FOREIGN KEY (scope_episode_id, scope_season_id)
        REFERENCES catalog_episodes(id, season_id) ON DELETE RESTRICT;

CREATE INDEX idx_quiz_definitions_scope
    ON quiz_definitions (content_id, scope_type, scope_season_id, scope_episode_id);
