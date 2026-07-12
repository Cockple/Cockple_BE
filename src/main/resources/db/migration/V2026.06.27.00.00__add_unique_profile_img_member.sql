-- 삭제될 중복 row 가 가리키던 이미지 중 '고아가 되는 key'(살아남는 row 가 안 쓰는 key)를
-- object_storage_delete_outbox 에 적재한다. 기존 스케줄러가 GCS 에서 비동기로 삭제한다.
-- 반드시 아래 DELETE 보다 먼저 실행되어야 한다(삭제 후엔 img_key 를 알 수 없음).
INSERT INTO object_storage_delete_outbox
    (object_key, source_type, source_id, status, retry_count, created_at, updated_at)
SELECT DISTINCT doomed.img_key, 'MEMBER_PROFILE_IMG', doomed.member_id, 'PENDING', 0, NOW(6), NOW(6)
FROM profile_img doomed
JOIN (SELECT member_id, MAX(id) AS keep_id
      FROM profile_img
      WHERE member_id IS NOT NULL
      GROUP BY member_id) keep
    ON doomed.member_id = keep.member_id
WHERE doomed.id <> keep.keep_id
  AND doomed.img_key NOT IN (
      SELECT survivor.img_key
      FROM profile_img survivor
      WHERE survivor.id IN (SELECT MAX(id)
                            FROM profile_img
                            WHERE member_id IS NOT NULL
                            GROUP BY member_id));

-- member 당 1개만 남기고 중복 row 정리
DELETE pi
FROM profile_img pi
JOIN (
    SELECT member_id, MAX(id) AS keep_id
    FROM profile_img
    WHERE member_id IS NOT NULL
    GROUP BY member_id
) keep
    ON pi.member_id = keep.member_id
WHERE pi.id <> keep.keep_id;

-- member 당 profile_img 1개를 DB 레벨에서 보장한다.
ALTER TABLE profile_img
    ADD CONSTRAINT uq_profile_img_member UNIQUE (member_id);
