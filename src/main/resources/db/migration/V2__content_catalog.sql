CREATE TABLE catalog_contents (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    content_type VARCHAR(20) NOT NULL,
    publication_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_catalog_contents_title_not_blank CHECK (length(trim(title)) > 0),
    CONSTRAINT ck_catalog_contents_type CHECK (content_type IN ('SERIES', 'FILM')),
    CONSTRAINT ck_catalog_contents_status CHECK (publication_status IN ('DRAFT', 'PUBLISHED'))
);

CREATE TABLE catalog_seasons (
    id UUID PRIMARY KEY,
    content_id UUID NOT NULL,
    season_number INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    CONSTRAINT fk_catalog_seasons_content
        FOREIGN KEY (content_id) REFERENCES catalog_contents(id) ON DELETE CASCADE,
    CONSTRAINT uq_catalog_seasons_content_number UNIQUE (content_id, season_number),
    CONSTRAINT ck_catalog_seasons_number_positive CHECK (season_number > 0),
    CONSTRAINT ck_catalog_seasons_title_not_blank CHECK (length(trim(title)) > 0)
);

CREATE TABLE catalog_episodes (
    id UUID PRIMARY KEY,
    season_id UUID NOT NULL,
    episode_number INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    CONSTRAINT fk_catalog_episodes_season
        FOREIGN KEY (season_id) REFERENCES catalog_seasons(id) ON DELETE CASCADE,
    CONSTRAINT uq_catalog_episodes_season_number UNIQUE (season_id, episode_number),
    CONSTRAINT ck_catalog_episodes_number_positive CHECK (episode_number > 0),
    CONSTRAINT ck_catalog_episodes_title_not_blank CHECK (length(trim(title)) > 0)
);

CREATE TABLE admin_audit_entries (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    action VARCHAR(80) NOT NULL,
    resource_type VARCHAR(40) NOT NULL,
    resource_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_admin_audit_action_not_blank CHECK (length(trim(action)) > 0),
    CONSTRAINT ck_admin_audit_resource_type_not_blank CHECK (length(trim(resource_type)) > 0)
);

CREATE INDEX idx_catalog_contents_public_listing
    ON catalog_contents (publication_status, created_at DESC, id DESC);

CREATE INDEX idx_catalog_seasons_content_id ON catalog_seasons (content_id);
CREATE INDEX idx_catalog_episodes_season_id ON catalog_episodes (season_id);
CREATE INDEX idx_admin_audit_resource ON admin_audit_entries (resource_type, resource_id);
