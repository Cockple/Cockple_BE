-- 프로필 사진 교체(UPDATE) 동시성에서 lost update 를 막기 위한 낙관적 락 버전 컬럼.
-- 기존 row 는 0 으로 시작한다.
ALTER TABLE profile_img
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
