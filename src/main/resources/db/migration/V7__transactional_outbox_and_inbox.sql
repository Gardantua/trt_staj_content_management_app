CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    event_version INTEGER NOT NULL,
    payload JSONB NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    publish_attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    last_error VARCHAR(500),
    CONSTRAINT ck_outbox_event_version_positive CHECK (event_version > 0),
    CONSTRAINT ck_outbox_publish_attempts_non_negative CHECK (publish_attempts >= 0),
    CONSTRAINT ck_outbox_trace_id_not_blank CHECK (btrim(trace_id) <> ''),
    CONSTRAINT ck_outbox_publish_state CHECK (
        (published_at IS NULL)
        OR (published_at IS NOT NULL AND last_error IS NULL)
    )
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (next_attempt_at, occurred_at, event_id)
    WHERE published_at IS NULL;

CREATE TABLE inbox_messages (
    event_id UUID NOT NULL,
    consumer_name VARCHAR(120) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    event_version INTEGER NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (consumer_name, event_id),
    CONSTRAINT ck_inbox_event_version_positive CHECK (event_version > 0),
    CONSTRAINT ck_inbox_consumer_name_not_blank CHECK (btrim(consumer_name) <> '')
);

CREATE INDEX idx_inbox_messages_processed
    ON inbox_messages (processed_at DESC, event_id);
