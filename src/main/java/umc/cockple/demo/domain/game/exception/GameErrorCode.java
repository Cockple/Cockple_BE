package umc.cockple.demo.domain.game.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

@Getter
@RequiredArgsConstructor
public enum GameErrorCode implements BaseErrorCode {

    GAME_BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME201", "게임판을 찾을 수 없습니다."),
    COURT_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME202", "해당 게임판에 존재하지 않는 코트입니다."),
    GAME_NOT_ON_COURT(HttpStatus.NOT_FOUND, "GAME203", "해당 코트에 이동할 게임이 없습니다."),

    GAME_BOARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "GAME301", "게임판을 관리할 권한이 없습니다."),

    GAME_BOARD_ID_REQUIRED(HttpStatus.BAD_REQUEST, "GAME401", "게임판 ID가 필요합니다."),
    INVALID_REALTIME_PAYLOAD(HttpStatus.BAD_REQUEST, "GAME402", "게임 요청 형식이 올바르지 않습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.of(code, message, httpStatus);
    }
}
