-- 기존 게임판 중 코트가 하나도 없는 게임판에 기본 코트 2개(1번 코트, 2번 코트)를 채운다.
-- 게임판 생성 시 기본 코트를 부여하도록 바뀌었으나, 그 이전에 생성된 게임판은 코트가 0개이므로 정합성을 맞춘다.
-- 이미 코트가 있는 게임판(운영 중 코트 관리를 거친 게임판)은 건드리지 않는다.

INSERT INTO court (created_at, updated_at, game_board_id, court_no, court_name)
SELECT CURRENT_TIMESTAMP(6),
       CURRENT_TIMESTAMP(6),
       gb.id,
       seq.court_no,
       CONCAT(seq.court_no, '번 코트')
FROM game_board gb
         JOIN (SELECT 1 AS court_no
               UNION ALL
               SELECT 2) seq
WHERE NOT EXISTS (SELECT 1
                  FROM court c
                  WHERE c.game_board_id = gb.id);
