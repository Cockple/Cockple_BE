ALTER TABLE exercise
    ADD COLUMN game_host_id BIGINT;

UPDATE exercise
    INNER JOIN party ON party.id = exercise.party_id
SET exercise.game_host_id = party.owner_id
WHERE exercise.game_host_id IS NULL;

ALTER TABLE exercise
    MODIFY COLUMN game_host_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_exercise_game_host FOREIGN KEY (game_host_id) REFERENCES member (id);
