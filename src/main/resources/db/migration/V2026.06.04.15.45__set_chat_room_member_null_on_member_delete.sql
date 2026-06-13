ALTER TABLE chat_room_member
    DROP FOREIGN KEY fk_chat_room_member_member;

ALTER TABLE chat_room_member
    MODIFY member_id BIGINT NULL;

ALTER TABLE chat_room_member
    ADD CONSTRAINT fk_chat_room_member_member
        FOREIGN KEY (member_id) REFERENCES member (id)
        ON DELETE SET NULL;
