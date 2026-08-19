ALTER TABLE catalog_contents
    ADD COLUMN watch_url VARCHAR(500);

ALTER TABLE catalog_contents
    ADD CONSTRAINT chk_catalog_contents_official_watch_url
    CHECK (
        watch_url IS NULL
        OR watch_url ~ '^https://(www\.)?tabii\.com/([A-Za-z]{2}(-[A-Za-z]{2})?/)?detail/[1-9][0-9]*(/[^/?#%]+)?/?$'
    );
