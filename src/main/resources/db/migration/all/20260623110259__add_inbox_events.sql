CREATE TABLE inbox_events
(
    id                UUID PRIMARY KEY,
    event_type        TEXT                     NOT NULL,
    event_detail_url  TEXT                     NOT NULL,
    event_occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_status  VARCHAR(20)              NOT NULL,
    processed_at      TIMESTAMP WITH TIME ZONE,
    payload           JSONB                    not null
);

CREATE INDEX idx_inbox_event_processed_status_occurred_at
    ON inbox_events (processed_status, event_occurred_at);