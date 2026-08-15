ALTER TABLE notification_push_outbox
    MODIFY COLUMN notification_id BIGINT NULL,
    ADD COLUMN target_type VARCHAR(20) NOT NULL DEFAULT 'NOTIFICATION',
    ADD COLUMN chat_room_id BIGINT NULL,
    ADD COLUMN chat_room_type VARCHAR(30) NULL,
    ADD COLUMN title VARCHAR(255) NULL,
    ADD COLUMN content TEXT NULL,
    ADD COLUMN sender_id BIGINT NULL,
    ADD COLUMN active_subscriber_ids TEXT NULL;
