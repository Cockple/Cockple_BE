package umc.cockple.demo.domain.exercise.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

@Getter
@RequiredArgsConstructor
public enum ExerciseErrorCode implements BaseErrorCode {

    PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "EXERCISE001", "존재하지 않는 파티입니다."),
    INSUFFICIENT_PERMISSION(HttpStatus.FORBIDDEN, "EXERCISE002", "운동을 생성할 권한이 없습니다."),
    INVALID_EXERCISE_TIME(HttpStatus.BAD_REQUEST, "EXERCISE003", "종료 시간은 시작 시간보다 늦어야 합니다."),
    PAST_TIME_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "EXERCISE004", "운동 시간은 과거로 할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.of(code, message, httpStatus);
    }
}
