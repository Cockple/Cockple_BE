ALTER TABLE game_board_member
    DROP FOREIGN KEY fk_game_board_member_game_board;

ALTER TABLE game_board_member
    ADD COLUMN member_id BIGINT NULL,
    ADD COLUMN guest_id BIGINT NULL,
    MODIFY COLUMN game_board_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_game_board_member_game_board
        FOREIGN KEY (game_board_id) REFERENCES game_board (id),
    ADD CONSTRAINT fk_game_board_member_member
        FOREIGN KEY (member_id) REFERENCES member (id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_game_board_member_guest
        FOREIGN KEY (guest_id) REFERENCES guest (id)
        ON DELETE CASCADE,
    ADD CONSTRAINT uk_game_board_member_board_member
        UNIQUE (game_board_id, member_id),
    ADD CONSTRAINT uk_game_board_member_board_guest
        UNIQUE (game_board_id, guest_id);
