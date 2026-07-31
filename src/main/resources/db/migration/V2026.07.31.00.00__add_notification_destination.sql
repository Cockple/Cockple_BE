ALTER TABLE notification
    ADD COLUMN resource_type VARCHAR(50) NULL,
    ADD COLUMN resource_id BIGINT NULL,
    ADD COLUMN action VARCHAR(50) NULL;

CREATE INDEX idx_notification_resource
    ON notification (resource_type, resource_id);
