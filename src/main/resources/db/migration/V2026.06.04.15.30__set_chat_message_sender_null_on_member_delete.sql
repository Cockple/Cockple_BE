ALTER TABLE chat_message
    DROP FOREIGN KEY fk_chat_message_sender;

ALTER TABLE chat_message
    MODIFY sender_id BIGINT NULL;

ALTER TABLE chat_message
    ADD CONSTRAINT fk_chat_message_sender
        FOREIGN KEY (sender_id) REFERENCES member (id)
        ON DELETE SET NULL;
