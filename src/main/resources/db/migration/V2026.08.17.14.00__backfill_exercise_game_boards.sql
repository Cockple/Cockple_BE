-- 기존 운동마다 빈 게임판을 생성하고 연결한다.
-- Exercise가 FK를 소유하므로 백필 중에만 GameBoard에 운동 ID를 임시로 보관한다.

ALTER TABLE game_board
    ADD COLUMN backfill_exercise_id BIGINT;

INSERT INTO game_board (created_at, updated_at, backfill_exercise_id)
SELECT COALESCE(exercise.created_at, CURRENT_TIMESTAMP(6)),
       COALESCE(exercise.updated_at, CURRENT_TIMESTAMP(6)),
       exercise.id
FROM exercise
WHERE exercise.game_board_id IS NULL;

UPDATE exercise
    INNER JOIN game_board
        ON game_board.backfill_exercise_id = exercise.id
SET exercise.game_board_id = game_board.id
WHERE exercise.game_board_id IS NULL;

ALTER TABLE game_board
    DROP COLUMN backfill_exercise_id;

ALTER TABLE exercise
    MODIFY COLUMN game_board_id BIGINT NOT NULL,
    ADD CONSTRAINT uk_exercise_game_board UNIQUE (game_board_id);
