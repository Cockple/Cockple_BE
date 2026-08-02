-- 기존 알림 중 data로 목적지를 안전하게 판별할 수 있는 행만 backfill한다.
-- 과거에는 NotificationTarget을 저장하지 않았기 때문에 SIMPLE 알림의
-- 모임 삭제/역할 변경을 완전히 구분할 수 없다. 구분할 수 없는 행은
-- 잘못된 리다이렉트를 막기 위해 destination을 null로 유지한다.

UPDATE notification
SET resource_type = 'PARTY_INVITATION',
    resource_id = CAST(JSON_UNQUOTE(JSON_EXTRACT(data, '$.invitationId')) AS UNSIGNED),
    action = 'RESPOND'
WHERE resource_type IS NULL
  AND type = 'INVITE'
  AND JSON_VALID(data)
  AND JSON_EXTRACT(data, '$.invitationId') IS NOT NULL;

UPDATE notification
SET resource_type = 'EXERCISE',
    resource_id = CAST(JSON_UNQUOTE(JSON_EXTRACT(data, '$.exerciseId')) AS UNSIGNED),
    action = 'VIEW'
WHERE resource_type IS NULL
  AND JSON_VALID(data)
  AND JSON_EXTRACT(data, '$.exerciseId') IS NOT NULL
  AND party_id IS NOT NULL;

UPDATE notification
SET resource_type = 'PARTY',
    resource_id = party_id,
    action = 'VIEW'
WHERE resource_type IS NULL
  AND party_id IS NOT NULL
  AND (
        type = 'CHANGE'
        OR content LIKE '%님이 모임 초대를 수락하셨습니다!'
        OR content LIKE '%님이 부모임장으로 지정되었습니다.'
        OR content LIKE '%님의 부모임장 권한이 해제되었습니다.'
      );
