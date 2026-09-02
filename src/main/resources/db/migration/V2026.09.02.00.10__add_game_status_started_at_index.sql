-- 자동 완료 스케줄러가 주기적으로 수행하는
-- "status = 'PLAYING' AND started_at < :threshold" 스캔을 뒷받침하는 복합 인덱스.
CREATE INDEX idx_game_status_started_at
    ON game (status, started_at);
