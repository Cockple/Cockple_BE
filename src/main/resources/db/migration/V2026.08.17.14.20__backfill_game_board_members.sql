-- 동일 운동에 같은 회원이 중복 참여한 기존 데이터를 최초 레코드만 남기고 정리한다.
DELETE duplicate_member_exercise
FROM member_exercise duplicate_member_exercise
    INNER JOIN member_exercise retained_member_exercise
        ON retained_member_exercise.exercise_id = duplicate_member_exercise.exercise_id
        AND retained_member_exercise.member_id = duplicate_member_exercise.member_id
        AND retained_member_exercise.id < duplicate_member_exercise.id;

-- 동시 참가 요청과 이후 백필에서도 같은 중복이 다시 생성되지 않도록 DB에서 보장한다.
ALTER TABLE member_exercise
    ADD CONSTRAINT uk_member_exercise_exercise_member
        UNIQUE (exercise_id, member_id);

-- 기존 회원 운동 참가자를 게임판 명단 스냅샷으로 백필한다.
INSERT INTO game_board_member (
    created_at,
    updated_at,
    game_board_id,
    member_id,
    guest_id,
    name,
    gender,
    level,
    age_group,
    shuttlecock_submitted,
    participating,
    game_count
)
SELECT COALESCE(member_exercise.created_at, exercise.created_at, CURRENT_TIMESTAMP(6)),
       COALESCE(member_exercise.updated_at, member_exercise.created_at, exercise.updated_at, CURRENT_TIMESTAMP(6)),
       exercise.game_board_id,
       member.id,
       NULL,
       member.member_name,
       member.gender,
       member.level,
       CASE FLOOR(TIMESTAMPDIFF(YEAR, member.birth, exercise.date) / 10)
           WHEN 1 THEN 'TEENS'
           WHEN 2 THEN 'TWENTIES'
           WHEN 3 THEN 'THIRTIES'
           WHEN 4 THEN 'FORTIES'
           WHEN 5 THEN 'FIFTIES'
           WHEN 6 THEN 'SIXTIES'
           WHEN 7 THEN 'SEVENTIES'
           ELSE NULL
       END,
       0,
       1,
       0
FROM member_exercise
    INNER JOIN exercise ON exercise.id = member_exercise.exercise_id
    INNER JOIN member ON member.id = member_exercise.member_id
    LEFT JOIN game_board_member existing_member
        ON existing_member.game_board_id = exercise.game_board_id
        AND existing_member.member_id = member.id
WHERE existing_member.id IS NULL;

-- 기존 운동 게스트를 연령대가 없는 게임판 명단 스냅샷으로 백필한다.
INSERT INTO game_board_member (
    created_at,
    updated_at,
    game_board_id,
    member_id,
    guest_id,
    name,
    gender,
    level,
    age_group,
    shuttlecock_submitted,
    participating,
    game_count
)
SELECT COALESCE(guest.created_at, exercise.created_at, CURRENT_TIMESTAMP(6)),
       COALESCE(guest.updated_at, guest.created_at, exercise.updated_at, CURRENT_TIMESTAMP(6)),
       exercise.game_board_id,
       NULL,
       guest.id,
       guest.guest_name,
       guest.gender,
       guest.level,
       NULL,
       0,
       1,
       0
FROM guest
    INNER JOIN exercise ON exercise.id = guest.exercise_id
    LEFT JOIN game_board_member existing_guest
        ON existing_guest.game_board_id = exercise.game_board_id
        AND existing_guest.guest_id = guest.id
WHERE existing_guest.id IS NULL;
