ALTER TABLE outbox_events ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE outbox_events ADD COLUMN next_attempt_at TIMESTAMP;
ALTER TABLE outbox_events ADD COLUMN lease_id UUID;
ALTER TABLE outbox_events ADD COLUMN lease_until TIMESTAMP;
ALTER TABLE outbox_events ADD COLUMN last_error VARCHAR(2000);
ALTER TABLE outbox_events ADD COLUMN dead_lettered_at TIMESTAMP;

CREATE INDEX idx_outbox_events_claimable
    ON outbox_events (processed_at, dead_lettered_at, next_attempt_at, lease_until, created_at);