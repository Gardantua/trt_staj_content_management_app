CREATE TABLE media_assets (
    id UUID PRIMARY KEY,
    storage_key VARCHAR(100) NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    byte_size BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_media_assets_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_media_assets_type CHECK (media_type IN ('IMAGE')),
    CONSTRAINT ck_media_assets_mime CHECK (mime_type IN ('image/jpeg', 'image/png')),
    CONSTRAINT ck_media_assets_size CHECK (byte_size > 0 AND byte_size <= 5242880),
    CONSTRAINT ck_media_assets_dimensions CHECK (
        width > 0 AND width <= 4096 AND height > 0 AND height <= 4096
    ),
    CONSTRAINT ck_media_assets_checksum CHECK (
        checksum_sha256 ~ '^[0-9a-f]{64}$'
    )
);

ALTER TABLE catalog_contents
    ADD COLUMN cover_media_id UUID,
    ADD COLUMN cover_alternative_text VARCHAR(500),
    ADD CONSTRAINT fk_catalog_contents_cover_media
        FOREIGN KEY (cover_media_id) REFERENCES media_assets(id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_catalog_contents_cover_pair CHECK (
        (cover_media_id IS NULL AND cover_alternative_text IS NULL)
        OR (cover_media_id IS NOT NULL
            AND length(trim(cover_alternative_text)) > 0)
    );

ALTER TABLE quiz_versions
    ADD COLUMN fallback_media_id UUID,
    ADD COLUMN fallback_alternative_text VARCHAR(500),
    ADD CONSTRAINT fk_quiz_versions_fallback_media
        FOREIGN KEY (fallback_media_id) REFERENCES media_assets(id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_quiz_versions_fallback_pair CHECK (
        (fallback_media_id IS NULL AND fallback_alternative_text IS NULL)
        OR (fallback_media_id IS NOT NULL
            AND length(trim(fallback_alternative_text)) > 0)
    );

ALTER TABLE quiz_questions
    ADD COLUMN visual_media_id UUID,
    ADD COLUMN visual_role VARCHAR(30),
    ADD COLUMN visual_alternative_text VARCHAR(500),
    ADD COLUMN accessible_prompt VARCHAR(1000),
    ADD CONSTRAINT fk_quiz_questions_visual_media
        FOREIGN KEY (visual_media_id) REFERENCES media_assets(id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_quiz_questions_visual_contract CHECK (
        (visual_media_id IS NULL
            AND visual_role IS NULL
            AND visual_alternative_text IS NULL
            AND accessible_prompt IS NULL)
        OR (visual_media_id IS NOT NULL
            AND visual_role = 'INFORMATIVE'
            AND length(trim(visual_alternative_text)) > 0
            AND length(trim(accessible_prompt)) > 0)
        OR (visual_media_id IS NOT NULL
            AND visual_role = 'DECORATIVE'
            AND visual_alternative_text IS NULL
            AND accessible_prompt IS NULL)
    );

ALTER TABLE gameplay_attempts
    ADD COLUMN timing_policy_version VARCHAR(40) NOT NULL DEFAULT 'STANDARD_V1',
    ADD CONSTRAINT ck_gameplay_attempts_timing_policy CHECK (
        timing_policy_version IN ('STANDARD_V1', 'EXTENDED_V1')
    );

CREATE INDEX idx_catalog_contents_cover_media ON catalog_contents (cover_media_id);
CREATE INDEX idx_quiz_versions_fallback_media ON quiz_versions (fallback_media_id);
CREATE INDEX idx_quiz_questions_visual_media ON quiz_questions (visual_media_id);
