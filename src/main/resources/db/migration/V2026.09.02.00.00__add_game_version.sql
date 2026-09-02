-- 게임 완료/이동/삭제 요청과 자동 완료 스케줄러가 같은 게임을 동시에 수정할 때
-- lost update(이중 완료/이중 게임횟수 증가)를 막기 위한 낙관적 락 버전 컬럼.
-- 기존 row 는 0 으로 시작한다.
ALTER TABLE game
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
