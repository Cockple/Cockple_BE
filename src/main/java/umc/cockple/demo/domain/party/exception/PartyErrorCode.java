package umc.cockple.demo.domain.party.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

@Getter
@RequiredArgsConstructor
public enum PartyErrorCode implements BaseErrorCode {

    /**
     * 1xx: 클라이언트가 수정해야 할 입력값 문제
     * 2xx: 서버에서 리소스를 찾을 수 없는 문제
     * 3xx: 권한/인증 문제
     * 4xx: 비즈니스 로직 위반
     */
    PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "PARTY201", "존재하지 않는 모임입니다."),

    ALREADY_MEMBER(HttpStatus.CONFLICT, "PARTY401", "이미 가입된 모임입니다."),
    JOIN_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "PARTY402", "처리 대기중인 가입 신청이 존재합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.of(code, message, httpStatus);
    }
}
