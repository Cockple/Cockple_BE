-- 보관 정책 정리(삭제 후보 조회)와 회원별 알림 목록 조회를 뒷받침하는 인덱스.
CREATE INDEX idx_notification_member_created_at
    ON notification (member_id, created_at);
