ALTER TABLE notification
    ADD COLUMN notification_key VARCHAR(255) NULL;

CREATE UNIQUE INDEX uk_notification_notification_key
    ON notification (notification_key);
