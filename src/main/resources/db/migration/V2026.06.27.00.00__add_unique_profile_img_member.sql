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
