INSERT INTO chat_room_member (
    created_at,
    updated_at,
    chat_room_id,
    member_id,
    display_name,
    last_read_message_id,
    status
)
SELECT
    NOW(6),
    NOW(6),
    cr.id,
    mp.member_id,
    NULL,
    NULL,
    'JOINED'
FROM member_party mp
JOIN party p
    ON p.id = mp.party_id
JOIN chat_room cr
    ON cr.party_id = mp.party_id
LEFT JOIN chat_room_member crm
    ON crm.chat_room_id = cr.id
   AND crm.member_id = mp.member_id
WHERE mp.status = 'ACTIVE'
  AND p.status = 'ACTIVE'
  AND crm.id IS NULL;
