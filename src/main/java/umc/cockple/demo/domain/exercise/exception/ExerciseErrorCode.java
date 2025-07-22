package umc.cockple.demo.domain.exercise.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

@Getter
@RequiredArgsConstructor
public enum ExerciseErrorCode implements BaseErrorCode {

    /**
     * 1xx: 클라이언트가 수정해야 할 입력값 문제
     * 2xx: 서버에서 리소스를 찾을 수 없는 문제
     * 3xx: 권한/인증 문제
     * 4xx: 비즈니스 로직 위반
     */
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "EXERCISE101", "유효하지 않은 날짜 형식입니다. (YYYY-MM-DD 형식으로 입력해주세요)"),
    INVALID_START_TIME_FORMAT(HttpStatus.BAD_REQUEST, "EXERCISE102", "유효하지 않은 시작 시간 형식입니다. (HH:mm 형식으로 입력해주세요)"),
    INVALID_END_TIME_FORMAT(HttpStatus.BAD_REQUEST, "EXERCISE103", "유효하지 않은 종료 시간 형식입니다. (HH:mm 형식으로 입력해주세요)"),
    INVALID_EXERCISE_TIME(HttpStatus.BAD_REQUEST, "EXERCISE104", "종료 시간은 시작 시간보다 늦어야 합니다."),
    INCOMPLETE_DATE_RANGE(HttpStatus.BAD_REQUEST, "EXERCISE105", "시작 날짜와 종료 날짜는 둘 다 null이거나 둘 다 null이 아니어야 합니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "EXERCISE106", "종료 날짜는 시작 날짜보다 이후여야 합니다"),

    PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "EXERCISE201", "존재하지 않는 파티입니다."),
    EXERCISE_NOT_FOUND(HttpStatus.NOT_FOUND, "EXERCISE202", "존재하지 않는 운동입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "EXERCISE203", "존재하지 않는 멤버입니다."),
    MEMBER_EXERCISE_NOT_FOUND(HttpStatus.NOT_FOUND, "EXERCISE204", "존재하지 않는 운동 참여입니다."),
    GUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "EXERCISE205", "존재하지 않는 게스트입니다."),

    INSUFFICIENT_PERMISSION(HttpStatus.FORBIDDEN, "EXERCISE301", "운동을 생성할 권한이 없습니다."),
    NOT_PARTY_MEMBER(HttpStatus.FORBIDDEN, "EXERCISE302", "파티 멤버만 참여할 수 있습니다."),
    NOT_PARTY_MEMBER_FOR_GUEST_INVITE(HttpStatus.FORBIDDEN, "EXERCISE303", "파티 멤버만 게스트를 초대할 수 있습니다."),
    GUEST_INVITATION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "EXERCISE304", "외부 게스트 초대가 허용되지 않았습니다."),
    GUEST_NOT_INVITED_BY_MEMBER(HttpStatus.FORBIDDEN, "EXERCISE305", "본인이 아닌 다른 사용자에 의해 초대된 게스트입니다."),

    PAST_TIME_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "EXERCISE401", "운동 시간은 과거로 할 수 없습니다."),
    EXERCISE_ALREADY_STARTED_PARTICIPATION(HttpStatus.BAD_REQUEST, "EXERCISE402", "이미 시작된 운동에는 참여할 수 없습니다."),
    EXERCISE_ALREADY_STARTED_INVITATION(HttpStatus.BAD_REQUEST, "EXERCISE403", "이미 시작된 운동에는 초대할 수 없습니다."),
    EXERCISE_ALREADY_STARTED_CANCEL(HttpStatus.BAD_REQUEST, "EXERCISE404", "이미 시작된 운동에는 취소할 수 없습니다."),
    EXERCISE_ALREADY_STARTED_UPDATE(HttpStatus.BAD_REQUEST, "EXERCISE405", "이미 시작된 운동은 수정할 수 없습니다."),
    ALREADY_JOINED_EXERCISE(HttpStatus.BAD_REQUEST, "EXERCISE406", "이미 참여 신청한 운동입니다."),
    GUEST_IS_NOT_PARTICIPATED_IN_EXERCISE(HttpStatus.BAD_REQUEST, "EXERCISE407", "게스트가 이 운동에 참여해있지 않습니다."),
    MEMBER_LEVEL_NOT_ALLOWED(HttpStatus.FORBIDDEN, "EXERCISE408", "해당 급수로는 이 운동에 참여할 수 없습니다."),
    MEMBER_AGE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "EXERCISE409", "해당 나이로는 이 운동에 참여할 수 없습니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.of(code, message, httpStatus);
    }
}
