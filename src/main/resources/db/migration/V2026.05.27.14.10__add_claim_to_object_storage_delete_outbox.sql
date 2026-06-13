ALTER TABLE object_storage_delete_outbox
    ADD COLUMN claim_token VARCHAR(36),
    ADD INDEX idx_object_storage_delete_outbox_claim (status, retry_count, last_attempted_at, created_at);
