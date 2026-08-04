ALTER TABLE outbox_events
    ADD COLUMN trace_parent VARCHAR(55),
    ADD COLUMN trace_state VARCHAR(512);

ALTER TABLE outbox_events
    ADD CONSTRAINT ck_outbox_trace_parent_w3c
        CHECK (
            trace_parent IS NULL
            OR trace_parent ~ '^00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$'
        ),
    ADD CONSTRAINT ck_outbox_trace_state_not_blank
        CHECK (trace_state IS NULL OR btrim(trace_state) <> '');

COMMENT ON COLUMN outbox_events.trace_parent IS
    'Nullable W3C traceparent captured with the business transaction; old rows remain publishable.';
COMMENT ON COLUMN outbox_events.trace_state IS
    'Optional W3C tracestate; baggage is intentionally not persisted.';
