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
