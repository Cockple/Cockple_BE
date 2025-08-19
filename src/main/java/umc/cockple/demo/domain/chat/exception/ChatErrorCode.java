package umc.cockple.demo.domain.chat.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements BaseErrorCode {

    //1xx: 클라이언트가 수정해야 할 입력값 문제
    CANNOT_CHAT_WITH_SELF(HttpStatus.BAD_REQUEST, "CHAT101", "나와의 채팅방 생성은 불가합니다."),
    CHATROOM_ID_NECESSARY(HttpStatus.BAD_REQUEST, "CHAT102", "채팅방 ID가 필요합니다."),
    EMPTY_MESSAGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CHAT103", "빈 메세지는 보낼 수 없습니다."),
    MESSAGE_TO_LONG(HttpStatus.BAD_REQUEST, "CHAT104", "메시지는 1000자를 초과할 수 없습니다."),

    // 2xx: 리소스 없음
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT201", "채팅방을 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT202", "사용자를 찾을 수 없습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT203", "존재하지 않는 파일입니다."),
    PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT204", "존재하지 않는 파티입니다."),

    // 3xx: 인증/인가 문제
    CHAT_ROOM_ACCESS_DENIED(HttpStatus.BAD_REQUEST, "CHAT301", "채팅방에 접근할 권한이 없습니다."),
    INVALID_DOWNLOAD_TOKEN(HttpStatus.FORBIDDEN, "CHAT302", "유효하지 않거나 만료된 다운로드 토큰입니다."),
    NOT_PARTY_MEMBER(HttpStatus.FORBIDDEN, "CHAT303", "해당 모임의 멤버만 채팅방에 접근할 수 있습니다."),

    // 5xx: 서버 내부 문제
    RESPONSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT501", "파일 다운로드 응답 생성에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.of(code, message, httpStatus);
    }
}
