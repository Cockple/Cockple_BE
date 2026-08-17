ALTER TABLE game_board_member
    DROP FOREIGN KEY fk_game_board_member_guest;

ALTER TABLE game_board_member
    ADD CONSTRAINT fk_game_board_member_guest
        FOREIGN KEY (guest_id) REFERENCES guest (id)
        ON DELETE CASCADE;
