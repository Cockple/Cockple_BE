ALTER TABLE notification_outbox
    ADD COLUMN next_attempt_at DATETIME(6) NULL;

ALTER TABLE notification_push_outbox
    ADD COLUMN next_attempt_at DATETIME(6) NULL,
    ADD COLUMN deduplication_key VARCHAR(255) NULL;

UPDATE notification_push_outbox
SET deduplication_key = CONCAT('notification:', notification_id, ':', channel)
WHERE deduplication_key IS NULL
  AND notification_id IS NOT NULL;

ALTER TABLE notification_push_outbox
    MODIFY COLUMN deduplication_key VARCHAR(255) NOT NULL;

CREATE UNIQUE INDEX uk_notification_push_outbox_deduplication_key
    ON notification_push_outbox (deduplication_key);

CREATE INDEX idx_notification_outbox_next_attempt
    ON notification_outbox (status, next_attempt_at, created_at);

CREATE INDEX idx_notification_push_outbox_next_attempt
    ON notification_push_outbox (status, next_attempt_at, created_at);
