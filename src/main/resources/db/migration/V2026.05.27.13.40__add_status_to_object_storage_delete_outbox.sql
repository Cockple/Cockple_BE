ALTER TABLE object_storage_delete_outbox
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_error VARCHAR(2000),
    ADD COLUMN last_attempted_at DATETIME(6),
    ADD INDEX idx_object_storage_delete_outbox_retry (status, retry_count, created_at);
