package umc.cockple.demo.domain.exercise.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

@Getter
@RequiredArgsConstructor
public enum ExerciseErrorCode implements BaseErrorCode {

    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "EXERCISE001", "유효하지 않은 날짜 형식입니다. (YYYY-MM-DD 형식으로 입력해주세요)"),
    INVALID_START_TIME_FORMAT(HttpStatus.BAD_REQUEST, "EXERCISE002", "유효하지 않은 시작 시간 형식입니다. (HH:mm 형식으로 입력해주세요)"),
    INVALID_END_TIME_FORMAT(HttpStatus.BAD_REQUEST, "EXERCISE003", "유효하지 않은 종료 시간 형식입니다. (HH:mm 형식으로 입력해주세요)"),

    PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "EXERCISE004", "존재하지 않는 파티입니다."),
    INSUFFICIENT_PERMISSION(HttpStatus.FORBIDDEN, "EXERCISE005", "운동을 생성할 권한이 없습니다."),
    INVALID_EXERCISE_TIME(HttpStatus.BAD_REQUEST, "EXERCISE006", "종료 시간은 시작 시간보다 늦어야 합니다."),
    PAST_TIME_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "EXERCISE007", "운동 시간은 과거로 할 수 없습니다."),
    EXERCISE_NOT_FOUND(HttpStatus.NOT_FOUND, "EXERCISE008", "존재하지 않는 운동입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "EXERCISE009", "존재하지 않는 멤버입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.of(code, message, httpStatus);
    }
}
