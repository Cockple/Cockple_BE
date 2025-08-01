package umc.cockple.demo.domain.chat.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements BaseErrorCode {

    // 2xx: 리소스 없음
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT201", "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_MEMBER_NOT_FOUND(HttpStatus.BAD_REQUEST, "CHAT202", "채팅방 멤버가 존재하지 않습니다."),

    // 3xx: 인증/인가 문제
    NOT_CHAT_ROOM_MEMBER(HttpStatus.BAD_REQUEST, "CHAT301", "채팅방에 참여한 멤버가 아닙니다."),
    PARTY_MEMBER_NOT_FOUND(HttpStatus.BAD_REQUEST, "CHAT302", "모임에 참여한 회원이 아닙니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.of(code, message, httpStatus);
    }
}
