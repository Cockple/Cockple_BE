-- 완료 게임이 라이브 코트 FK(fk_game_court)에 묶이지 않도록 코트 번호를 game에 스냅샷(denormalize)한다.
-- 이렇게 하면 완료 이력이 참조하는 코트를 삭제해도 FK 위반이 발생하지 않고, 코트 번호 표시는 유지된다.

ALTER TABLE game
    ADD COLUMN court_no INTEGER;

-- 현재 코트를 참조하는 게임(진행/완료)의 코트 번호를 스냅샷으로 채운다.
UPDATE game g
    JOIN court c ON g.court_id = c.id
SET g.court_no = c.court_no
WHERE g.court_id IS NOT NULL;

-- 기존 완료 게임은 라이브 코트 FK를 끊는다(신규 complete() 동작과 정합성 유지).
UPDATE game
SET court_id = NULL
WHERE status = 'COMPLETED';
