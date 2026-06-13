CREATE INDEX idx_message_read_member_read_room_message
    ON message_read_status (member_id, is_read, chat_room_id, chat_message_id);

CREATE INDEX idx_chat_room_member_member_room_status_last_read
    ON chat_room_member (member_id, chat_room_id, status, last_read_message_id);
