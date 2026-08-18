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
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME204", "게임을 찾을 수 없습니다."),
    GAME_BOARD_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME205", "게임판 명단에 없는 멤버가 포함되어 있습니다."),

    GAME_BOARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "GAME301", "게임판을 관리할 권한이 없습니다."),

    GAME_BOARD_ID_REQUIRED(HttpStatus.BAD_REQUEST, "GAME401", "게임판 ID가 필요합니다."),
    INVALID_REALTIME_PAYLOAD(HttpStatus.BAD_REQUEST, "GAME402", "게임 요청 형식이 올바르지 않습니다."),
    GAME_NOT_WAITING(HttpStatus.BAD_REQUEST, "GAME403", "대기 중인 게임이 아닙니다."),
    GAME_NOT_PLAYING(HttpStatus.BAD_REQUEST, "GAME404", "진행 중인 게임이 아닙니다."),
    COURT_ALREADY_IN_USE(HttpStatus.BAD_REQUEST, "GAME405", "이미 게임이 진행 중인 코트입니다."),
    DUPLICATE_COURT_ID(HttpStatus.BAD_REQUEST, "GAME406", "코트 ID가 중복되어 요청되었습니다."),
    COURT_HAS_ACTIVE_GAME(HttpStatus.BAD_REQUEST, "GAME407", "진행 중인 게임이 있는 코트는 삭제할 수 없습니다."),
    INVALID_GAME_PLAYER_COUNT(HttpStatus.BAD_REQUEST, "GAME408", "게임 인원은 1명 이상 4명 이하여야 합니다."),
    DUPLICATE_GAME_PLAYER(HttpStatus.BAD_REQUEST, "GAME409", "같은 플레이어를 중복으로 넣을 수 없습니다."),
    INVALID_AGE_GROUP_FORMAT(HttpStatus.BAD_REQUEST, "GAME410", "올바른 연령대를 입력해주세요. (10대, 20대, 30대, 40대, 50대, 60대, 70대)"),
    GAME_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "GAME411", "이미 완료된 게임은 취소할 수 없습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "GAME412", "커서 형식이 올바르지 않습니다."),
    ACTIVE_GAME_MEMBER_CANNOT_BE_INACTIVE(HttpStatus.BAD_REQUEST, "GAME413", "진행 또는 대기 중인 게임에 포함된 선수는 참여 해제할 수 없습니다.")

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.of(code, message, httpStatus);
    }
}
